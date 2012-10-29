package com.ttProject.streaming.data;

import java.nio.ByteBuffer;

/**
 * Mediaデータは要求された場合に、そのパケットのデータをファイルに書き出す機能があればよい。
 * @author taktod
 */
public interface IMediaPacket {
	/**
	 * データの書き込みを実行する。
	 */
	public void writeData();
	/**
	 * byteBufferを解析します。
	 * @param buffer 解析するネタ
	 * @return true:解析完了パケットが書き込みreadyになっています。false:解析途上もっとデータが必要。
	 */
	public boolean analize(ByteBuffer buffer);
	/**
	 * header用のパケットであるか応答する。
	 * @return
	 */
	public boolean isHeader();
}
