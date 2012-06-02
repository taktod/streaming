package com.ttProject.xuggle.in.flv;

import com.ttProject.xuggle.InputManager;

/**
 * flvDataInputを管理するマネージャー
 * このクラス存在意義があまりよくわからんw
 * @author taktod
 */
public class FlvInputManager extends InputManager {
	/** このqueueにbyteBufferとしたflvデータをいれていけば、勝手に処理されます。 */
	private FlvDataQueue flvDataQueue;
	private FlvHandler flvHandler;
	
	// 変換元データに必要なプロパティは特にない。
	/**
	 * FlvDataの入力ストリームを管理するマネージャー
	 * @param name
	 */
	public FlvInputManager() {
	}
	/**
	 * queue用オブジェクトを応答する。
	 */
	public FlvDataQueue getQueue() {
		return flvDataQueue;
	}
	/**
	 * flvHandlerを応答する。
	 */
	public FlvHandler getHandler() {
		return flvHandler;
	}
	public String getProtocol() {
		return FlvHandlerFactory.DEFAULT_PROTOCOL;
	}
	public String getFormat() {
		return "flv";
	}
}
