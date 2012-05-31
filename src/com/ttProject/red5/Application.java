package com.ttProject.red5;

import java.util.Map;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.stream.IBroadcastStream;

import com.ttProject.xuggle.in.flv.FlvDataInputManager;
import com.ttProject.xuggle.in.flv.FlvHandlerFactory;
import com.ttProject.xuggle.out.mpegts.MpegtsHandlerFactory;
import com.ttProject.xuggle.out.mpegts.MpegtsOutputManager;

public class Application extends ApplicationAdapter {
	private Map<String, String> test;

	public void setTest(Map<String, String> test) {
		this.test = test;
//		System.out.println(test);
	}
	private StreamListener slistener = null;
	/**
	 * ストリームを開始したときのイベントを拾う
	 */
	@Override
	public void streamBroadcastStart(IBroadcastStream stream) {
		super.streamBroadcastStart(stream);
		// スコープが違っても同じstreamIdになるらしい。

		FlvDataInputManager flvInputManager = new FlvDataInputManager("test");
		FlvHandlerFactory.getFactory().registerManager("test", flvInputManager);
		MpegtsOutputManager mpegtsOutputManager = new MpegtsOutputManager("test");
		MpegtsHandlerFactory.getFactory();
		// ここでFlvDataQueueを作成して、Flvに登録されるようにする。
		slistener = new StreamListener(stream, 
				flvInputManager.getQueue(),
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
