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
	private FlvInputManager     flvInputManager     = null;
	private MpegtsOutputManager mpegtsOutputManager = null;
	private TsSegmentCreator   tsSegmentCreator   = null;
	private Mp3SegmentCreator  mp3SegmentCreator  = null;
	private JpegSegmentCreator jpegSegmentCreator = null;
	private TakSegmentCreator  takSegmentCreator  = null;
	/**
	 * 読み込みを実行する。
	 */
	public void load() {
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
	private void setupMpegtsOutputManager(Properties prop) throws Exception {
		Map<String, String> propMap = null;
		Map<String, Boolean> flagMap = null;
		if("true".equals(prop.get("mpegtsOutputManager"))) {
			mpegtsOutputManager = new MpegtsOutputManager();
			for(Object _key : prop.keySet()) {
				String key = (String)_key;
				if(!(key).startsWith("mpegtsOutputManager")) {
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
	private void setupTsSegmentCreator(Properties encodeProp) {
		if("true".equals(encodeProp.get("tsSegmentCreator"))) {
			
		}
		else {
			tsSegmentCreator = null;
		}
	}
	private void setupMp3SegmentCreator(Properties encodeProp) {
		if("true".equals(encodeProp.get("mp3SegmentCreator"))) {
			
		}
		else {
			mp3SegmentCreator = null;
		}
	}
	private void setupJpegSegmentCreator(Properties encodeProp) {
		if("true".equals(encodeProp.get("jpegSegmentCreator"))) {
			
		}
		else {
			jpegSegmentCreator = null;
		}
	}
	private void setupTakSegmentCreator(Properties encodeProp) {
		if("true".equals(encodeProp.get("takSegmentCreator"))) {
			
		}
		else {
			takSegmentCreator = null;
		}
	}
}
