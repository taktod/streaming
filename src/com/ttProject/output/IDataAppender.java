package com.ttProject.output;

/**
 * データの追記用インターフェイス
 * @author taktod
 */
public interface IDataAppender {
	/**
	 * バイナリデータの書き込み
	 * @param data
	 * @param size
	 */
	public void writeBytes(byte[] data, int size);
	/**
	 * 文字列データの書き込み
	 * @param data
	 */
	public void writeText(String data);
}
