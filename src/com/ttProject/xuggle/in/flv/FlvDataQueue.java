package com.ttProject.xuggle.in.flv;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * xuggleのIURLProtocolHandlerはffmpegが処理のトリガーになってしまっているので、次のような動作にする。
 * １：データをqueueにためておく。
 * ２：ffmpegから要求があれば、queueから必要数のデータを取り出して応答する。
 * パケットデータを解析して、音声or映像がきちんと存在しているかの判定は必要ないことがわかったので、パスしてます。
 * 
 * @author toktod
 */
public class FlvDataQueue {
	/** データを保持しておく。queue */
	LinkedBlockingQueue<ByteBuffer> dataQueue = new LinkedBlockingQueue<ByteBuffer>();

	/**
	 * headerデータを更新します。
	 * @param header
	 */
	public void putHeaderData(ByteBuffer header) {
		// コンバート開始前のデータ待ちになっている状態の場合はflvHeaderを保持しておかないとだめ。(保持しておいて、スタートするタイミングですべて書き込む方が吉)
		// すでにコンバートが成立しているときにheaderの生成依頼がきた場合は、ffmpegのコンバートを再度実行しなおさないとだめ。
		dataQueue.add(header.duplicate());
		// 仮にここでデータをflvファイルに追加するようにしておき、動作させていく。
	}
	/**
	 * tagデータを更新します。
	 * @param tag
	 */
	public void putTagData(ByteBuffer tag) {
		dataQueue.add(tag.duplicate());
		// 仮にここでデータをflvファイルに追加するようにしておき、ffmpegにとおして正しいファイルと認識されるか確認する。
	}
	/**
	 * 動作が停止するときの動作
	 */
	public void close() {
		// 特にすることはないか？
		dataQueue.clear(); // dataQueueの待ちがある場合にこまる。
		dataQueue = null;
	}
	/**
	 * queueから要素を読み込んで処理を実行する。
	 * @return
	 * @throws InterruptedException
	 */
	public ByteBuffer read() {
		ByteBuffer result = null;
		// 待たずに応答を返すようにしたいと思います。データがなければNullが応答されます。
		try {
			result = dataQueue.poll();
			return result;
		}
		catch (Exception e) {
			// 例外がでた場合は、nullを応答しておく。
			return null;
		}
	}
}
