package com.ttProject.streaming.tak.data;

import java.nio.ByteBuffer;

public class TakMediaPacket extends TakPacket {
	@Override
	public boolean isHeader() {
		return false;
	}
	public TakMediaPacket(TakHeaderPacket headerPacket) {
		super(headerPacket);
	}
/*	@Override
	public boolean analize(ByteBuffer buffer) {
		// タグデータをいれておくが、こちらはメディアデータがベースとなります。
		return false;
	}*/
}
