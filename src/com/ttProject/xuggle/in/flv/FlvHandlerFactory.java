package com.ttProject.xuggle.in.flv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory;
import com.xuggle.xuggler.io.URLProtocolManager;

/**
 * FLVのストリームデータをffmpegに流し込むための動作
 * ProtocolHandlerを応答するFactoryクラス(基本シングルトン)
 * @author taktod
 */
public class FlvHandlerFactory implements IURLProtocolHandlerFactory{
	/** シングルトンインスタンス */
	private static FlvHandlerFactory instance = new FlvHandlerFactory();
	/** このFactoryが扱うプロトコル名 */
	public static final String DEFAULT_PROTOCOL = "flvStreamInput";
	/** 内部で処理しているFlvDataInputManagerの保持 */
	private final Map<String, FlvDataInputManager> managers = new ConcurrentHashMap<String, FlvDataInputManager>();
	/**
	 * ffmpegからurlが合致する場合にhandlerを求められます。
	 */
	@Override
	public IURLProtocolHandler getHandler(String protocol, String url, int flags) {
		String streamName = URLProtocolManager.getResourceFromURL(url);
		System.out.println(streamName);
		FlvDataInputManager manager = managers.get(streamName);
		if(manager != null) {
			System.out.println("managerあるよ");
			return manager.getHandler();
		}
		System.out.println("managerないよ");
		return null;
	}
	/**
	 * コンストラクタ
	 */
	private FlvHandlerFactory() {
		System.out.println("flvHandlerFactoryを登録しておきます。");
		// URLProtocolManagerに登録することで、今後(redfile:xxxx)のURL向けの処理はこのfactoryが返すHandlerを利用して動作するようになります。
		URLProtocolManager manager = URLProtocolManager.getManager();
		manager.registerFactory(DEFAULT_PROTOCOL, this);
	}
	/**
	 * factoryの取得
	 * @return
	 */
	public static FlvHandlerFactory getFactory() {
		if(instance == null) {
			throw new RuntimeException("no flvStreamInput factory.");
		}
		return instance;
	}
	/**
	 * マネージャーを登録する。
	 * @param manager
	 */
	public void registerManager(String name, FlvDataInputManager manager) {
		managers.put(name, manager);
	}
}
