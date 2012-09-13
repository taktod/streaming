package com.ttProject.streaming.hls;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory;
import com.xuggle.xuggler.io.URLProtocolManager;

public class HlsHandlerFactory implements IURLProtocolHandlerFactory {
	/** シングルトンインスタンス */
	private static HlsHandlerFactory instance = new HlsHandlerFactory();
	/** このファクトリーが扱うプロトコル */
	public static final String DEFAULT_PROTOCOL = "HlsStreamOutput";
	/** 内部で処理している、Handlerの保持 */
	private final Map<String, HlsHandler> handlers = new ConcurrentHashMap<String, HlsHandler>();
	/**
	 * コンストラクタ
	 */
	private HlsHandlerFactory() {
		URLProtocolManager manager = URLProtocolManager.getManager();
		manager.registerFactory(DEFAULT_PROTOCOL, this);
	}
	/**
	 * factoryの取得
	 */
	public static HlsHandlerFactory getFactory() {
		if(instance == null) {
			throw new RuntimeException("no hls stream output factory");
		}
		return instance;
	}
	/**
	 * 処理Handlerを登録する。
	 */
	public void registerHandler(String name, HlsHandler handler) {
		handlers.put(name, handler);
	}
	/**
	 * handlerを取得する(ffmpeg用)
	 */
	@Override
	public IURLProtocolHandler getHandler(String protocol, String url, int flags) {
		String streamName = URLProtocolManager.getResourceFromURL(url);
		return handlers.get(streamName);
	}
}
