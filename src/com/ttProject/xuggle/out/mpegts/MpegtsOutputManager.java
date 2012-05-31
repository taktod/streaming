package com.ttProject.xuggle.out.mpegts;

/**
 * mpegtsDataoutputを管理するマネージャー
 * @author taktod
 */
public class MpegtsOutputManager {
	private MpegtsHandler mpegtsHandler;
	public MpegtsOutputManager(String name) {
		mpegtsHandler = new MpegtsHandler();
	}
	public MpegtsHandler getHandler() {
		return mpegtsHandler;
	}
}
