package com.ttProject.streaming.data;

/**
 * Mediaデータは要求された場合に、そのパケットのデータをファイルに書き出す機能があればよい。
 * @author taktod
 */
public interface IMediaPacket {
	public void writeData();
}
