package com.ttProject.xuggle;

import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;

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
	// 変換目標
	private int sampleRate;
	private int channels;

	/**
	 * コンストラクタ
	 * @param sampleRate
	 * @param channels
	 */
	public AudioResampleManager(int sampleRate, int channels) {
		this.sampleRate = sampleRate;
		this.channels = channels;
	}
	/**
	 * 音声をリサンプルします。
	 * @param samples
	 * @return
	 */
	public IAudioSamples resampleAudio(IAudioSamples samples) {
		checkResampler(samples);
		IAudioSamples result = IAudioSamples.make(1024, channels);
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
		resampler = IAudioResampler.make(channels, samples.getChannels(), sampleRate, samples.getSampleRate());
		if(resampler == null) {
			throw new RuntimeException("audioリサンプラーの作成に失敗しました。");
		}
	}
}

