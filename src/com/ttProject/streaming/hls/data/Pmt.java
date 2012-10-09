package com.ttProject.streaming.hls.data;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class Pmt extends HlsPacket {
	private Set<Integer> h264Pid = new HashSet<Integer>();
	private Set<Integer> mediaPid = new HashSet<Integer>();
	/**
	 * pmtパケット
	 * @param buffer
	 */
	public Pmt(byte[] buffer) {
		super(buffer);
		analize();
	}
	/**
	 * 解析を実施する。
	 */
	private void analize() {
		// 内容を解析する。
		ByteBuffer buffer = getBuffer();
		buffer.position(5);
		byte buf = buffer.get();
		// テーブル識別0x02 8bit
		if(buf != 0x02) {
			throw new RuntimeException("pmtの識別子がおかしい。");
		}
		// シンタックス指示1 1bit
		// 011 3bit
		buf = buffer.get();
		if((buf & 0xF0) != 0xB0) {
			throw new RuntimeException("シンタクス指示とそれに付随するデータがおかしいです。");
		}
		// セクション長12bit
		int length = ((buf << 8) | buffer.get()) & 0x0FFF;
		// 放送番組識別 16bit 2byte
		length -= 2;
		buffer.getShort();
		// 11 2bit
		// バージョン番号 5bit
		// カレントネクスト指示 1bit
		length --;
		buf = buffer.get(); // 1バイト読み込む
		if((buf & 0xC0) != 0xC0) {
			throw new RuntimeException("ビットフラグが一致してない？");
		}
		// セクション番号
		length --;
		buffer.get(); // 1バイト読み込む
		// 最終セクション番号
		length --;
		buffer.get(); // 1バイト読み込む
		
		// 111 3ビット固定
		// PCR_PID 13ビット 時刻の主体になるパケット情報(とりあえずいらない)
		length --;
		buf = buffer.get();
		if((buf & 0xE0) != 0xE0) {
			throw new RuntimeException("固定bitが一致しません。");
		}
		length --;
		buf = buffer.get();
		// 1111 4ビット固定
		// 番組情報長 12bit
		length -= 2;
		int dataLength = buffer.getShort();
		if((dataLength & 0xF000) != 0xF000) {
			throw new RuntimeException("固定bitが一致しませんでした。");
		}
		dataLength = dataLength & 0x0FFF;
		// dataLength分スキップする。
		length -= dataLength;
		int position = buffer.position() + dataLength;
		buffer.position(position);
		// ここから先がストリームデータ
		while(length > 4) {
			// ストリーム形式種別 8ビット
			length --;
			byte type = buffer.get(); // この値が0x1bならh.264ということになる。
			// 111 3ビット固定
			// PID 13ビット
			length --;
			buf = buffer.get();
			if((buf & 0xE0) != 0xE0) {
				System.out.println(buf & 0xFF);
				throw new RuntimeException("固定bitが一致しませんでした。");
			}
			length --;
			int pid = ((buf & 0x1F) << 8 | buffer.get());
			if(type == 0x1B) {
				// h.264
				// 見つけたpidを保持しておく。
				h264Pid.add(pid);
			}
			// 見つけたpidを保持しておく。
			mediaPid.add(pid);
			// 1111 4ビット固定
			// 情報長 12ビット
			length -= 2;
			dataLength = buffer.getShort();
			if((dataLength & 0xF000) != 0xF000) {
				throw new RuntimeException("固定bitが一致しませんでした。");
			}
			dataLength = dataLength & 0x0FFF;
			// 情報データ(任意の長さ)
			// dataLength分スキップする。
			length -= dataLength;
			position = buffer.position() + dataLength;
			buffer.position(position);
		}
		// crc
		buffer.rewind();
	}
	public boolean isH264(int pid) {
		return h264Pid.contains(pid);
	}
	public boolean isMedia(int pid) {
		return mediaPid.contains(pid);
	}
}
