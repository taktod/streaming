package com.ttProject.xuggle.in.flv;

/**
 * flvDataInputを管理するマネージャー
 * このクラス存在意義があまりよくわからんw
 * @author taktod
 */
public class FlvDataInputManager {
	/** このqueueにbyteBufferとしたflvデータをいれていけば、勝手に処理されます。 */
	private FlvDataQueue flvDataQueue;
	private FlvHandler flvHandler;
	/**
	 * FlvDataの入力ストリームを管理するマネージャー
	 * @param name
	 */
	public FlvDataInputManager(String name) {
		flvDataQueue = new FlvDataQueue();
		flvHandler = new FlvHandler(this);
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
}
