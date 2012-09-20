package com.ttProject.streaming;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ttProject.util.DomHelper;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.SimpleMediaFile;

/**
 * メディアデータを保持すマネージャー
 * nodeを保持しておいて、
 * @author todatakahiko
 */
public abstract class MediaManager {
	/** 出力用のストリーム情報の保持 */
	private final ISimpleMediaFile streamInfo = new SimpleMediaFile();
	/** ffmpegに渡すvideo用のプロパティの詳細 */
	private final Map<String, String> videoProperties = new HashMap<String, String>();
	/** ffmpegに渡すvideo用のフラグデータの詳細 */
	private final Map<IStreamCoder.Flags, Boolean> videoFlags = new HashMap<IStreamCoder.Flags, Boolean>();
	/** 参照先のデータを保持しておく必要あり。 */
	private final Node node;
	
	/** 音声まわりの設定 */
	/**
	 * 音声の有無確認
	 */
	public void setHasAudio(Boolean flg) {
		System.out.println("hasAudio:" + flg);
		streamInfo.setHasAudio(flg);
	}
	/**
	 * 音声ビットレート
	 * @param bitRate
	 */
	public void setAudioBitRate(int bitRate) {
		System.out.println("AudioBitRate:" + bitRate);
		streamInfo.setAudioBitRate(bitRate);
	}
	/**
	 * 音声チャンネル定義 1:モノラル 2:ステレオ等
	 * @param channels
	 */
	public void setAudioChannels(int channels) {
		System.out.println("audioChannels:" + channels);
		streamInfo.setAudioChannels(channels);
	}
	/**
	 * 音声サンプリングレート
	 * @param sampleRate
	 */
	public void setAudioSampleRate(int sampleRate) {
		System.out.println("audioSampleRate:" + sampleRate);
		streamInfo.setAudioSampleRate(sampleRate);
	}
	/**
	 * 音声コーデック
	 * @param codecName
	 */
	public void setAudioCodec(String codecName) {
		try {
			System.out.println("audioCodec:" + codecName);
			streamInfo.setAudioCodec(ICodec.ID.valueOf(codecName));
		}
		catch (Exception e) {
		}
	}
	/** 動画まわりの設定 */
	/**
	 * 映像の有無
	 */
	public void setHasVideo(Boolean flg) {
		System.out.println("hasVideo:" + flg);
		streamInfo.setHasVideo(flg);
	}
	/**
	 * 映像の横幅
	 * @param width
	 */
	public void setVideoWidth(int width) {
		System.out.println("videoWidth:" + width);
		streamInfo.setVideoWidth(width);
	}
	/**
	 * 映像の縦幅
	 * @param height
	 */
	public void setVideoHeight(int height) {
		System.out.println("videoHeight:" + height);
		streamInfo.setVideoHeight(height);
	}
	/**
	 * 映像のビットレート
	 * @param bitRate
	 */
	public void setVideoBitRate(int bitRate) {
		System.out.println("videoBitRate:" + bitRate);
		streamInfo.setVideoBitRate(bitRate);
	}
	/**
	 * 映像のフレームレート
	 * 映像のキーフレーム間隔はgのプロパティでいれてほしい。
	 * @param frameRate
	 */
	public void setVideoFrameRate(int frameRate) {
		System.out.println("videoFrameRate:" + frameRate);
		streamInfo.setVideoFrameRate(IRational.make(1, frameRate));
	}
	/**
	 * globalクオリティー
	 * @param quality
	 */
	public void setVideoGlobalQuality(int quality) {
		System.out.println("videoGlobalQuality:" + quality);
		streamInfo.setVideoGlobalQuality(quality);
	}
	/**
	 * 映像コーデック
	 * @param codecName
	 */
	public void setVideoCodec(String codecName) {
		try {
			System.out.println("videoCodec:" + codecName);
			streamInfo.setVideoCodec(ICodec.ID.valueOf(codecName));
		}
		catch (Exception e) {
		}
	}
	/**
	 * ビデオ用の細かいプロパティー
	 * @param properties
	 */
	public void setVideoProperty(Map<String, String> properties) {
		videoProperties.putAll(properties);
	}
	/**
	 * ビデオ用の細かいフラグ
	 * @param flags
	 */
	public void setVideoFlags(Map<String, Boolean> flags) {
		for(String key : flags.keySet()) {
			videoFlags.put(IStreamCoder.Flags.valueOf(key), flags.get(key));
		}
	}
	/**
	 * コンストラクタ
	 * @param node
	 */
	public MediaManager(Node node) {
		this.node = node;
	}
	/**
	 * 解析開始
	 * @param node
	 */
	public void analize() {
		for(int i = 0;i < node.getChildNodes().getLength();i ++) {
			analize(node.getChildNodes().item(i));
		}
	}
	private void analize(Node node) {
		String nodeName = node.getNodeName();
		if(nodeName.equalsIgnoreCase("audio")) {
			System.out.println("  audio");
			// 各audio要素があるか確認して、あれば、設定する。
			setHasAudio(true);
			setupAudio(node);
		}
		else if(nodeName.equalsIgnoreCase("video")) {
			System.out.println("  video");
			setHasVideo(true);
			setupVideo(node);
		}
		// 解析不可
	}
	private void setupVideo(Node node) {
		// video情報を設定していく。
		// attributeを検索して合致するものがあったら、それを設定に採用。
		NamedNodeMap attributes = node.getAttributes();
		if(attributes != null) {
			for(int i=0;i < attributes.getLength();i ++) {
				Node attribute = attributes.item(i);
				setupVideoElement(attribute.getNodeName(), attribute.getNodeValue(), attribute);
			}
		}
		// childNodesを検索して、合致するものがあったら、それを設定に採用。
		NodeList children = node.getChildNodes();
		if(children != null) {
			for(int i=0;i < children.getLength();i ++) {
				Node child = children.item(i);
				setupVideoElement(child.getNodeName(), DomHelper.getNodeValue(child), child);
			}
		}
	}
	private void setupVideoElement(String name, String value, Node node) {
		if(name == null) {
			return;
		}
		if("properties".equalsIgnoreCase(name)) {
			System.out.println("properties");
			// nodeのさらに子要素について調査する必要あり
			setupProperties(node);
			System.out.println(videoProperties);
		}
		if("flags".equalsIgnoreCase(name)) {
			System.out.println("flags");
			// nodeのさらに子要素について調査する必要あり
			setupFlags(node);
			System.out.println(videoFlags);
		}
		if(value == null || value.trim().equals("")) {
			// 設定値がない場合は、無視する。
			return;
		}
		if("codec".equalsIgnoreCase(name)) {
			setVideoCodec(value);
			return;
		}
		if("bitrate".equalsIgnoreCase(name)) {
			try {
				setVideoBitRate(Integer.parseInt(value));
			}
			catch (Exception e) {
			}
			return;
		}
		if("franerate".equalsIgnoreCase(name)) {
			try {
				setVideoFrameRate(Integer.parseInt(value));
			}
			catch (Exception e) {
			}
			return;
		}
		if("globalQuality".equalsIgnoreCase(name)) {
			try {
				setVideoGlobalQuality(Integer.parseInt(value));
			}
			catch (Exception e) {
			}
			return;
		}
		if("width".equalsIgnoreCase(name)) {
			try {
				setVideoWidth(Integer.parseInt(value));
			}
			catch (Exception e) {
			}
			return;
		}
		if("height".equalsIgnoreCase(name)) {
			try {
				setVideoHeight(Integer.parseInt(value));
			}
			catch (Exception e) {
			}
			return;
		}
		if("ref".equalsIgnoreCase(name)) {
			// リファレンス設定とりいそぎ無視しておく。
			// refの場合は、そのデータがついている場合はnodeのデータとして処理させる。
			ReferenceManager refManager = ReferenceManager.getReferenceData(value);
			if(refManager == null) {
				return;
			}
			setupVideo(refManager.getData());
			return;
		}
	}
	private void setupAudio(Node node) {
		// audio情報を設定していく。
		// attributeを検索して、合致するものがあったら、それを設定に採用。
		NamedNodeMap attributes = node.getAttributes();
		if(attributes != null) {
			for(int i=0;i < attributes.getLength();i ++) {
				Node attribute = attributes.item(i);
				setupAudioElement(attribute.getNodeName(), attribute.getNodeValue());
			}
		}
		// childNodesを検索して、合致するものがあったら、それを設定に採用。
		NodeList children = node.getChildNodes();
		if(children != null) {
			for(int i=0;i < children.getLength();i ++) {
				Node child = children.item(i);
				setupAudioElement(child.getNodeName(), DomHelper.getNodeValue(child));
			}
		}
	}
	private void setupAudioElement(String name, String value) {
		if(name == null || value == null || value.trim().equals("")) {
			// 設定値がない場合は、無視する。
			return;
		}
		if("codec".equalsIgnoreCase(name)) {
			setAudioCodec(value);
			return;
		}
		if("bitRate".equalsIgnoreCase(name)) {
			try {
				setAudioBitRate(Integer.parseInt(value));
			}
			catch (Exception e) {
			}
			return;
		}
		if("channel".equalsIgnoreCase(name)) {
			try {
				setAudioChannels(Integer.parseInt(value));
			}
			catch (Exception e) {
			}
			return;
		}
		if("sampleRate".equalsIgnoreCase(name)) {
			try {
				setAudioSampleRate(Integer.parseInt(value));
			}
			catch (Exception e) {
			}
			return;
		}
		if("ref".equalsIgnoreCase(name)) {
			// リファレンス設定とりいそぎ無視しておく。
			// refの場合は、そのデータがついている場合はnodeのデータとして処理させる。
			ReferenceManager refManager = ReferenceManager.getReferenceData(value);
			if(refManager == null) {
				return;
			}
			setupAudio(refManager.getData());
			return;
		}
	}
	private void setupProperties(Node node) {
		NamedNodeMap attributes = node.getAttributes();
		if(attributes != null) {
			for(int i=0;i < attributes.getLength();i ++) {
				Node attribute = attributes.item(i);
				String name = attribute.getNodeName();
				String value = attribute.getNodeValue();
				if(name != null && value != null && !name.equals("") && !value.equals("")) {
					videoProperties.put(name, value);
				}
			}
		}
		NodeList children = node.getChildNodes();
		if(children != null) {
			for(int i=0;i < children.getLength();i ++) {
				Node child = children.item(i);
				String name = child.getNodeName();
				String value = DomHelper.getNodeValue(child);
				if(name != null && value != null && !name.equals("") && !value.equals("")) {
					videoProperties.put(name, value);
				}
			}
		}
	}
	private void setupFlags(Node node) {
		NamedNodeMap attributes = node.getAttributes();
		if(attributes != null) {
			for(int i=0;i < attributes.getLength();i ++) {
				Node attribute = attributes.item(i);
				String name = attribute.getNodeName();
				String value = attribute.getNodeValue();
				if(name != null && value != null && !name.equals("") && !value.equals("")) {
					videoFlags.put(IStreamCoder.Flags.valueOf(name), value.equals("true"));
				}
			}
		}
		NodeList children = node.getChildNodes();
		if(children != null) {
			for(int i=0;i < children.getLength();i ++) {
				Node child = children.item(i);
				String name = child.getNodeName();
				String value = DomHelper.getNodeValue(child);
				if(name != null && value != null && !name.equals("") && !value.equals("")) {
					videoFlags.put(IStreamCoder.Flags.valueOf(name), value.equals("true"));
				}
			}
		}
	}
	/** 以下抽象メソッド */
//	public abstract boolean test();
	/** 以下取得用 */
	public ISimpleMediaFile getStreamInfo() {
		return streamInfo;
	}
	public Map<String, String> getVideoProperty() {
		return videoProperties;
	}
	public Map<IStreamCoder.Flags, Boolean> getVideoFlags() {
		return videoFlags;
	}
}
