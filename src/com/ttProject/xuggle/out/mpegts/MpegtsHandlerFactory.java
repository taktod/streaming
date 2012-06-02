package com.ttProject.xuggle.out.mpegts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory;
import com.xuggle.xuggler.io.URLProtocolManager;

/**
 * mpegtsのストリームデータを受け取るための動作
 * @author taktod
 */
public class MpegtsHandlerFactory implements IURLProtocolHandlerFactory {
	/** シングルトンインスタンス */
	private static MpegtsHandlerFactory instance = new MpegtsHandlerFactory();
	/** このファクトリーが扱うインスタンス */
	public static final String DEFAULT_PROTOCOL = "mpegtsStreamOutput";
	/** 内部で処理しているMpegtsHandlerの保持 */
	private final Map<String, MpegtsHandler> handlers = new ConcurrentHashMap<String, MpegtsHandler>();
	/**
	 * ffmpegからurlが合致する場合にhandlerが求められます。
	 */
	@Override
	public IURLProtocolHandler getHandler(String protocol, String url, int flags) {
		String streamName = URLProtocolManager.getResourceFromURL(url);
		return handlers.get(streamName);
	}
	/**
	 * コンストラクタ
	 */
	private MpegtsHandlerFactory() {
		URLProtocolManager manager = URLProtocolManager.getManager();
		manager.registerFactory(DEFAULT_PROTOCOL, this);
	}
	/**
	 * factoryの取得
	 */
	public static MpegtsHandlerFactory getFactory() {
		if(instance == null) {
			throw new RuntimeException("no mpegtsStreamOutput factory");
		}
		return instance;
	}
	/**
	 * 処理Handlerを登録する。
	 */
	public void registerHandler(String name, MpegtsHandler handler) {
		handlers.put(name, handler);
	}
}
