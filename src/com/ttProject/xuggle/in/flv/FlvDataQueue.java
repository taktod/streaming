package com.ttProject.xuggle.in.flv;

import java.io.FileOutputStream;
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
	private FileOutputStream fos;
	public FlvDataQueue() {
		try {
			fos = new FileOutputStream("/home/poepoemix/www/stest/sample.flv");
		}
		catch (Exception e) {
		}
	}
	/**
	 * headerデータを設定します。
	 * @param header
	 */
	public void putHeaderData(ByteBuffer header) {
		dataQueue.add(header.duplicate());
		try {
			ByteBuffer buf = header.duplicate();
			byte[] data = new byte[buf.limit()];
			buf.get(data);
			fos.write(data);
		}
		catch (Exception e) {
		}
	}
	/**
	 * tagデータを更新します。
	 * @param tag
	 */
	public void putTagData(ByteBuffer tag) {
		dataQueue.add(tag.duplicate());
		try {
			ByteBuffer buf = tag.duplicate();
			byte[] data = new byte[buf.limit()];
			buf.get(data);
			fos.write(data);
		}
		catch (Exception e) {
		}
	}
	/**
	 * 動作が停止するときの動作
	 */
	public void close() {
		if(dataQueue != null) {
			dataQueue.clear(); // dataQueueの待ちがある場合にこまる。
			dataQueue = null;
		}
		if(fos != null) {
			try {
				fos.close();
			}
			catch (Exception e) {
			}
			fos = null;
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
