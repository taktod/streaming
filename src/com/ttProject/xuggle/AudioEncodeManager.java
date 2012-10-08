package com.ttProject.xuggle;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.streaming.MediaManager;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

/**
 * コンバートの動作を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここではコーデック変換を担当
 * 
 * encodeManagerはaudioもしくはvideoのチャンネルが追加されたら作りなおす必要がでてくる。
 */
public class AudioEncodeManager {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(AudioEncodeManager.class);
	/** 音声コーダー */
	private IStreamCoder audioCoder = null;
	/** 処理対象コンテナ */
	private Set<IContainer> containers = new HashSet<IContainer>();
	/** 処理対象コーデック情報 */
	private ICodec.ID codec;
	private int bitRate;
	private int sampleRate;
	private int channels;
//	private IRational timeBase;
	/**
	 * コンストラクタ
	 * @param mediaManager
	 */
	public AudioEncodeManager(MediaManager mediaManager) {
		ISimpleMediaFile streamInfo = mediaManager.getStreamInfo();
		addContainer(mediaManager.getContainer());
		setCodec(streamInfo.getAudioCodec());
		setBitRate(streamInfo.getAudioBitRate());
		setSampleRate(streamInfo.getAudioSampleRate());
		setChannels(streamInfo.getAudioChannels());
	}
	/**
	 * 対象メディアマネージャーが合致する場合は登録する。
	 * @param mediaManager
	 * @return true:合致した場合 false:合致しない場合
	 */
	public boolean addMediaManager(MediaManager mediaManager) {
		ISimpleMediaFile streamInfo = mediaManager.getStreamInfo();
		if(streamInfo.getAudioCodec().equals(getCodec())
		&& streamInfo.getAudioBitRate() == getBitRate()
		&& streamInfo.getAudioSampleRate() == getSampleRate()
		&& streamInfo.getAudioChannels() == getChannels()) {
			addContainer(mediaManager.getContainer());
			return true;
		}
		return false;
	}
	/**
	 * 登録されている情報でcoderを作成する。
	 */
	public void setupCoder() {
		ICodec outCodec = ICodec.findEncodingCodec(getCodec());
		if(outCodec == null) {
			throw new RuntimeException("audio出力用のコーデックを取得することができませんでした。");
		}
		for(IContainer container : getContainers()) {
			IStream outStream = null;
			if(audioCoder != null) {
				outStream = container.addNewStream(audioCoder);
			}
			else {
				outStream = container.addNewStream(outCodec);
			}
			if(outStream == null) {
				throw new RuntimeException("コンテナ用のストリーム作成失敗");
			}
			if(audioCoder == null) {
				IStreamCoder outCoder = outStream.getStreamCoder();
				outCoder.setBitRate(getBitRate());
				outCoder.setSampleRate(getSampleRate());
				outCoder.setChannels(getChannels());
				outCoder.open(null, null);
				audioCoder = outCoder;
			}
		}
	}
	public void encodeAudio(IAudioSamples samples) {
		int retval = -1;
		IPacket outPacket = IPacket.make();
		int numSamplesConsumed = 0;
		while(numSamplesConsumed < samples.getNumSamples()) {
			retval = audioCoder.encodeAudio(outPacket, samples, numSamplesConsumed);
			if(retval <= 0) {
				logger.warn("audioのエンコードに失敗しましたが、無視して続けます。");
				break;
			}
			numSamplesConsumed += retval;
			if(outPacket.isComplete()) {
				// コンテナにいれます。
				for(IContainer container : getContainers()) {
					container.writePacket(outPacket);
				}
			}
		}
	}
	/**
	 * コンテナ追加動作
	 * @param container
	 */
	private void addContainer(IContainer container) {
		containers.add(container);
	}
	/**
	 * 音声コーダーを参照
	 * @return
	 */
	public IStreamCoder getAudioCoder() {
		return audioCoder;
	}
	/**
	 * 内部保持コンテナを参照
	 * @return
	 */
	private Set<IContainer> getContainers() {
		return containers;
	}
	private ICodec.ID getCodec() {
		return codec;
	}
	private void setCodec(ICodec.ID codec) {
		this.codec = codec;
	}
	private int getBitRate() {
		return bitRate;
	}
	private void setBitRate(int bitRate) {
		this.bitRate = bitRate;
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
