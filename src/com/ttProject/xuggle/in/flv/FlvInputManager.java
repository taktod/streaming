package com.ttProject.xuggle.in.flv;

/**
 * flvDataInputを管理するマネージャー
 * このクラス存在意義があまりよくわからんw
 * @author taktod
 */
public class FlvInputManager {
	/**
	 * 利用プロトコルを応答する。
	 * @return
	 */
	public String getProtocol() {
		return FlvHandlerFactory.DEFAULT_PROTOCOL;
	}
	/**
	 * 利用ファイルフォーマットを応答する。
	 * @return
	 */
	public String getFormat() {
		return "flv";
	}
}
