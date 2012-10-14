package com.ttProject.streaming.hls.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * HttpLiveStreamingのパケットを管理するマネージャー
 * @author taktod
 */
public class HlsPacketManager {
	/** 読み込みBuffer */
	private ByteBuffer buffer = null;
	private HlsPacket currentPacket = null;
	/**
	 * byteデータをいれればHlsPacketとして応答する。
	 * @param data
	 * @return
	 */
	public List<HlsPacket> getPackets(byte[] data) {
		if(buffer != null) {
			int length = buffer.remaining() + data.length;
			ByteBuffer newBuffer = ByteBuffer.allocate(length);
			newBuffer.put(buffer);
			buffer = newBuffer;
		}
		else {
			buffer = ByteBuffer.allocate(data.length);
		}
		buffer.put(data);
		buffer.flip();
		List<HlsPacket> result = new ArrayList<HlsPacket>();
		// bufferにデータがはいったので、188バイトずつ読み込む
		while(buffer.remaining() > 0) {
			HlsPacket packet = analizePacket(buffer);
			if(packet == null) {
				break;
			}
			else {
				packet.writeData();
				result.add(packet);
			}
		}
		return result;
	}
	/**
	 * パケットの内容を解析します。
	 * @param buffer
	 * @return
	 */
	private HlsPacket analizePacket(ByteBuffer buffer) {
		HlsPacket packet = currentPacket;
		if(packet == null) {
			// mediaかheaderか決めなければいけない。
			if(HlsPacket.isPmtChecked()) {
				// PMTIdがきまっているのでMedia決定
				packet = new HlsMediaPacket();
			}
			else {
				// PMTIdがきまっていないのでheader決定
				packet = new HlsHeaderPacket();
			}
		}
		if(packet.analize(buffer)) {
			currentPacket = null;
			return packet;
		}
		else {
			currentPacket = packet;
			return null;
		}
	}
}
