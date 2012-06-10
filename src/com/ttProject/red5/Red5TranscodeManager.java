package com.ttProject.red5;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.stream.IBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * red5のbroadcastされているストリームの変換を定義するマネージャー
 * 今回はここで、すべての出力を定義します。
 * @author taktod
 */
public class Red5TranscodeManager {
	private final Logger logger = LoggerFactory.getLogger(Red5TranscodeManager.class);
	/** xuggleの出力マネージャー */
	private MpegtsOutputManager mpegtsManager = null;
	private TsSegmentCreator tsSegmentCreator = null;
	private Mp3SegmentCreator mp3SegmentCreator = null;

	public void setOutputManager(MpegtsOutputManager mpegtsManager) {
		this.mpegtsManager = mpegtsManager;
	}
	public void setTsSegmentCreator(TsSegmentCreator creator) {
		this.tsSegmentCreator = creator;
	}
	public void setMp3SegmentCreator(Mp3SegmentCreator creator) {
		this.mp3SegmentCreator = creator;
	}
	private Map<String, Holder> map = new ConcurrentHashMap<String, Red5TranscodeManager.Holder>();
	/**
	 * 変換を登録します。
	 * @param stream
	 */
	public void registerTranscoder(IBroadcastStream stream) {
		// このクラスはストリームのtranscode命令まわりをセットアップします。
		logger.info("変換操作を構築します。");
		String name = stream.getPublishedName();
		// creatorの作成
		TsSegmentCreator tsSegmentCreator = null;
		if(this.tsSegmentCreator != null) {
			tsSegmentCreator = new TsSegmentCreator();
			tsSegmentCreator.initialize(name);
		}
		Mp3SegmentCreator mp3SegmentCreator = null;
		if(this.mp3SegmentCreator != null) {
			mp3SegmentCreator = new Mp3SegmentCreator();
			mp3SegmentCreator.initialize(name, mpegtsManager.getStreamInfo());//こいつはtranscoderに渡す必要あり。
		}
		JpegSegmentCreator jpegSegmentCreator = null;
		TakSegmentCreator takSegmentCreator = null;

		// 変換プロセスの構築
		Transcoder transcoder = new Transcoder(new FlvInputManager(), mpegtsManager, name, mp3SegmentCreator, jpegSegmentCreator);
		// flvHandlerFactoryに処理のHandlerを登録します。
		FlvHandlerFactory flvFactory = FlvHandlerFactory.getFactory();
		FlvDataQueue inputDataQueue = new FlvDataQueue();
		FlvHandler flvHandler = new FlvHandler(inputDataQueue);
		flvFactory.registerHandler(name, flvHandler);

		// MpegtsHandlerFactoryに処理のHandlerを登録します。(今回はコンテナを開くだけで処理をしない。)
		MpegtsHandlerFactory mpegtsFactory = MpegtsHandlerFactory.getFactory();
		MpegtsHandler mpegtsHandler = new MpegtsHandler(tsSegmentCreator, transcoder);
		mpegtsFactory.registerHandler(name, mpegtsHandler);

		// streamListenerを起動させておきおます。
		StreamListener listener = new StreamListener(stream, inputDataQueue, takSegmentCreator);
		listener.open(); // listener稼動

		// transcoder稼動
		Thread transcodeThread = new Thread(transcoder);
		transcodeThread.setDaemon(true);
		transcodeThread.start();

		// 参照保持
		Holder holder = new Holder();
		holder.listener = listener;
		holder.transcoder = transcoder;
		holder.tsSegmentCreator = tsSegmentCreator;
		holder.mp3SegmentCreator = mp3SegmentCreator;
		holder.jpegSegmentCreator = jpegSegmentCreator;
		holder.takSegmentCreator = takSegmentCreator;
		map.put(stream.getName(), holder);
	}
	/**
	 * 変換を停止します。
	 * @param stream
	 */
	public void unregisterTranscoder(IBroadcastStream stream) {
		Holder holder = map.remove(stream.getName());
		holder.close();
	}
	/**
	 * データ保持クラス
	 * @author taktod
	 */
	private class Holder {
		// streamListenerとtranscoderをセットで保持しておきます。
		public StreamListener listener = null;
		public Transcoder transcoder = null;
		public TsSegmentCreator tsSegmentCreator = null;
		public Mp3SegmentCreator mp3SegmentCreator = null;
		public JpegSegmentCreator jpegSegmentCreator = null;
		public TakSegmentCreator takSegmentCreator = null;
		/**
		 * 不要になったオブジェクトをcloseします。
		 */
		public void close() {
			if(listener != null) {
				listener.close();
				listener = null;
			}
			if(transcoder != null) {
				transcoder.close();
				transcoder = null;
			}
			if(tsSegmentCreator != null) {
				tsSegmentCreator.close();
				tsSegmentCreator = null;
			}
			if(mp3SegmentCreator != null) {
				mp3SegmentCreator.close();
				mp3SegmentCreator = null;
			}
			if(jpegSegmentCreator != null) {
				jpegSegmentCreator.close();
				jpegSegmentCreator = null;
			}
			if(takSegmentCreator != null) {
				takSegmentCreator.close();
				takSegmentCreator = null;
			}
		}
	}
}
