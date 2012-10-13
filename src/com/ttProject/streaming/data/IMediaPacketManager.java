package com.ttProject.streaming.data;

import java.util.List;

/**
 * MediaPacketManagerは解析を依頼した場合に与えられたbyteデータからMediaPacketデータを応答すればよい。
 * @author taktod
 */
public interface IMediaPacketManager {
	/**
	 * IURLProtocolHandlerで取得したbyteデータをそのまま突っ込む
	 * 書き込みReadyになったパケットデータを応答します。
	 * @param data
	 */
	public List<IMediaPacket> getPackets(byte[] data);
}
