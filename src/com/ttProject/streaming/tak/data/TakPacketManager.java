package com.ttProject.streaming.tak.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ttProject.streaming.data.IMediaPacket;
import com.ttProject.streaming.data.IMediaPacketManager;

public class TakPacketManager implements IMediaPacketManager {
	/** 書き込みBuffer */
	private ByteBuffer buffer = null;
	
	private TakPacket currentPacket = null;
	private TakHeaderPacket headerPacket = null; // ヘッダパケット

	@Override
	public List<IMediaPacket> getPackets(byte[] data) {
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
		
		List<IMediaPacket> result = new ArrayList<IMediaPacket>();
		while(buffer.remaining() > 0) {
			TakPacket packet = analizePacket(buffer);
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
	private TakPacket analizePacket(ByteBuffer buffer) {
		TakPacket packet = currentPacket;
		if(packet == null) {
			int position = buffer.position();
			// headerかどうか判定する。
			// httpTakStreamingについては、判定できないので(中途で更新される可能性もある)
			// この動作ってmediaパケットとheaderパケットの違いって意味あるんだろうか？(生データ変換なら、中途でデータが追加されることもありうる(xuggle変換なら、中途でパケットが増えてもコンテナ開き直しになる。))
			if(headerPacket == null) {
				headerPacket = new TakHeaderPacket();
			}
			packet = new TakMediaPacket(headerPacket);
		}
		if(packet.analize(buffer)) {
			currentPacket = null;
			return packet;
		}
		else {
			currentPacket = packet;
			return null;
		}
//		System.exit(0);
//		return null;
	}
}
