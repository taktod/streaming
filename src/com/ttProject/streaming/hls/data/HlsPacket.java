package com.ttProject.streaming.hls.data;

import java.nio.ByteBuffer;

/**
 * HttpLiveStreamingのパケットを解析するプログラム
 * @author taktod
 */
public class HlsPacket {
	// http://www.arib.or.jp/english/html/overview/doc/2-STD-B10v4_7.pdf
	private final ByteBuffer buffer;
	private int pid;
	/**
	 * 
	 */
	public HlsPacket(byte[] buffer) {
		this.buffer = ByteBuffer.allocate(188);
		this.buffer.put(buffer);
		this.buffer.flip();
		analize();
	}
	/**
	 * bufferデータアクセス
	 * @return
	 */
	protected ByteBuffer getBuffer() {
		return buffer;
	}
	private void analize() {
		ByteBuffer buf = getBuffer();
		// 先頭の４バイトを取得する。
		if(buf.get() != 0x47) {
			throw new RuntimeException("syncByteがおかしいです。");
		}
		// PIDを取得する。
		pid = (buf.getShort()) & 0x1FFF;
//		System.out.println(pid);
/*
		// 4,5が0750
		if((buf[4]  == 0x07 && buf[5]  == 0x50)
		&& (buf[11] == 0x00 && buf[12] == 0x00 && buf[13] == 0x00
			&& buf[14] == 0x01 && buf[15] == (byte)0xE0)) {
		}
		else {
			
		}
//		byte[] b = new byte[4];
//		System.out.println(Utils.toHex(b));*/
		buf.rewind();
	}
	/**
	 * pid参照
	 * @return
	 */
	public int getPid() {
		return pid;
	}
}
