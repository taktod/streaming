package com.ttProject.flazr;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.flazr.io.flv.VideoTag;
import com.flazr.io.flv.VideoTag.FrameType;
import com.flazr.rtmp.RtmpHeader;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpWriter;
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
public class FlvWriter implements RtmpWriter {
	/** ロガー */
	private static final Logger logger = LoggerFactory.getLogger(FlvWriter.class);

	/** 動作ストリーム名 */
//	private String name;
	/** 各チャンネルの時刻保持 */
	private final int[] channelTimes = new int[RtmpHeader.MAX_CHANNEL_ID];
	private int primaryChannel = -1;
	
	private Transcoder transcoder = null;
	private FlvDataQueue inputDataQueue = null;
	/**
	 * コンストラクタ
	 * @param name
	 */
	public FlvWriter(String name) {
//		this.name = name;
		// TODO 対象ストリームに対するプログラムをいれておく。(transcoderの定義とかそのあたり)
		MpegtsOutputManager mpegtsManager = new MpegtsOutputManager();
		mpegtsManager.setHasAudio(true);
		mpegtsManager.setAudioBitRate(64000);
		mpegtsManager.setAudioChannels(2);
		mpegtsManager.setAudioSampleRate(44100);
		mpegtsManager.setAudioCodec("CODEC_ID_MP3");
		mpegtsManager.setHasVideo(true);
		mpegtsManager.setVideoWidth(320);
		mpegtsManager.setVideoHeight(240);
		mpegtsManager.setVideoBitRate(300000);
		mpegtsManager.setVideoFrameRate(15);
		mpegtsManager.setVideoGlobalQuality(0);
		mpegtsManager.setVideoCodec("CODEC_ID_H264");
		
		mpegtsManager.setVideoProperty(new HashMap<String, String>(){
			private static final long serialVersionUID = 1L;
		{
			put("level", "30");
			put("coder", "0");
			put("qmin", "10");
			put("async", "4");
			put("bf", "0");
			put("wprefp", "0");
			put("cmp", "+chroma");
			put("partitions", "-parti8x8+parti4x4+partp8x8+partp4x4-partb8x8");
			put("me_method", "hex");
			put("subq", "5");
			put("me_range", "16");
			put("g", "250");
			put("keyint_min", "25");
			put("sc_threshold", "40");
			put("i_qfactor", "0.71");
			put("b_strategy", "1");
			put("qcomp", "0.6");
			put("qmax", "30");
			put("qdiff", "4");
			put("directpred", "1");
			put("cqp", "0");
		}});
		mpegtsManager.setVideoFlags(new HashMap<String, Boolean>() {
			private static final long serialVersionUID = 1L;
		{
			put("FLAG_LOOP_FILTER", true);
			put("FLAG_CLOSED_GOP", true);
			put("FLAG2_FASTPSKIP", true);
			put("FLAG2_WPRED", false);
			put("FLAG2_8X8DCT", false);
		}});
		transcoder = new Transcoder(new FlvInputManager(), mpegtsManager, name, null, null);
		
		FlvHandlerFactory flvFactory = FlvHandlerFactory.getFactory();
		inputDataQueue = new FlvDataQueue();
		FlvHandler flvHandler = new FlvHandler(inputDataQueue);
		flvFactory.registerHandler(name, flvHandler);
		
		MpegtsHandlerFactory mpegtsFactory = MpegtsHandlerFactory.getFactory();
		MpegtsHandler mpegtsHandler = new MpegtsHandler(null, transcoder);
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
