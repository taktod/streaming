package com.ttProject.xuggle.out.mpegts;

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
	/**
	 * ffmpegからurlが合致する場合にhandlerが求められます。
	 */
	@Override
	public IURLProtocolHandler getHandler(String protocol, String url, int flags) {
		return new MpegtsHandler();
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
}
