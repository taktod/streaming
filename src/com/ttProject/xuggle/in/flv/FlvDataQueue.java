package com.ttProject.xuggle.in.flv;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * xuggleのIURLProtocolHandlerはffmpegが処理のトリガーになってしまっているので、次のような動作にする。
 * １：データをqueueにためておく。
 * ２：ffmpegから要求があれば、queueから必要数のデータを取り出して応答する。
 * @author toktod
 */
public class FlvDataQueue {
	/** データを保持しておく。queue */
	LinkedBlockingQueue<ByteBuffer> dataQueue = new LinkedBlockingQueue<ByteBuffer>();
	/**
	 * headerデータを設定します。
	 * @param header
	 */
	public void putHeaderData(ByteBuffer header) {
		dataQueue.add(header.duplicate());
	}
	/**
	 * tagデータを更新します。
	 * @param tag
	 */
	public void putTagData(ByteBuffer tag) {
		dataQueue.add(tag.duplicate());
	}
	/**
	 * 動作が停止するときの動作
	 */
	public void close() {
		if(dataQueue != null) {
			dataQueue.clear(); // dataQueueの待ちがある場合にこまる。
			dataQueue = null;
		}
	}
	/**
	 * queueから要素を読み込んで処理を実行する。
	 * @return
	 * @throws InterruptedException
	 */
	public ByteBuffer read() {
		ByteBuffer result = null;
		try {
			// takeを利用して、内容がない場合は、データが届くまで待つようにします。
			result = dataQueue.take();
			return result;
		}
		catch (Exception e) {
			// 例外がでた場合は、nullを応答しておく。
			return null;
		}
	}
}
