package com.ttProject.xuggle.flv;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * flvの入力データqueueを保持するクラス
 * xuggleのIURLProtocolHandlerはffmpegが処理のトリガーになっています。
 * １：データをqueueにいれておく。
 * ２：ffmpegから要求をうけとったら、要求に応じてデータを応答する。(flvの形式)
 * 
 * @author taktod
 */
public class FlvDataQueue {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(FlvDataQueue.class);
	/** データを保持しておくqueue */
	private LinkedBlockingQueue<ByteBuffer> dataQueue = new LinkedBlockingQueue<ByteBuffer>();
	/**
	 * headerデータを設定します。
	 */
	public void putHeaderData(ByteBuffer header) {
		dataQueue.add(header.duplicate());
	}
	/**
	 * tagデータを更新します。
	 */
	public void putTagData(ByteBuffer tag) {
		dataQueue.add(tag.duplicate());
	}
	/**
	 * 動作を停止するときの動作
	 */
	public void close() {
		if(dataQueue != null) {
			dataQueue.clear();
		}
	}
	/**
	 * queueから要素を読み込んで処理を実行する。
	 */
	public ByteBuffer read() {
		ByteBuffer result = null;
		try {
			// takeを利用して、データが存在しない場合は待つようにしておく。
			result = dataQueue.take(); // 一度draintoをつかって、邪魔にならないように調整したが、やっぱりとめておく。必要ないと思う。
			return result;
		}
		catch (InterruptedException e) {
			// threadの動作が阻害された場合はそのまま抜ける(動作がとまっただけ)
			return null;
		}
		catch (Exception e) {
			logger.error("dataQueueの取得で例外が発生しました。");
			// 例外がでたときにはnullを応答しておく。
			return null;
		}
	}
}
