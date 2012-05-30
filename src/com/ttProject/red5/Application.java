package com.ttProject.red5;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.stream.IBroadcastStream;

public class Application extends ApplicationAdapter {
	/**
	 * ストリームを開始したときのイベントを拾う
	 */
	@Override
	public void streamBroadcastStart(IBroadcastStream stream) {
		super.streamBroadcastStart(stream);
		// スコープが違っても同じstreamIdになるらしい。
		stream.stop();
	}
	/**
	 * ストリームを停止したときのイベントを拾う
	 */
	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		super.streamBroadcastClose(stream);
	}
}
