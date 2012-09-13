package com.ttProject.xuggle.flv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory;
import com.xuggle.xuggler.io.URLProtocolManager;

/**
 * flvのストリームをffmpegに流し込むために、独自の応答規格をつくっておくためのfactoryクラス
 * @author taktod
 */
public class FlvHandlerFactory implements IURLProtocolHandlerFactory {
	/** シングルトンインスタンス */
	private static FlvHandlerFactory instance = new FlvHandlerFactory();
	/** このFactoryに予約されているプロトコル名 */
	public static final String DEFAULT_PROTOCOL = "flvStreamInput";
	/** 内部で処理しているFlvHandlerの保持 */
	private final Map<String, FlvHandler> handlers = new ConcurrentHashMap<String, FlvHandler>();
	/**
	 * コンストラクタ
	 */
	private FlvHandlerFactory() {
		URLProtocolManager manager = URLProtocolManager.getManager();
		manager.registerFactory(DEFAULT_PROTOCOL, this);
	}
	/**
	 * factory取得
	 * @return instance
	 */
	public static FlvHandlerFactory getFactory() {
		if(instance == null) {
			throw new RuntimeException("no flv streamInput Factory");
		}
		return instance;
	}
	/**
	 * 処理Handlerを登録する
	 * @param name
	 * @param handler
	 */
	public void registerHandler(String name, FlvHandler handler) {
		handlers.put(name, handler);
	}
	/**
	 * ffmpegからurlが合致する場合にhandlerを求められます。
	 * @param protocol
	 * @param url
	 * @param flags
	 * @return flvHanler
	 */
	@Override
	public IURLProtocolHandler getHandler(String protocol, String url, int flags) {
		String streamName = URLProtocolManager.getResourceFromURL(url);
		return handlers.get(streamName);
	}
}
