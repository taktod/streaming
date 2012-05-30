package com.ttProject.xuggle.in.flv;

import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory;
import com.xuggle.xuggler.io.URLProtocolManager;

/**
 * FLVのストリームデータをffmpegに流し込むための動作
 * ProtocolHandlerを応答するFactoryクラス(基本シングルトン)
 * @author taktod
 *
 */
public class FlvHandlerFactory implements IURLProtocolHandlerFactory{
	/** シングルトンインスタンス */
	private static FlvHandlerFactory instance = new FlvHandlerFactory();
	/** このFactoryが扱うプロトコル名 */
	public static final String DEFAULT_PROTOCOL="flvStream";
	/**
	 * ffmpegからurlが合致する場合にhandlerを求められます。
	 */
	@Override
	public IURLProtocolHandler getHandler(String protocol, String url, int flags) {
		return null;
	}
	/**
	 * factoryの取得
	 * @return
	 */
	public static FlvHandlerFactory getFactory() {
		if(instance == null) {
			throw new RuntimeException("no redfile factory.");
		}
		// URLProtocolManagerに登録することで、今後(redfile:xxxx)のURL向けの処理はこのfactoryが返すHandlerを利用して動作するようになります。
		URLProtocolManager manager = URLProtocolManager.getManager();
		manager.registerFactory(DEFAULT_PROTOCOL, instance);
		return instance;
	}
}
