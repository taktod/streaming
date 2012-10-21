package com.ttProject.xuggle.flv;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.xuggle.ConvertManager;

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
	/** データを保持しておくqueue */
	private LinkedBlockingQueue<ByteBuffer> dataQueueForReader = new LinkedBlockingQueue<ByteBuffer>();
	/**
	 * headerデータを設定します。
	 */
	public void putHeaderData(ByteBuffer header) {
		ConvertManager convertManager = ConvertManager.getInstance();
		if(convertManager.isProcessingFlvHandler()) {
			dataQueue.add(header.duplicate());
		}
		dataQueueForReader.add(header.duplicate());
	}
	/**
	 * tagデータを更新します。
	 */
	public void putTagData(ByteBuffer tag) {
		ConvertManager convertManager = ConvertManager.getInstance();
		if(convertManager.isProcessingFlvHandler()) {
			dataQueue.add(tag.duplicate());
		}
		dataQueueForReader.add(tag.duplicate());
	}
	/**
	 * 動作を停止するときの動作
	 */
	public void close() {
		if(dataQueue != null) {
			dataQueue.clear();
		}
		if(dataQueueForReader != null) {
			dataQueueForReader.clear();
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
	public ByteBuffer readForReader() {
		ByteBuffer result = null;
		ConvertManager convertManager = ConvertManager.getInstance();
		try {
			// takeを利用して、データが存在しない場合は待つようにしておく。
			if(convertManager.isProcessingFlvHandler()) {
				result = dataQueueForReader.poll(); // 速攻で応答を返せばそれでよい。
			}
			else {
				result = dataQueueForReader.take(); // 速攻で応答を返せばそれでよい。
			}
			return result;
		}
		catch (Exception e) {
			logger.error("dataQueueの取得で例外が発生しました。");
			// 例外がでたときにはnullを応答しておく。
			return null;
		}
	}
}
