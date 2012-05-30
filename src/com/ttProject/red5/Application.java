package com.ttProject.red5;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.stream.IBroadcastStream;

import com.ttProject.xuggle.in.flv.FlvDataQueue_Test;

public class Application extends ApplicationAdapter {
	private StreamListener slistener = null;
	/**
	 * ストリームを開始したときのイベントを拾う
	 */
	@Override
	public void streamBroadcastStart(IBroadcastStream stream) {
		super.streamBroadcastStart(stream);
		// スコープが違っても同じstreamIdになるらしい。
//		stream.stop();
		
		// ここでFlvDataQueueを作成して、Flvに登録されるようにする。
		slistener = new StreamListener(stream, 
				new FlvDataQueue_Test("/Users/todatakahiko/flvData.flv"),
				null);
	}
	/**
	 * ストリームを停止したときのイベントを拾う
	 */
	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		if(slistener != null) {
			slistener.close();
			slistener = null;
		}
		super.streamBroadcastClose(stream);
	}
}
