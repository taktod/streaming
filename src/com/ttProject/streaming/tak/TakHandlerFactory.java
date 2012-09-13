package com.ttProject.streaming.tak;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory;
import com.xuggle.xuggler.io.URLProtocolManager;

public class TakHandlerFactory implements IURLProtocolHandlerFactory {
	/** シングルトンインスタンス */
	private static TakHandlerFactory instance = new TakHandlerFactory();
	/** このファクトリーが扱うプロトコル */
	public static final String DEFAULT_PROTOCOL = "takStreamOutput";
	/** 内部で処理している、Handlerの保持 */
	private final Map<String, TakHandler> handlers = new ConcurrentHashMap<String, TakHandler>();
	/**
	 * コンストラクタ
	 */
	private TakHandlerFactory() {
		URLProtocolManager manager = URLProtocolManager.getManager();
		manager.registerFactory(DEFAULT_PROTOCOL, this);
	}
	/**
	 * factoryの取得
	 */
	public static TakHandlerFactory getFactory() {
		if(instance == null) {
			throw new RuntimeException("no tak stream output factory");
		}
		return instance;
	}
	/**
	 * 処理Handlerを登録する。
	 */
	public void registerHandler(String name, TakHandler handler) {
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
