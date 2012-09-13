package com.ttProject.streaming.webm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory;
import com.xuggle.xuggler.io.URLProtocolManager;

public class WebMHandlerFactory implements IURLProtocolHandlerFactory {
	/** シングルトンインスタンス */
	private static WebMHandlerFactory instance = new WebMHandlerFactory();
	/** このファクトリーが扱うプロトコル */
	public static final String DEFAULT_PROTOCOL = "webMStreamOutput";
	/** 内部で処理している、Handlerの保持 */
	private final Map<String, WebMHandler> handlers = new ConcurrentHashMap<String, WebMHandler>();
	/**
	 * コンストラクタ
	 */
	private WebMHandlerFactory() {
		URLProtocolManager manager = URLProtocolManager.getManager();
		manager.registerFactory(DEFAULT_PROTOCOL, this);
	}
	/**
	 * factoryの取得
	 */
	public static WebMHandlerFactory getFactory() {
		if(instance == null) {
			throw new RuntimeException("no webm stream output factory");
		}
		return instance;
	}
	/**
	 * 処理Handlerを登録する。
	 */
	public void registerHandler(String name, WebMHandler handler) {
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
