package com.ttProject.flazr;

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.red5.io.utils.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.flazr.io.flv.VideoTag;
import com.flazr.io.flv.VideoTag.FrameType;
import com.flazr.rtmp.RtmpHeader;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpWriter;

/**
 * 標準出力にデータを書き出すWriter
 * 標準出力に書き出すことで、ffmpegにパイプ接続できることを期待します。
 * @author taktod
 */
public class StdoutWriter implements RtmpWriter {
	/** ロガー */
	private static final Logger logger = LoggerFactory.getLogger(StdoutWriter.class);
	private final int[] channelTimes = new int[RtmpHeader.MAX_CHANNEL_ID];
	private int primaryChannel = -1;
	/**
	 * コンストラクタ
	 */
	public StdoutWriter() {
		try {
			ByteBuffer buffer = FlvAtom.flvHeader().toByteBuffer();
			byte[] data = new byte[buffer.limit()];
			buffer.get(data);
			System.out.write(data);
//			System.out.println(HexDump.toHexString(data));
		}
		catch (Exception e) {
			logger.error("", e);
		}
	}
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
				// よくわからんDTSエラーがでる。timestampがひっくり返るデータができることはHttpTakStreamingをつくったときにわかっているので、その兼ね合いですかね？
				return;
			}
		}
 		try {
			// このデータをFlvDataQueueに渡せばOK
			ByteBuffer buffer = flvAtom.write().toByteBuffer();
			byte[] data = new byte[buffer.limit()];
			buffer.get(data);
			System.out.write(data);
//			System.out.println(HexDump.toHexString(data));
		}
		catch (Exception e) {
			logger.error("", e);
		}
	}
	/**
	 * 閉じる
	 */
	@Override
	public void close() {
	}
}
