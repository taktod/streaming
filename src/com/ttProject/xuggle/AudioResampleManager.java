package com.ttProject.xuggle;

import java.util.HashSet;
import java.util.Set;

import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IStreamCoder;

/**
 * コンバートの動作を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここではリサンプルの部分を担当
 * sampleRateとChannelsをみて、目標と違う場合はリサンプラーを作成する必要あり。
 */
public class AudioResampleManager {
	private IAudioResampler resampler = null;
	private Set<AudioEncodeManager> encodeManagers = new HashSet<AudioEncodeManager>();
	// 変換目標
	private int sampleRate;
	private int channels;

	public AudioResampleManager(AudioEncodeManager encodeManager) {
		IStreamCoder audioCoder = encodeManager.getAudioCoder();
		encodeManagers.add(encodeManager);
		setSampleRate(audioCoder.getSampleRate());
		setChannels(audioCoder.getChannels());
	}
	public boolean addEncodeManager(AudioEncodeManager encodeManager) {
		IStreamCoder audioCoder = encodeManager.getAudioCoder();
		if(audioCoder.getSampleRate() == getSampleRate()
		|| audioCoder.getChannels() == getChannels()) {
			encodeManagers.add(encodeManager);
			return true;
		}
		return false;
	}
	/**
	 * 音声をリサンプルします。
	 * @param samples
	 * @return
	 */
	public IAudioSamples resampleAudio(IAudioSamples samples) {
		checkResampler(samples);
		IAudioSamples result = IAudioSamples.make(1024, getChannels());
		int retval = -1;
		retval = resampler.resample(result, samples, samples.getNumSamples());
		if(retval < 0) {
			throw new RuntimeException("audioのリサンプルに失敗しました。");
		}
		return result;
	}
	/**
	 * 入力音声データの確認
	 * @param samples
	 */
	private void checkResampler(IAudioSamples samples) {
		if(resampler == null
		|| samples.getChannels() != resampler.getInputChannels()
		|| samples.getFormat() != resampler.getInputFormat()
		|| samples.getSampleRate() != resampler.getInputRate()) {
			setupResampler(samples);
		}
	}
	/**
	 * リサンプラーの再構成
	 * @param samples
	 */
	private void setupResampler(IAudioSamples samples) {
//		if(resampler != null) {
//			resampler.delete();
//		}
		resampler = IAudioResampler.make(getChannels(), samples.getChannels(), getSampleRate(), samples.getSampleRate());
		if(resampler == null) {
			throw new RuntimeException("audioリサンプラーの作成に失敗しました。");
		}
	}
	public Set<AudioEncodeManager> getEncodeManagers() {
		return encodeManagers;
	}
	private int getSampleRate() {
		return sampleRate;
	}
	private void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}
	private int getChannels() {
		return channels;
	}
	private void setChannels(int channels) {
		this.channels = channels;
	}
	
}

