package com.ttProject.flazr;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
//import com.flazr.io.flv.VideoTag;
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
	private ConcurrentLinkedQueue<FlvAtom> dataQueue = new ConcurrentLinkedQueue<FlvAtom>();
	private FileOutputStream fos;
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
//			VideoTag videoTag = new VideoTag(flvAtom.encode().getByte(0));
 			// queueの中身をすべて外にだして、現在のタイムスタンプ以前のものなら、書き込みを実施する。
			ConcurrentLinkedQueue<FlvAtom> queue = new ConcurrentLinkedQueue<FlvAtom>();
 			while(dataQueue.size() > 0) {
 				FlvAtom data = dataQueue.poll();
				if(data.getHeader().getTime() <= flvAtom.getHeader().getTime()) {
			 		try {
	 		 			// このデータをFlvDataQueueに渡せばOK
	 		 			logger.info("audioTimestamp:" + data.getHeader().getTime());
	 					ByteBuffer buffer = data.write().toByteBuffer();
	 					byte[] dat = new byte[buffer.limit()];
	 					buffer.get(dat);
	 					System.out.write(dat);
	 				}
	 				catch (Exception e) {
	 					logger.error("", e);
	 				}
 				}
 				else {
 					queue.add(data);
 				}
 			}
 			dataQueue = queue;
 			logger.info("queueSize:" + dataQueue.size());
			logger.info("videoTimestamp:" + flvAtom.getHeader().getTime());
			try {
				ByteBuffer buffer = flvAtom.write().toByteBuffer();
				byte[] dat = new byte[buffer.limit()];
				buffer.get(dat);
				System.out.write(dat);
			}
			catch (Exception e) {
				
			}
		}
		else if(flvAtom.getHeader().isAudio()) {
			// audioデータはすべてqueueにいれる。
	 		try {
				// このデータをFlvDataQueueに渡せばOK
				dataQueue.add(flvAtom);
			}
			catch (Exception e) {
				logger.error("", e);
			}
		}
	}
	/**
	 * 閉じる
	 */
	@Override
	public void close() {
		try {
			fos.close();
		}
		catch (Exception e) {
		}
	}
}
