package com.ttProject.xuggle.out.mpegts;

import java.util.HashMap;
import java.util.Map;

import com.ttProject.xuggle.OutputManager;
import com.xuggle.xuggler.IStreamCoder;

/**
 * mpegtsDataoutputを管理するマネージャー
 * @author taktod
 */
public class MpegtsOutputManager extends OutputManager {
	private static final Map<String, String> videoProperties = new HashMap<String, String>();
	private static final Map<IStreamCoder.Flags, Boolean> videoFlags = new HashMap<IStreamCoder.Flags, Boolean>();
	private MpegtsHandler mpegtsHandler;
	// 出力側はいろんなパラメーターを受け取る必要があり、これをトランスコーダーで利用する。
	public MpegtsOutputManager(){
	}
	public MpegtsOutputManager(String name) {
		mpegtsHandler = new MpegtsHandler();
	}
	public MpegtsHandler getHandler() {
		return mpegtsHandler;
	}
	public void setVideoProperty(Map<String, String> properties) {
		videoProperties.putAll(properties);
	}
	public void setVideoFlags(Map<String, Boolean> flags) {
		for(String key : flags.keySet()) {
			videoFlags.put(IStreamCoder.Flags.valueOf(key), flags.get(key));
		}
	}
}
