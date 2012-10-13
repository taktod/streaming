package com.ttProject.streaming.webm.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ttProject.streaming.data.IMediaPacket;
import com.ttProject.streaming.data.IMediaPacketManager;

/**
 * webmのストリーミングのパケットを管理するマネージャー
 * @author taktod
 */
public class WebMPacketManager implements IMediaPacketManager {
	// ID list
/*	public static final int EBMLId = 0x1A45DFA3;
	public static final int VoidId = 0xEC;
	public static final int CRC32Id = 0xBF;
	public static final int SegmentId = 0x18538067;
	  public static final int InfoId = 0x1549A966; // segment情報にはタイムコードベース情報とかあるので、保持する必要あり。
	    // 以下はffmpegでコンバートしたwebmにあった情報
	    public static final int TimecodeScaleId = 0x2AD7B1;
	    public static final int MuxingAppId = 0x4D80;
	    public static final int WritingAppId = 0x5741;
	    public static final int SegmentUIDId = 0x73A4;
	    // ffmpegでコンバートしたデータにはなかったが取得すべきであろう情報
	    public static final int TimecodeScaleDenominatorId = 0x2AD7B2;
/ *	    // あとでしらべておきたいもの。file分割ストリームできる？
	    public static final int SegmentFilenameId = 0x7384;
	    public static final int PrevUIDId = 0x3CB923;
	    public static final int PrevFilenameId = 0x3C83AB;
	    public static final int NextUIDId = 0x3EB923;
	    public static final int NextFilenameId = 0x3E83BB;* /
	  public static final int TracksId = 0x1654AE6B;
	    public static final int TrackEntryId = 0xAE;
	      public static final int TrackNumberId = 0xD7;
	      public static final int TrackUIDId = 0x73C5;
	      public static final int FlagLacingId = 0x9C;
	      public static final int LanguageId = 0x22B59C;
	      public static final int CodecIDId = 0x86;
	      public static final int TrackTypeId = 0x83; // 1 video 2 audio 3 complex 0x10 logo 0x11 subtitle 0x12 buttons 0x20 control
	      public static final int DefaultDurationId = 0x23E383;
	      public static final int VideoId = 0xE0; // videoSetting
	      public static final int AudioId = 0xE1; // audioSetting*/
	  public static final int ClusterId = 0x1F43B675; // clusterIdがきたら次のパケットとおもっておいた方がよさそう。
	    public static final int TimecodeId = 0xE7; // timecodeだけ、あとで数値がいれやすいように、サイズをかえておく。

	  // 以下の情報はliveStreamingでは使わないもの
/*	  public static final int SeekHeadId = 0x114D9B74;
	  public static final int CuesId = 0x1C53BB6B;
	  public static final int AttachmentsId = 0x1941A469;
	  public static final int ChaptersId = 0x1043A770;
	  // liveStreamingでつかってもいいけど、とりあえず含まれていなかったもの
	  public static final int TagsId = 0x1254C367; */

	/** 読み込みBuffer */
	private ByteBuffer buffer = null;

	// basePacketには、書き込むデータのトップにくるSegmentをいれておく。
	private WebMPacket currentPacket = null;
	/**
	 * byteデータをいれればWebMPacketとして応答する。
	 */
	public List<IMediaPacket> getPackets(byte[] data) {
		if(buffer != null) {
			int length = buffer.remaining() + data.length;
			ByteBuffer newBuffer = ByteBuffer.allocate(length);
			newBuffer.put(buffer);
			buffer = newBuffer;
		}
		else {
			buffer = ByteBuffer.allocate(data.length);
		}
		buffer.put(data);
		buffer.flip();

		List<IMediaPacket> result = new ArrayList<IMediaPacket>();
		// bufferにデータがはいったので読み込んでいく。(サイズは不明)
		while(buffer.remaining() > 0) {
			WebMPacket packet = analizePacket(buffer);
			if(packet == null) {
				// データが足りないので追加を要求する。
				break;
			}
			else {
				// パケットが完成したので、書き込みを実行する。
				packet.writeData();
				result.add(packet);
			}
		}
		return result;
	}
	/**
	 * パケットの内容を解析します。
	 * @param buffer
	 * @return
	 */
	private WebMPacket analizePacket(ByteBuffer buffer) {
		// 最後までいっていないのに、パケットが完成したら、ぬけてしまうっぽい。
		// パケットの内容を確認します。
		WebMPacket packet = currentPacket;
		// ファイルに書き込みが可能なパケットができたら、その時点で応答する。
		if(packet == null) {
			// ヘッダー情報を読み込んでパケットがなにであるか調べる。
			int position = buffer.position();
			Long id = WebMPacket.getEBMLId(buffer);
			if(id == null) {
				// nullの場合はデータがたりないので処理を戻します。
				return null;
			}
			// nullではないのでIDが取得できた。
			if(id == ClusterId) {
				// clusterIDの場合はWebMMediaPacketをつくる。
				packet = new WebMMediaPacket();
			}
			else {
				// clusterIDではじまっていないので、WebMHeaderPacketをつくる。
				packet = new WebMHeaderPacket();
			}
			// bufferの位置をもどしておく。
			buffer.position(position);
		}
		if(packet.analize(buffer)) {
			// 完了した。
			currentPacket = null;
			return packet;
		}
		else {
			// 不完全なので、続行する必要あり。
			currentPacket = packet;
			return null;
		}
	}
}
