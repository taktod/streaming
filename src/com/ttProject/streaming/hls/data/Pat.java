package com.ttProject.streaming.hls.data;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Patパケット
 * @author taktod
 */
public class Pat extends HlsPacket {
	/** pmtIDのセット */
	private final Set<Integer> pmtSet = new HashSet<Integer>();
	/**
	 * コンストラクタ
	 * @param buffer
	 */
	public Pat(byte[] buffer) {
		super(buffer);
		analize();
	}
	/**
	 * 内容を解析します。
	 */
	private void analize() {
		ByteBuffer buffer = getBuffer();
		buffer.position(5);
		// テーブル識別 0x00 8bit
		byte buf = buffer.get();
		if(buf != 0x00) {
			throw new RuntimeException("テーブル識別データがおかしいです。");
		}
		// セクションシンタックス指示1 1bit
		// 011 3bit固定
		buf = buffer.get();
		if((buf & 0xF0) != 0xB0) {
			throw new RuntimeException("シンタクス指示とそれに付随するデータがおかしいです。");
		}
		// セクション長12bit
		int length = ((buf << 8) | buffer.get()) & 0x0FFF;
		// トランスポートストリーム識別16 bit
		length -= 2;
		buffer.getShort(); // 2バイト読み込む
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
		while(length > 4) {
			length -= 4;
			byte[] buff = new byte[4];
			buffer.get(buff); // 4バイト読み込む
			if((buff[0] << 8 | buff[1]) != 0) {
				// PMT情報
				pmtSet.add((buff[2] << 8 | buff[3]) & 0x1FFF);
			}
			else {
				// ネットワークPID情報
			}
		}
		// crc32(無視しておく。)
		// bufferを巻き戻しておく。
		buffer.rewind();
	}
	/**
	 * pidがPMTと一致するか確認
	 * @param pid 数値
	 * @return true:PMTパケット false:それ以外
	 */
	public boolean isPMT(int pid) {
		return pmtSet.contains(pid);
	}
}
