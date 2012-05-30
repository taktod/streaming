package com.ttProject.xuggle.in.flv;

import java.nio.ByteBuffer;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.flv.FLVHeader;

/**
 * xuggleのIURLProtocolHandlerはffmpegが処理のトリガーになってしまっているので、次のような動作にする。
 * １：データをqueueにためておく。
 * ２：ffmpegから要求があれば、queueから必要数のデータを取り出して応答する。
 * パケットデータを解析して、音声or映像がきちんと存在しているかの判定は必要ないことがわかったので、パスしてます。
 * 
 * 今回のrtmp -> flv変換の元ネタはred5 1.0.0rc2のorg.red5.io.flv.impl.FLVWriterとします。
 * @author toktod
 */
public class FlvDataQueue {
	/**
	 * headerデータを更新します。
	 * @param header
	 * @param metaData
	 * @param videoFirstTag
	 * @param audioFirstTag
	 */
	public void putHeaderData(ByteBuffer header) {
		// コンバート開始前のデータ待ちになっている状態の場合はflvHeaderを保持しておかないとだめ。(保持しておいて、スタートするタイミングですべて書き込む方が吉)
		// すでにコンバートが成立しているときにheaderの生成依頼がきた場合は、ffmpegのコンバートを再度実行しなおさないとだめ。
	}
	/**
	 * tagデータを更新します。
	 * @param tag
	 */
	public void putTagData(ByteBuffer header) {
	}
}
