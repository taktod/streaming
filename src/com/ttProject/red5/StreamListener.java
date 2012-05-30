package com.ttProject.red5;

import java.nio.ByteBuffer;

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
import com.ttProject.xuggle.in.flv.FlvDataQueue;

/**
 * red5の放送されているストリームを監視する動作
 * すべてのデータをflvの形にするために、ByteBufferの形に変更して、各モジュールにデータを渡す。
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
	private FlvDataQueue flvDataQueue;
	/** httpTakStreaming用のfileCreator */
	private TakSegmentCreator takSegmentCreator; // tag segmenterに渡すデータは生データにしたいので、このクラスで生データを扱うようにしたいところ。
	/** 動作ストリームを保持します。 */
	IBroadcastStream stream;

	// このストリームとひもづいているRed5DataQueueとFlvByteCreatorを保持しておく必要がある。
	private ITag audioFirstTag = null; // それぞれの初期タグを保持しておく
	private ITag videoFirstTag = null; // それぞれの初期タグを保持しておく
	/**
	 * コンストラクタ
	 */
	public StreamListener(IBroadcastStream stream, FlvDataQueue flvDataQueue, TakSegmentCreator takSegmentCreator) {
		if(stream == null) {
			throw new RuntimeException("ストリームがありません。");
		}
		if(flvDataQueue == null && takSegmentCreator == null) {
			throw new RuntimeException("動作対象のモジュールの設定がありません。動作させる意味がありません。");
		}
		// 保持する必要なさそうなので、放置しておく。
		this.stream = stream;
		// 初期時に利用するモジュールを設定してもらう。必要なければNULLをいれる。
		this.flvDataQueue = flvDataQueue;
		this.takSegmentCreator = takSegmentCreator;
		// headerを構築しておきます。
		ByteBuffer header = makeHeaderByteBuffer();
		if(flvDataQueue != null) {
			flvDataQueue.putHeaderData(header);
		}
		if(takSegmentCreator != null) {
			takSegmentCreator.writeHeaderPacket(header, null, null);
		}
		// streamの監視を開始します。
		stream.addStreamListener(this);
	}
	/**
	 * 必要なくなったらcloseします。
	 */
	public void close() {
		stream.removeStreamListener(this);
		if(flvDataQueue != null) {
			flvDataQueue.close();
		}
		if(takSegmentCreator != null) {
			takSegmentCreator.close();
		}
	}
	/**
	 * headerを作成する。
	 */
	private ByteBuffer makeHeaderByteBuffer() {
		// flvHeaderを生成します。
		FLVHeader flvHeader = new FLVHeader();
		// TODO transcoderの方で、audioパケットがながれてきたら、audioがあるものとして動作するようにしますが、入力データは一定のままとして扱いたい。
		// なので、ここでのFlvHeaderとしては、少々不正になる可能性がありますが、audioもvideoも存在するものとして動作させます。
		flvHeader.setFlagAudio(true);
		flvHeader.setFlagVideo(true);
		// byteBufferにすることで各所にデータを送ります。
		ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4);
		flvHeader.write(header);
		return header;
	}
	/**
	 * Tagの情報からByteBufferのデータを構築し、応答します。
	 * @param tag
	 * @return
	 */
	private ByteBuffer makeTagByteBuffer(ITag tag) {
		if(tag == null) {
			return null;
		}
		try {
			ByteBuffer tagBuffer = null;
			// サイズの計算
			int bodySize = tag.getBodySize();
			int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
			tagBuffer = ByteBuffer.allocate(totalTagSize);
			// tagのタイプ設定
			byte dataType = tag.getDataType();
			// タイムスタンプの取得
			int timestamp = tag.getTimestamp();
			// タグの実体を取得しておきます。
			byte[] bodyBuf = null;
			if(bodySize > 0) {
				bodyBuf = new byte[bodySize];
				tag.getBody().get(bodyBuf);
			}
			// 実際のデータの挿入
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
			return tagBuffer;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * tagデータを確認します。
	 * @param tag
	 */
	private void checkTag(ITag tag) {
		byte dataType = tag.getDataType();
		// httpTakStreamingはそのまま動作してOKなので(あとでメタデータやheaderデータがきても問題ない。)
		if(audioFirstTag == null && dataType != ITag.TYPE_AUDIO) {
			// TODO ここの部分は、コーデック情報等がいれかわったときにも変更する必要があるかもしれない。
			// 新規にタグを発見したので、updateが必要
			audioFirstTag = tag;
			if(takSegmentCreator != null) {
				takSegmentCreator.writeHeaderPacket(makeHeaderByteBuffer(), makeTagByteBuffer(videoFirstTag), makeTagByteBuffer(audioFirstTag));
			}
		}
		if(videoFirstTag == null && dataType != ITag.TYPE_VIDEO) {
			// TODO ここの部分は、コーデック情報等がいれかわったときにも変更する必要があるかもしれない。
			// 新規にタグを発見したので、updateが必要
			videoFirstTag = tag;
			if(takSegmentCreator != null) {
				takSegmentCreator.writeHeaderPacket(makeHeaderByteBuffer(), makeTagByteBuffer(videoFirstTag), makeTagByteBuffer(audioFirstTag));
			}
		}
		// codec情報等解析することが可能ですが、今回は興味がないのでパスします。
		// タグデータをbyteBufferにいれこんで渡します。
		ByteBuffer tagBuffer = makeTagByteBuffer(tag);
		if(takSegmentCreator != null) {
			takSegmentCreator.writeTagData(tagBuffer);
		}
		if(flvDataQueue != null) {
			flvDataQueue.putTagData(tagBuffer);
		}
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
			e.printStackTrace();
		}
	}
}
