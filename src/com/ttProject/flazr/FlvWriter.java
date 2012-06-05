package com.ttProject.flazr;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.flazr.io.flv.VideoTag;
import com.flazr.io.flv.VideoTag.FrameType;
import com.flazr.rtmp.RtmpHeader;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpWriter;
import com.flazr.rtmp.message.MessageType;

/**
 * 独自のFlvWriterの動作
 * 受け取ったRtmpMessageを分解してFlvDataQueueにデータを流しこんでやる。
 * オリジナルはClientHandlerの内部で生成されています。
 * @author taktod
 */
public class FlvWriter implements RtmpWriter {
	/** ロガー */
	private static final Logger logger = LoggerFactory.getLogger(FlvWriter.class);

	/** 動作ストリーム名 */
	private String name;
	/** 各チャンネルの時刻保持 */
	private final int[] channelTimes = new int[RtmpHeader.MAX_CHANNEL_ID];
	private int primaryChannel = -1;
	/**
	 * コンストラクタ
	 * @param name
	 */
	public FlvWriter(String name) {
		this.name = name;
		// TODO 対象ストリームに対するプログラムをいれておく。(transcoderの定義とかそのあたり)
		initialize(); // 初期化して動作開始
	}
	/**
	 * 初期化
	 */
	public void initialize() {
		try {
			// headerデータを作成すでにAudio + Videoになっているので、そのままつかわせてもらう。
			// このデータをFlvDataQueueに渡せばOK
			FlvAtom.flvHeader().toByteBuffer(); // しかも都合がいいことにnio.ByteBufferになってる。
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 書き込み呼び出し
	 */
	@Override
	public void write(RtmpMessage message) {
		final RtmpHeader header = message.getHeader();
		if(header.isAggregate()) {
			final ChannelBuffer in = message.encode();
			while(in.readable()) {
				final FlvAtom flvAtom = new FlvAtom(in);
				final int absoluteTime = flvAtom.getHeader().getTime();
				channelTimes[primaryChannel] = absoluteTime;
				write(flvAtom); // 書き込む
			}
		}
		else { // metadata audio videoの場合
			final int channelId = header.getChannelId();
			channelTimes[channelId] = header.getTime();
            if(primaryChannel == -1 && (header.isAudio() || header.isVideo())) {
            	// 先に見つけたデータをprimaryデータとして扱う。？
                logger.info("first media packet for channel: {}", header);
                primaryChannel = channelId;
            }
            if(header.getSize() <= 2) { // サイズが小さすぎる場合は不正な命令として無視する？
            	return;
            }
            write(new FlvAtom(header.getMessageType(), channelTimes[channelId], message.encode()));
		}
	}
	/**
	 * 書き込みの実際の動作
	 * @param flvAtom
	 */
	private void write(final FlvAtom flvAtom) {
		if(flvAtom.getHeader().isVideo()) {
			VideoTag videoTag = new VideoTag(flvAtom.encode().getByte(0));
			if(videoTag.getFrameType() == FrameType.DISPOSABLE_INTER) {
				// TODO red5のときにxuggleに渡さなかったdisposable interframe. flazrならいけるか？
			}
		}
		try {
			// このデータをFlvDataQueueに渡せばOK
			flvAtom.write().toByteBuffer();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void close() {
		// ストリームの停止と、transcoder等もろもろの停止を実行する必要あり。
	}
}
