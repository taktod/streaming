package com.ttProject.xuggle;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	/** ロガー */
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(AudioResampleManager.class);
	/** リサンプラー */
	private IAudioResampler resampler = null;
	/** 紐付いているEncodeManager */
	private Set<AudioEncodeManager> encodeManagers = new HashSet<AudioEncodeManager>();
	/** 変換目標データ */
	private int sampleRate;
	private int channels;
	private IAudioSamples.Format format;
	/**
	 * コンストラクタ
	 * @param encodeManager
	 */
	public AudioResampleManager(AudioEncodeManager encodeManager) {
		IStreamCoder audioCoder = encodeManager.getAudioCoder();
		encodeManagers.add(encodeManager);
		setSampleRate(audioCoder.getSampleRate());
		setChannels(audioCoder.getChannels());
		setFormat(audioCoder.getSampleFormat());
	}
	/**
	 * 同じ処理で済むencodeManagerを追加
	 * @param encodeManager
	 * @return true:追加可能な場合 false:リサンプルしたデータが合わない場合
	 */
	public boolean addEncodeManager(AudioEncodeManager encodeManager) {
		IStreamCoder audioCoder = encodeManager.getAudioCoder();
		if(audioCoder.getSampleRate() == getSampleRate()
		&& audioCoder.getChannels() == getChannels()
		&& audioCoder.getSampleFormat().equals(getSampleFormat())) {
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
		|| samples.getSampleRate() != resampler.getInputRate()
		|| !samples.getFormat().equals(getSampleFormat())) {
			setupResampler(samples);
		}
	}
	/**
	 * リサンプラーの再構成
	 * @param samples
	 */
	private void setupResampler(IAudioSamples samples) {
		// リサンプラーがのこっている場合は消す処理をいれた方がいいか？それともnativeの方でgcが橋ってきちんと消えるか？
//		if(resampler != null) {
//			resampler.delete();
//		}
//		IAudioResampler.make(outputChannels, inputChannels, outputRate, inputRate, outputFmt, inputFmt)
		resampler = IAudioResampler.make(getChannels(), samples.getChannels(), getSampleRate(), samples.getSampleRate(), getSampleFormat(), samples.getFormat());
		if(resampler == null) {
			throw new RuntimeException("audioリサンプラーの作成に失敗しました。");
		}
	}
	/**
	 * 紐付いているEncodeManagerを参照
	 * @return
	 */
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
	private IAudioSamples.Format getSampleFormat() {
		return format;
	}
	private void setFormat(IAudioSamples.Format format) {
		this.format = format;
	}
}

