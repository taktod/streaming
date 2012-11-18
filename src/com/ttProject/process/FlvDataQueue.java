package com.ttProject.process;

import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * flv入力データをqueueの形で保持しておくことで、あとから接続が完了してもきちんと動作できるようにしておく。
 * 正直LinkedBlockingQueueを生で利用するだけでいい気がする。
 * @author taktod
 */
public class FlvDataQueue {
	/** データを保持するqueue */
	private LinkedBlockingQueue<ChannelBuffer> dataQueue = new LinkedBlockingQueue<ChannelBuffer>();
	/**
	 * データの登録
	 * @param buffer
	 */
	public void putData(ChannelBuffer buffer) {
		dataQueue.add(buffer);
	}
	/**
	 * queueの解放
	 */
	public void close() {
		if(dataQueue != null) {
			dataQueue.clear();
			dataQueue = null;
		}
	}
	/**
	 * バッファを取得する。
	 * @return
	 */
	public ChannelBuffer getData() throws InterruptedException{
		return dataQueue.take();
	}
}
