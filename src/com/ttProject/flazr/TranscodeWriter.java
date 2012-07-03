package com.ttProject.flazr;

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.AudioTag;
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
	
	/** 変換動作スレッド */
	Thread transcodeThread = null;

	/** 開始時の動作タイムスタンプ */
	private int startTime = -1;
	
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
			jpegSegmentCreator.setMp3SegmentCreator(mp3SegmentCreator);
		}
		// takSegmenterの設定
		if(EncodePropertyLoader.getTakSegmentCreator() != null) {
			takSegmentCreator = new TakSegmentCreator();
			takSegmentCreator.initialize(name);
		}
		if(mpegtsManager != null) { // flvInputManagerも別途encode.propertiesで定義されているならここで判定候補にした方がいいと思う。
			transcoder = new Transcoder(new FlvInputManager(), mpegtsManager, name, mp3SegmentCreator, jpegSegmentCreator);
			
			FlvHandlerFactory flvFactory = FlvHandlerFactory.getFactory();
			inputDataQueue = new FlvDataQueue();
			FlvHandler flvHandler = new FlvHandler(inputDataQueue);
			flvFactory.registerHandler(name, flvHandler);
			
			MpegtsHandlerFactory mpegtsFactory = MpegtsHandlerFactory.getFactory();
			MpegtsHandler mpegtsHandler = new MpegtsHandler(tsSegmentCreator, transcoder);
			mpegtsFactory.registerHandler(name, mpegtsHandler);
		}

		initialize(); // 初期化して動作開始

		if(mpegtsManager != null) {
			// これは動きっぱなしにはなってません。transcoder.closeできちんととまっている。
			// 別のところ(FlvDataQueueのところでwait状態になってしまうので、interruptかける必要あり。)
			transcodeThread = new Thread(transcoder);
			transcodeThread.setDaemon(true);
			transcodeThread.start();
		}
	}
	/**
	 * 放送が停止したとき
	 */
	public void onUnpublish() {
		logger.info("unpublishきたよ？");
		close();
	}
	/**
	 * 初期化
	 */
	public void initialize() {
		try {
			firstAudioPacket = null;
			firstVideoPacket = null;
			// タイムスタンプのずれ分を登録しておく。
			startTime = -1;
			// headerデータを作成すでにAudio + Videoになっているので、そのままつかわせてもらう。
			// ヘッダ情報を作成した瞬間にデータを送っておく。
			if(takSegmentCreator != null) {
				takSegmentCreator.writeHeaderPacket(FlvAtom.flvHeader().toByteBuffer(), null, null);
			}
			// コンバート用のqueueにもいれておく。
			if(inputDataQueue != null) {
				inputDataQueue.putHeaderData(FlvAtom.flvHeader().toByteBuffer());
			}
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
		if(startTime == -1) {
			startTime = header.getTime();
		}
		header.setTime(header.getTime() - startTime);
//		logger.info("timestamp: {}", header.getTime());
		try {
			// このデータをFlvDataQueueに渡せばOK
			ByteBuffer buf = flvAtom.write().toByteBuffer();
			if(header.isVideo() && firstVideoPacket == null) {
				firstVideoPacket = buf.duplicate();
				firstVideoPacket.position(4);
				firstVideoPacket.putInt(0);
				firstVideoPacket.rewind();
				if(takSegmentCreator != null) {
					VideoTag videoTag = new VideoTag(flvAtom.encode().getByte(0));
					if(videoTag.getCodecType() == VideoTag.CodecType.AVC) {
						// h.264の場合のみfirstパケットをとっておく。
						takSegmentCreator.writeHeaderPacket(
								FlvAtom.flvHeader().toByteBuffer(),
								firstVideoPacket,
								firstAudioPacket);
					}
				}
			}
			else if(header.isAudio() && firstAudioPacket == null) {
				firstAudioPacket = buf.duplicate();
				firstAudioPacket.position(4);
				firstAudioPacket.putInt(0);
				firstAudioPacket.rewind();
				if(takSegmentCreator != null) {
					AudioTag audioTag = new AudioTag(flvAtom.encode().getByte(0));
					if(audioTag.getCodecType() == AudioTag.CodecType.AAC) {
						takSegmentCreator.writeHeaderPacket(
								FlvAtom.flvHeader().toByteBuffer(),
								firstVideoPacket,
								firstAudioPacket);
					}
				}
			}
			if(inputDataQueue != null) {
				inputDataQueue.putTagData(buf);
			}
			if(takSegmentCreator != null) {
				if(header.isVideo()) {
					VideoTag videoTag = new VideoTag(flvAtom.encode().getByte(0));
//					if(videoTag.isKeyFrame()) {
//						logger.info("keyframe detected!");
//					}
					takSegmentCreator.writeTagData(buf, header.getTime(), videoTag.isKeyFrame());
				}
				else {
					takSegmentCreator.writeTagData(buf, header.getTime(), false);
				}
			}
		}
		catch (Exception e) {
			logger.error("データ書き込み中に例外が発生しました。", e);
			throw new RuntimeException(e);
		}
	}
	/**
	 * 閉じる
	 */
	@Override
	public void close() {
		// ストリームの停止と、transcoder等もろもろの停止を実行する必要あり。
		if(transcoder != null) {
			logger.info("transcoderを閉じます。");
			transcoder.close();
			transcoder = null;
		}
		if(transcodeThread != null) {
			logger.info("transcoderのスレッドをとめます。");
			transcodeThread.interrupt();
			transcodeThread = null;
		}
		if(inputDataQueue != null) {
			logger.info("入力queueを閉じます。");
			inputDataQueue.close();
			inputDataQueue = null;
		}
		if(takSegmentCreator != null) {
			logger.info("takセグメント動作を停止します。");
			takSegmentCreator.close();
			takSegmentCreator = null;
		}
		if(jpegSegmentCreator != null) {
			logger.info("jpegセグメント動作を停止します。");
			jpegSegmentCreator.close();
			jpegSegmentCreator = null;
		}
		if(mp3SegmentCreator != null) {
			logger.info("mp3セグメント動作を停止します。");
			mp3SegmentCreator.close();
			mp3SegmentCreator = null;
		}
		if(tsSegmentCreator != null) {
			logger.info("tsセグメント動作を停止します。");
			tsSegmentCreator.close();
			tsSegmentCreator = null;
		}
	}
}
