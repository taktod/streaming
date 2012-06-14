package com.ttProject.flazr;

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.flazr.io.flv.VideoTag;
import com.flazr.rtmp.RtmpHeader;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpWriter;
import com.ttProject.flazr.ex.EncodePropertyLoader;
import com.ttProject.streaming.JpegSegmentCreator;
import com.ttProject.streaming.Mp3SegmentCreator;
import com.ttProject.streaming.TakSegmentCreator;
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

	/** 変換動作用のオブジェクト */
	private MpegtsOutputManager mpegtsManager = null;
	private Transcoder   transcoder     = null;
	private FlvDataQueue inputDataQueue = null;
	private TakSegmentCreator  takSegmentCreator  = null;
	private TsSegmentCreator   tsSegmentCreator   = null;
	private Mp3SegmentCreator  mp3SegmentCreator  = null;
	private JpegSegmentCreator jpegSegmentCreator = null;
	
	/** 動作名 */
	private final String name;
	
	/** takSegmenterのために、audioとvideoの第一パケットは保持しておかなければいけない・・・のかな？本当に */
	private ByteBuffer firstAudioPacket = null;
	private ByteBuffer firstVideoPacket = null;
	
	/**
	 * コンストラクタ
	 * @param name
	 */
	public TranscodeWriter(String name) {
		this.name = name;
		// encode.propertiesから変換に関するデータを読み込んでおく。
		mpegtsManager = EncodePropertyLoader.getMpegtsOutputManager();
		onPublish();
	}
	/**
	 * 放送が開始したとき
	 */
	public void onPublish() {
		// 前の動作がのこっている場合は、いったん停止する。
		close();
		// tsSegmenterの設定
		if(EncodePropertyLoader.getTsSegmentCreator() != null) {
			tsSegmentCreator = new TsSegmentCreator();
			tsSegmentCreator.initialize(name);
		}
		// mp3Segmenterの設定
		if(EncodePropertyLoader.getMp3SegmentCreator() != null) {
			mp3SegmentCreator = new Mp3SegmentCreator();
			mp3SegmentCreator.initialize(name, mpegtsManager.getStreamInfo());
		}
		// jpegSegmenterの設定
		if(EncodePropertyLoader.getJpegSegmentCreator() != null) {
			jpegSegmentCreator = new JpegSegmentCreator();
			jpegSegmentCreator.initialize(name);
		}
		// takSegmenterの設定
		logger.info("ここにきた。");
		if(EncodePropertyLoader.getTakSegmentCreator() != null) {
			logger.info("nullではない");
			takSegmentCreator = new TakSegmentCreator();
			takSegmentCreator.initialize(name);
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

		// これは動きっぱなしにはなってません。transcoder.closeできちんととまっている。
		Thread transcodeThread = new Thread(transcoder);
		transcodeThread.setDaemon(true);
		transcodeThread.start();
	}
	/**
	 * 放送が停止したとき
	 */
	public void onUnpublish() {
		close();
	}
	/**
	 * 初期化
	 */
	public void initialize() {
		try {
			// headerデータを作成すでにAudio + Videoになっているので、そのままつかわせてもらう。
			// ヘッダ情報を作成した瞬間にデータを送っておく。
			if(takSegmentCreator != null) {
				takSegmentCreator.writeHeaderPacket(FlvAtom.flvHeader().toByteBuffer(), null, null);
			}
			// コンバート用のqueueにもいれておく。
			inputDataQueue.putHeaderData(FlvAtom.flvHeader().toByteBuffer());
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
		// firstパケットは保持しておく必要がある。
		RtmpHeader header = flvAtom.getHeader();
		try {
			// このデータをFlvDataQueueに渡せばOK
			ByteBuffer buf = flvAtom.write().toByteBuffer();
			if(header.isVideo() && firstVideoPacket == null) {
				firstVideoPacket = buf.duplicate();
				firstVideoPacket.position(4);
				firstVideoPacket.putInt(0);
				firstVideoPacket.rewind();
				logger.info("packet: {}", firstVideoPacket);
				if(takSegmentCreator != null) {
					takSegmentCreator.writeHeaderPacket(
							FlvAtom.flvHeader().toByteBuffer(),
							firstVideoPacket,
							firstAudioPacket);
				}
			}
			else if(header.isAudio() && firstAudioPacket == null) {
				firstAudioPacket = buf.duplicate();
				firstAudioPacket.position(4);
				firstAudioPacket.putInt(0);
				firstAudioPacket.rewind();
				if(takSegmentCreator != null) {
					takSegmentCreator.writeHeaderPacket(
							FlvAtom.flvHeader().toByteBuffer(),
							firstVideoPacket,
							firstAudioPacket);
				}
			}
			inputDataQueue.putTagData(buf);
			if(takSegmentCreator != null) {
				if(header.isVideo()) {
					VideoTag videoTag = new VideoTag(flvAtom.encode().getByte(0));
					takSegmentCreator.writeTagData(buf, header.getTime(), videoTag.isKeyFrame());
				}
				else {
					takSegmentCreator.writeTagData(buf, header.getTime(), false);
				}
			}
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
		if(takSegmentCreator != null) {
			takSegmentCreator.close();
			takSegmentCreator = null;
		}
	}
}
