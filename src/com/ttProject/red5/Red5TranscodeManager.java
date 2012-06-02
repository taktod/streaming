package com.ttProject.red5;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.stream.IBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.xuggle.Transcoder;
import com.ttProject.xuggle.in.flv.FlvDataQueue;
import com.ttProject.xuggle.in.flv.FlvHandler;
import com.ttProject.xuggle.in.flv.FlvHandlerFactory;
import com.ttProject.xuggle.in.flv.FlvInputManager;
import com.ttProject.xuggle.out.mpegts.MpegtsHandlerFactory;
import com.ttProject.xuggle.out.mpegts.MpegtsOutputManager;

/**
 * red5のbroadcastされているストリームの変換を定義するマネージャー
 * 今回はここで、すべての出力を定義します。
 * @author taktod
 */
public class Red5TranscodeManager {
	private final Logger logger = LoggerFactory.getLogger(Red5TranscodeManager.class);
	/** httpTakStreamingを生成するかどうか */
	private boolean httpTak = false;
	/** httpLiveStreamingを生成するかどうか */
	private boolean httpLive = false;
	/** jpegMp3Streamingを生成するかどうか */
	private boolean jpegMp3 = false;
	private MpegtsOutputManager mpegtsManager = null;

	public void setMpegtsManager(MpegtsOutputManager mpegtsManager) {
		this.mpegtsManager = mpegtsManager;
	}
	public void setHttpTak(boolean httpTak) {
		this.httpTak = httpTak;
	}
	public void setHttpLive(boolean httpLive) {
		this.httpLive = httpLive;
	}
	public void setJpegMp3(boolean jpegMp3) {
		this.jpegMp3 = jpegMp3;
	}
	private Map<String, Holder> map = new ConcurrentHashMap<String, Red5TranscodeManager.Holder>();
	/**
	 * 変換と登録します。s
	 * @param stream
	 */
	public void registerTranscoder(IBroadcastStream stream) {
		// このクラスはストリームのtranscode命令まわりをセットアップします。
		logger.info("変換操作を構築します。");
		// flvHandlerFactoryに処理のHandlerを登録します。
		FlvHandlerFactory flvFactory = FlvHandlerFactory.getFactory();
		FlvDataQueue inputDataQueue = new FlvDataQueue();
		FlvHandler flvHandler = new FlvHandler(inputDataQueue);
		flvFactory.registerHandler(stream.getName(), flvHandler);

		// MpegtsHandlerFactoryに処理のHandlerを登録します。(今回はコンテナを開くだけで処理をしない。)
		// こっちは適当
		MpegtsHandlerFactory.getFactory();

		// streamListenerを起動させておきおます。
		StreamListener listener = new StreamListener(stream, inputDataQueue, null);
		listener.open();
		Transcoder transcoder = new Transcoder(new FlvInputManager(), mpegtsManager, stream.getName());
		Thread transcodeThread = new Thread(transcoder);
		transcodeThread.setDaemon(true);
		transcodeThread.start();
		Holder holder = new Holder();
		holder.listener = listener;
		holder.trancoder = transcoder;
		map.put(stream.getName(), holder);
	}
	public void unregisterTranscoder(IBroadcastStream stream) {
		Holder holder = map.get(stream.getName());
		holder.listener.close();
		holder.trancoder.close();
	}
	private class Holder {
		// streamListenerとtranscoderをセットで保持しておきます。
		public StreamListener listener;
		public Transcoder trancoder;
	}
}
