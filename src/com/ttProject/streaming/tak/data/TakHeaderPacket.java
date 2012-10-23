package com.ttProject.streaming.tak.data;

import java.nio.ByteBuffer;

public class TakHeaderPacket extends TakPacket {
	public TakHeaderPacket() {
		super(null);
	}
	@Override
	public boolean analize(ByteBuffer buffer) {
		// 特にデータを解析したりはしません。
		// こちらはタグデータをそのまま追記する形でやっていく
		// ここでのデータは有無をいわさず書き込んでしまう。
		return false;
	}
}
