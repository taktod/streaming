package com.ttProject.xuggle;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.SimpleMediaFile;

public abstract class OutputManager implements IOutputManager {
	private static final ISimpleMediaFile streamInfo = new SimpleMediaFile();
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
}
