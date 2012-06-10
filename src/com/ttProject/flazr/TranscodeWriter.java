package com.ttProject.flazr;

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.flazr.io.flv.VideoTag;
import com.flazr.io.flv.VideoTag.FrameType;
import com.flazr.rtmp.RtmpHeader;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpWriter;
import com.ttProject.streaming.JpegSegmentCreator;
import com.ttProject.streaming.Mp3SegmentCreator;
import com.ttProject.streaming.TsSegmentCreator;
import com.ttProject.xuggle.Transcoder;
import com.ttProject.xuggle.in.flv.FlvDataQueue;
import com.ttProject.xuggle.in.flv.FlvHandler;
import com.ttProject.xuggle.in.flv.FlvHandlerFactory;
import com.ttProject.xuggle.in.flv.FlvInputManager;
import com.ttProject.xuggle.out.mpegts.MpegtsHandler;
import com.ttProject.xuggle.out.mpegts.MpegtsHandlerFactory;
import com.ttProject.xuggle.out.mpegts.MpegtsOutputManager;

/**
 * 独自のFlvWriterの動作
 * 受け取ったRtmpMessageを分解してFlvDataQueueにデータを流しこんでやる。
 * オリジナルはClientHandlerの内部で生成されています。
 * @author taktod
 */
public class TranscodeWriter implements RtmpWriter {
	/** ロガー */
	private static final Logger logger = LoggerFactory.getLogger(TranscodeWriter.class);

	/** 各チャンネルの時刻保持 */
	private final int[] channelTimes = new int[RtmpHeader.MAX_CHANNEL_ID];
	private int primaryChannel = -1;
	
	private Transcoder transcoder = null;
	private FlvDataQueue inputDataQueue = null;
	/**
	 * コンストラクタ
	 * @param name
	 */
	public TranscodeWriter(String name) {
		// encode.propertiesから変換に関するデータを読み込んでおく。
		MpegtsOutputManager mpegtsManager = EncodePropertyLoader.getMpegtsOutputManager();
		// tsSegmenterの設定
		TsSegmentCreator tsSegmentCreator = null;
		if(EncodePropertyLoader.getTsSegmentCreator() != null) {
			tsSegmentCreator = new TsSegmentCreator();
			tsSegmentCreator.initialize(name);
		}
		// mp3Segmenterの設定
		Mp3SegmentCreator mp3SegmentCreator = null;
		if(EncodePropertyLoader.getMp3SegmentCreator() != null) {
			mp3SegmentCreator = new Mp3SegmentCreator();
			mp3SegmentCreator.initialize(name, mpegtsManager.getStreamInfo());
		}
		// jpegSegmenterの設定
		JpegSegmentCreator jpegSegmentCreator = null;
		if(EncodePropertyLoader.getJpegSegmentCreator() != null) {
			jpegSegmentCreator = new JpegSegmentCreator();
			jpegSegmentCreator.initialize(name);
		}
		transcoder = new Transcoder(new FlvInputManager(), mpegtsManager, name, mp3SegmentCreator, jpegSegmentCreator);
		
		FlvHandlerFactory flvFactory = FlvHandlerFactory.getFactory();
		inputDataQueue = new FlvDataQueue();
		FlvHandler flvHandler = new FlvHandler(inputDataQueue);
		flvFactory.registerHandler(name, flvHandler);
		
		MpegtsHandlerFactory mpegtsFactory = MpegtsHandlerFactory.getFactory();
		MpegtsHandler mpegtsHandler = new MpegtsHandler(tsSegmentCreator, transcoder);
		mpegtsFactory.registerHandler(name, mpegtsHandler);

		initialize(); // 初期化して動作開始
		
		Thread transcodeThread = new Thread(transcoder);
		transcodeThread.setDaemon(true);
		transcodeThread.start();
	}
	/**
	 * 初期化
	 */
	public void initialize() {
		try {
			// headerデータを作成すでにAudio + Videoになっているので、そのままつかわせてもらう。
			// このデータをFlvDataQueueに渡せばOK
			ByteBuffer buffer = FlvAtom.flvHeader().toByteBuffer(); // しかも都合がいいことにnio.ByteBufferになってる。
			inputDataQueue.putHeaderData(buffer);
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
//				logger.info("disposable writerを発見。");
			}
		}
 		try {
			// このデータをFlvDataQueueに渡せばOK
			ByteBuffer buffer = flvAtom.write().toByteBuffer();
			inputDataQueue.putTagData(buffer);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void close() {
		// ストリームの停止と、transcoder等もろもろの停止を実行する必要あり。
		if(transcoder != null) {
			transcoder.close();
			transcoder = null;
		}
		if(inputDataQueue != null) {
			inputDataQueue.close();
			inputDataQueue = null;
		}
	}
}
