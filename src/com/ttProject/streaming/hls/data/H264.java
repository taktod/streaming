package com.ttProject.streaming.hls.data;

import java.nio.ByteBuffer;

/**
 * h.264のパケット解析用
 * @author taktod
 */
public class H264 extends Media {
	private boolean isKey = false;
	public H264(byte[] buffer) {
		super(buffer);
		analize();
	}
	private void analize() {
//		System.out.println("h.264解析");
		// key frameか確認しておく。(このやり方があっているのかは不明)
		ByteBuffer buf = getBuffer();
		buf.position(4);
		int data = buf.getShort();
		if(data != 0x0750) {
//			System.out.println("キーパケットではなさそう。");
			return;
		}
		buf.position(12);
		data = buf.getInt();
		if(data != 0x000001E0) {
//			System.out.println("キーパケットではなさそう２。");
			return;
		}
		isKey = true;
//		System.out.println("キーパケットだった。");
	}
	/**
	 * h.264のキーパケットであるか確認
	 * @return
	 */
	public boolean isKey() {
		return isKey;
	}
}
