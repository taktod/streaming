package com.ttProject.flazr;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.ttProject.streaming.JpegSegmentCreator;
import com.ttProject.streaming.Mp3SegmentCreator;
import com.ttProject.streaming.TakSegmentCreator;
import com.ttProject.streaming.TsSegmentCreator;
import com.ttProject.xuggle.in.flv.FlvInputManager;
import com.ttProject.xuggle.out.mpegts.MpegtsOutputManager;

/**
 * encode.propertiesのデータを読み込むローダー
 */
public class EncodePropertyLoader {
	private static FlvInputManager     flvInputManager     = null;
	private static MpegtsOutputManager mpegtsOutputManager = null;
	private static TsSegmentCreator   tsSegmentCreator   = null;
	private static Mp3SegmentCreator  mp3SegmentCreator  = null;
	private static JpegSegmentCreator jpegSegmentCreator = null;
	private static TakSegmentCreator  takSegmentCreator  = null;
	/**
	 * 読み込みを実行する。
	 */
	static {
		// 入力は固定でOKということにしておきます。
		flvInputManager = new FlvInputManager();
		try {
			Properties encodeProp = new Properties();
			encodeProp.load(new FileInputStream("encode.properties"));
			setupMpegtsOutputManager(encodeProp);
			setupTsSegmentCreator(encodeProp);
			setupTakSegmentCreator(encodeProp);
			setupMp3SegmentCreator(encodeProp);
			setupJpegSegmentCreator(encodeProp);
		}
		catch (Exception e) {
		}
	}
	/**
	 * mpegtsの出力定義を作成する。
	 * @param encodeProp
	 */
	private static void setupMpegtsOutputManager(Properties prop) throws Exception {
		Map<String, String> propMap = null;
		Map<String, Boolean> flagMap = null;
		if("true".equals(prop.get("mpegtsOutputManager"))) {
			mpegtsOutputManager = new MpegtsOutputManager();
			for(Object _key : prop.keySet()) {
				String key = (String)_key;
				if(!key.startsWith("mpegtsOutputManager")) {
					continue;
				}
				if("mpegtsOutputManager.audioBitRate".equals(key)) {
					mpegtsOutputManager.setAudioBitRate(Integer.parseInt(prop.getProperty(key)));
				}
				if("mpegtsOutputManager.audioChannels".equals(key)) {
					mpegtsOutputManager.setAudioChannels(Integer.parseInt(prop.getProperty(key)));
				}
				if("mpegtsOutputManager.audioSampleRate".equals(key)) {
					mpegtsOutputManager.setAudioSampleRate(Integer.parseInt(prop.getProperty(key)));
				}
				if("mpegtsOutputManager.audioCodec".equals(key)) {
					mpegtsOutputManager.setAudioCodec(prop.getProperty(key));
				}
				if("mpegtsOutputManager.videoWidth".equals(key)) {
					mpegtsOutputManager.setVideoWidth(Integer.parseInt(prop.getProperty(key)));
				}
				if("mpegtsOutputManager.videoHeight".equals(key)) {
					mpegtsOutputManager.setVideoHeight(Integer.parseInt(prop.getProperty(key)));
				}
				if("mpegtsOutputManager.videoBitRate".equals(key)) {
					mpegtsOutputManager.setVideoBitRate(Integer.parseInt(prop.getProperty(key)));
				}
				if("mpegtsOutputManager.videoFrameRate".equals(key)) {
					mpegtsOutputManager.setVideoFrameRate(Integer.parseInt(prop.getProperty(key)));
				}
				if("mpegtsOutputManager.videoGlobalQuality".equals(key)) {
					mpegtsOutputManager.setVideoGlobalQuality(Integer.parseInt(prop.getProperty(key)));
				}
				if("mpegtsOutputManager.videoCodec".equals(key)) {
					mpegtsOutputManager.setVideoCodec(prop.getProperty(key));
				}
				// videoProperty
				if(key.startsWith("mpegtsOutputManager.videoProperty.")) {
					if(propMap == null) {
						propMap = new HashMap<String, String>();
					}
					String propKey = key.replace("mpegtsOutputManager.videoProperty.", "");
					propMap.put(propKey, prop.getProperty(key));
				}
				// videoFlags
				if(key.startsWith("mpegtsOutputManager.videoFlags.")) {
					if(flagMap == null) {
						flagMap = new HashMap<String, Boolean>();
					}
					String flagKey = key.replace("mpegtsOutputManager.videoFlags.", "");
					flagMap.put(flagKey, "true".equals(prop.getProperty(key)));
				}
			}
			if(propMap != null) {
				mpegtsOutputManager.setVideoProperty(propMap);
			}
			if(flagMap != null) {
				mpegtsOutputManager.setVideoFlags(flagMap);
			}
		}
		else {
			mpegtsOutputManager = null;
		}
	}
	private static void setupTsSegmentCreator(Properties prop) {
		if("true".equals(prop.get("tsSegmentCreator"))) {
			tsSegmentCreator = new TsSegmentCreator();
			for(Object _key : prop.keySet()) {
				String key = (String)_key;
				if(!key.startsWith("tsSegmentCreator")) {
					continue;
				}
				if("tsSegmentCreator.duration".equals(key)) {
					tsSegmentCreator.setDuration(Integer.parseInt(prop.getProperty(key)));
				}
				if("tsSegmentCreator.tmpPath".equals(key)) {
					tsSegmentCreator.setTmpPath(prop.getProperty(key));
				}
			}
		}
		else {
			tsSegmentCreator = null;
		}
	}
	private static void setupMp3SegmentCreator(Properties prop) {
		if("true".equals(prop.get("mp3SegmentCreator"))) {
			mp3SegmentCreator = new Mp3SegmentCreator();
			for(Object _key : prop.keySet()) {
				String key = (String)_key;
				if(!key.startsWith("mp3SegmentCreator")) {
					continue;
				}
				if("mp3SegmentCreator.duration".equals(key)) {
					mp3SegmentCreator.setDuration(Integer.parseInt(prop.getProperty(key)));
				}
				if("mp3SegmentCreator.tmpPath".equals(key)) {
					mp3SegmentCreator.setTmpPath(prop.getProperty(key));
				}
			}
		}
		else {
			mp3SegmentCreator = null;
		}
	}
	private static void setupJpegSegmentCreator(Properties prop) {
		if("true".equals(prop.get("jpegSegmentCreator"))) {
			jpegSegmentCreator = new JpegSegmentCreator();
			for(Object _key : prop.keySet()) {
				String key = (String)_key;
				if(!key.startsWith("jpegSegmentCreator")) {
					continue;
				}
				if("jpegSegmentCreator.duration".equals(key)) {
					jpegSegmentCreator.setDuration(Integer.parseInt(prop.getProperty(key)));
				}
				if("jpegSegmentCreator.tmpPath".equals(key)) {
					jpegSegmentCreator.setTmpPath(prop.getProperty(key));
				}
			}
		}
		else {
			jpegSegmentCreator = null;
		}
	}
	private static void setupTakSegmentCreator(Properties prop) {
		if("true".equals(prop.get("takSegmentCreator"))) {
			takSegmentCreator = new TakSegmentCreator();
			for(Object _key : prop.keySet()) {
				String key = (String)_key;
				if(!key.startsWith("takSegmentCreator")) {
					continue;
				}
			}
		}
		else {
			takSegmentCreator = null;
		}
	}
	public static FlvInputManager getFlvInputManager() {
		return flvInputManager;
	}
	public static MpegtsOutputManager getMpegtsOutputManager() {
		return mpegtsOutputManager;
	}
	public static TsSegmentCreator getTsSegmentCreator() {
		return tsSegmentCreator;
	}
	public static Mp3SegmentCreator getMp3SegmentCreator() {
		return mp3SegmentCreator;
	}
	public static JpegSegmentCreator getJpegSegmentCreator() {
		return jpegSegmentCreator;
	}
	public static TakSegmentCreator getTakSegmentCreator() {
		return takSegmentCreator;
	}
	
}
