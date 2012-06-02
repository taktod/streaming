package com.ttProject.xuggle.out.mpegts;

import java.util.HashMap;
import java.util.Map;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.SimpleMediaFile;

/**
 * mpegtsDataoutputを管理するマネージャー
 * @author taktod
 */
public class MpegtsOutputManager {
	private static final ISimpleMediaFile streamInfo = new SimpleMediaFile();
	private static final Map<String, String> videoProperties = new HashMap<String, String>();
	private static final Map<IStreamCoder.Flags, Boolean> videoFlags = new HashMap<IStreamCoder.Flags, Boolean>();
	private MpegtsHandler mpegtsHandler;
	// 出力側はいろんなパラメーターを受け取る必要があり、これをトランスコーダーで利用する。
	/**
	 * 定義されたstreamInfoを取得します
	 * @return
	 */
	public ISimpleMediaFile getStreamInfo() {
		return streamInfo;
	}
	// beanによる設定部、音声
	public void setHasAudio(Boolean flg) {
		streamInfo.setHasAudio(flg);
	}
	public void setAudioBitRate(int bitRate) {
		streamInfo.setAudioBitRate(bitRate);
	}
	public void setAudioChannels(int channels) {
		streamInfo.setAudioChannels(channels);
	}
	public void setAudioSampleRate(int sampleRate) {
		streamInfo.setAudioSampleRate(sampleRate);
	}
	public void setAudioCodec(String codecName) {
		try {
			streamInfo.setAudioCodec(ICodec.ID.valueOf(codecName));
		}
		catch (Exception e) {
		}
	}
	// beanによる設定部、映像
	public void setHasVideo(Boolean flg) {
		streamInfo.setHasVideo(flg);
	}
	public void setVideoWidth(int width) {
		streamInfo.setVideoWidth(width);
	}
	public void setVideoHeight(int height) {
		streamInfo.setVideoHeight(height);
	}
	public void setVideoBitRate(int bitRate) {
		streamInfo.setVideoBitRate(bitRate);
	}
	public void setVideoFrameRate(int frameRate) {
		streamInfo.setVideoFrameRate(IRational.make(1, frameRate));
	}
	public void setVideoGlobalQuality(int quality) {
		streamInfo.setVideoGlobalQuality(quality);
	}
	public void setVideoCodec(String codecName) {
		try {
			streamInfo.setVideoCodec(ICodec.ID.valueOf(codecName));
		}
		catch (Exception e) {
		}
	}
	public MpegtsHandler getHandler() {
		return mpegtsHandler;
	}
	public void setVideoProperty(Map<String, String> properties) {
		videoProperties.putAll(properties);
	}
	public Map<String, String> getVideoProperty() {
		return videoProperties;
	}
	public void setVideoFlags(Map<String, Boolean> flags) {
		for(String key : flags.keySet()) {
			videoFlags.put(IStreamCoder.Flags.valueOf(key), flags.get(key));
		}
	}
	public Map<IStreamCoder.Flags, Boolean> getVideoFlags() {
		return videoFlags;
	}
	public String getProtocol() {
		return MpegtsHandlerFactory.DEFAULT_PROTOCOL;
	}
	public String getFormat() {
		return "mpegts";
	}
}
