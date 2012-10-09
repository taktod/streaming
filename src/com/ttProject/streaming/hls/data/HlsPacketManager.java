package com.ttProject.streaming.hls.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HttpLiveStreamingのパケットを管理するマネージャー
 * @author taktod
 */
public class HlsPacketManager {
	public static final int PAT = 0x0000;
	private Pat patPacket = null;
	private final Map<Integer, Pmt> pmtMap = new HashMap<Integer, Pmt>(); 
	/** 読み込みBuffer */
	private ByteBuffer buffer = null;
	/**
	 * 専用のパケットマネージャーを作成する。
	 */
	public HlsPacketManager() {
		
	}
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
		while(buffer.remaining() >= 188) {
			byte[] buf = new byte[188];
			buffer.get(buf);
			result.add(analizePacket(buf));
		}
		return result;
	}
	public HlsPacket analizePacket(byte[] buf) {
		// bufの中身を調査します。
		if(buf[0] != 0x47) {
			throw new RuntimeException("先頭が0x47になっていません。mpegtsとして成立していません。");
		}
		int pid = (buf[1] << 8 | buf[2]) & 0x1FFF;
		if(pid == PAT) {
			// patの場合
			patPacket = new Pat(buf);
			return patPacket;
		}
		// pmtであるか確認
		if(patPacket != null && patPacket.isPMT(pid)) {
			Pmt pmt = new Pmt(buf);
			pmtMap.put(pid, pmt);
			return pmt;
		}
		// h.264であるか確認
		for(Pmt pmt : pmtMap.values()) {
			if(pmt.isH264(pid)) {
				return new H264(buf);
			}
		}
		// その他メディアデータであるか確認
		for(Pmt pmt : pmtMap.values()) {
			if(pmt.isMedia(pid)) {
				return new Media(buf);
			}
		}
		// 該当しないmpegtsパケットの場合
		return new HlsPacket(buf);
	}
	// パケットの解析を実行してPATをつくります。
	public void remain() {
		System.out.println(buffer.remaining());
	}
}
