package com.ttProject.xuggle.in.flv;

/**
 * flvDataInputを管理するマネージャー
 * このクラスはbeanやpropertiesでの定義情報を保持するためのものです。
 * が、入力フォーマットは定義するものではなく、ffmpegが勝手に解釈するので、することは特にありません。
 * @author taktod
 */
public class FlvInputManager {
	/**
	 * 動作プロトコル定義
	 * @return
	 */
	public String getProtocol() {
		return FlvHandlerFactory.DEFAULT_PROTOCOL;
	}
	/**
	 * 動作フォーマット
	 * @return
	 */
	public String getFormat() {
		return "flv";
	}
}
