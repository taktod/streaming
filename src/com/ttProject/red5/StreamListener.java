package com.ttProject.red5;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.flv.FLVHeader;
import org.red5.io.flv.impl.Tag;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.stream.IStreamData;

import com.ttProject.streaming.httptakstreaming.TakSegmentCreator;
import com.ttProject.xuggle.in.red5.Red5DataQueue;

/**
 * red5の放送されているストリームを監視する動作
 * ここである程度動作を実施する。
 * やること。streamの先頭数パケットはそのままスルーして、動画と映像どっちのデータが存在しているか確認する。先頭からやりたいけど、どうしようもないので、次のようにする。
 * 1:始めのキーフレームはすて、無効10フレームはスルーする。(ただし先頭のキーフレームは保持しておく。)
 * 2:その間に送られてきた音声フレームと映像フレームで、要素があるか確認する。(どちらかが送られてきていない場合は、送られていないフレームは、もうこないものとする。(フレームがきた場合はxuggleは設定し直しが必要。))
 * 
 * このクラスでflvHeaderとflvMetaDataは保持しておく。
 * 
 * @author taktod
 */
public class StreamListener implements IStreamListener {
	/** flvヘッダのデータ長 */
	private final static int HEADER_LENGTH = 9;
	/** 各flvタグのヘッダー長 */
	private final static int TAG_HEADER_LENGTH = 11;
	/** デフォルトストリームID */
	private final static byte[] DEFAULT_STREAM_ID = {(byte)0x00, (byte)0x00, (byte)0x00};
	/** xuggle用のデータqueue */
	private Red5DataQueue red5DataQueue;
	/** httpTakStreaming用のfileCreator */
	private TakSegmentCreator ftFileCreator; // tag segmenterに渡すデータは生データにしたいので、このクラスで生データを扱うようにしたいところ。

	// このストリームとひもづいているRed5DataQueueとFlvByteCreatorを保持しておく必要がある。
	private Set<ITag> metaData = new HashSet<ITag>();
	private ITag audioFirstTag = null; // それぞれの初期タグを保持しておく
	private ITag videoFirstTag = null; // それぞれの初期タグを保持しておく
	/**
	 * headerを作成する。
	 */
	private void makeHeader() {
		// flvHeaderを生成します。
		FLVHeader flvHeader = new FLVHeader();
		flvHeader.setFlagAudio(audioFirstTag != null);
		flvHeader.setFlagVideo(videoFirstTag != null);
		// headerが更新された場合ftFileCreatorに通知しておく。
		// byte配列を復元する。
		ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4);
		flvHeader.write(header);
		ftFileCreator.writeHeaderPacket(header);
		header.clear();
		header = null;
		red5DataQueue.putHeaderData(flvHeader, metaData, videoFirstTag, audioFirstTag);
	}
	/**
	 * tagデータを確認します。
	 * @param tag
	 */
	private void checkTag(ITag tag) {
		byte dataType = tag.getDataType();
		// httpTakStreamingはそのまま動作してOKなので(あとでメタデータやheaderデータがきても問題ない。)
		if(audioFirstTag == null && dataType != ITag.TYPE_AUDIO) {
			// 新規にタグを発見したので、updateが必要
			audioFirstTag = tag;
			// headerを作り直す。
			makeHeader();
		}
		if(videoFirstTag == null && dataType != ITag.TYPE_VIDEO) {
			// 新規にタグを発見したので、updateが必要
			videoFirstTag = tag;
			// headerを作り直す
			makeHeader();
		}
		if(dataType != ITag.TYPE_METADATA) {
			// metaデータ記録しておく。
			metaData.add(tag);
		}
		// codec情報等解析することが可能ですが、今回は興味がないのでパスします。
		// タグデータをbyteBufferにいれこんで渡します。
		try {
			int bodySize = tag.getBodySize();
			// 全体のサイズはヘッダ長(11バイト) + データ長 + 全体のサイズ(4バイト)で構成されます。
			int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
			ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
			int timestamp = tag.getTimestamp();
			
			byte[] bodyBuf = null;
			if(bodySize > 0) {
				bodyBuf = new byte[bodySize];
				tag.getBody().get(bodyBuf);
			}
			// タイプ メタ0x12 音声0x80 映像0x90など(1バイト)
			IOUtils.writeUnsignedByte(tagBuffer, dataType);
			// データサイズ(3バイト)
			IOUtils.writeMediumInt(tagBuffer, bodySize);
			// タイムスタンプ
			IOUtils.writeExtendedMediumInt(tagBuffer, timestamp);
			// streamID(0x00 0x00 0x00)
			tagBuffer.put(DEFAULT_STREAM_ID);
			// 実データ
			if(bodyBuf != null) {
				tagBuffer.put(bodyBuf);
			}
			// tagサイズを加えて終了
			tagBuffer.putInt(TAG_HEADER_LENGTH + bodySize);
			// 参照カウントを先頭に戻す。
			tagBuffer.flip();
			ftFileCreator.writeTagData(tagBuffer);
		}
		catch (Exception e) {
		}
		red5DataQueue.putTagData(tag);
	}
	/**
	 * パケットデータをうけとったときの動作
	 */
	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
		try {
			if(!(packet instanceof IRTMPEvent) || !(packet instanceof IStreamData<?>)) {
				// パケットデータがRTMPEventでもStreamDataでもない場合は処理しません。
				return;
			}
			IRTMPEvent rtmpEvent = (IRTMPEvent) packet;
			if(rtmpEvent.getHeader().getSize() == 0) {
				// sizeが取得できないので、スキップする。
				return;
			}
			// tagを作成しておく。
			ITag tag = new Tag();
			tag.setDataType(rtmpEvent.getDataType());
			tag.setTimestamp(rtmpEvent.getTimestamp());
			IoBuffer data = ((IStreamData<?>) rtmpEvent).getData().asReadOnlyBuffer();
			tag.setBodySize(data.limit());
			tag.setBody(data);
			checkTag(tag);
		}
		catch (Exception e) {
		}
	}
}
