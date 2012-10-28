package com.ttProject.streaming;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ttProject.util.DomHelper;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
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
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(MediaManager.class);
	/** 出力用のストリーム情報の保持 */
	private final ISimpleMediaFile streamInfo = new SimpleMediaFile();
	/** ffmpegに渡すvideo用のプロパティの詳細 */
	private final Map<String, String> videoProperties = new HashMap<String, String>();
	/** ffmpegに渡すvideo用のフラグデータの詳細 */
	private final Map<IStreamCoder.Flags, Boolean> videoFlags = new HashMap<IStreamCoder.Flags, Boolean>();
	/** コンテナデータ */
	private IContainer container = null;
	/**
	 * コンテナデータの設定
	 * @param container
	 */
	protected void setContainer(IContainer container) {
		this.container = container;
	}
	/**
	 * コンテナデータの参照
	 * @return
	 */
	public IContainer getContainer() {
		return container;
	}
	/** 参照先のデータを保持しておく必要あり。 */
	private final Node node;
	/** 出力ディレクトリ設定 */
	private String name;
	
	/** 音声まわりの設定 */
	/**
	 * 音声の有無確認
	 */
	public void setHasAudio(Boolean flg) {
//		logger.info("hasAudio:" + flg);
		streamInfo.setHasAudio(flg);
	}
	/**
	 * 音声ビットレート
	 * @param bitRate
	 */
	public void setAudioBitRate(int bitRate) {
//		logger.info("AudioBitRate:" + bitRate);
		streamInfo.setAudioBitRate(bitRate);
	}
	/**
	 * 音声チャンネル定義 1:モノラル 2:ステレオ等
	 * @param channels
	 */
	public void setAudioChannels(int channels) {
//		logger.info("audioChannels:" + channels);
		streamInfo.setAudioChannels(channels);
	}
	/**
	 * 音声サンプリングレート
	 * @param sampleRate
	 */
	public void setAudioSampleRate(int sampleRate) {
//		logger.info("audioSampleRate:" + sampleRate);
		streamInfo.setAudioSampleRate(sampleRate);
	}
	/**
	 * 音声コーデック
	 * @param codecName
	 */
	public void setAudioCodec(String codecName) {
		try {
//			logger.info("audioCodec:" + codecName);
			streamInfo.setAudioCodec(ICodec.ID.valueOf("CODEC_ID_" + codecName.toUpperCase()));
		}
		catch (Exception e) {
			logger.error("audioCodec取得ミス", e);
		}
	}
	/** 動画まわりの設定 */
	/**
	 * 映像の有無
	 */
	public void setHasVideo(Boolean flg) {
//		logger.info("hasVideo:" + flg);
		streamInfo.setHasVideo(flg);
	}
	/**
	 * 映像の横幅
	 * @param width
	 */
	public void setVideoWidth(int width) {
//		logger.info("videoWidth:" + width);
		streamInfo.setVideoWidth(width);
	}
	/**
	 * 映像の縦幅
	 * @param height
	 */
	public void setVideoHeight(int height) {
//		logger.info("videoHeight:" + height);
		streamInfo.setVideoHeight(height);
	}
	/**
	 * 映像のビットレート
	 * @param bitRate
	 */
	public void setVideoBitRate(int bitRate) {
//		logger.info("videoBitRate:" + bitRate);
		streamInfo.setVideoBitRate(bitRate);
	}
	/**
	 * 映像のフレームレート
	 * 映像のキーフレーム間隔はgのプロパティでいれてほしい。
	 * @param frameRate
	 */
	public void setVideoFrameRate(int frameRate) {
//		logger.info("videoFrameRate:" + frameRate);
		streamInfo.setVideoFrameRate(IRational.make(1, frameRate));
	}
	/**
	 * globalクオリティー
	 * @param quality
	 */
	public void setVideoGlobalQuality(int quality) {
//		logger.info("videoGlobalQuality:" + quality);
		streamInfo.setVideoGlobalQuality(quality);
	}
	/**
	 * 映像コーデック
	 * @param codecName
	 */
	public void setVideoCodec(String codecName) {
		try {
//			logger.info("videoCodec:" + codecName);
			streamInfo.setVideoCodec(ICodec.ID.valueOf("CODEC_ID_" + codecName.toUpperCase()));
		}
		catch (Exception e) {
			logger.error("videoCodec取得ミス", e);
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
//		this.streamInfo.setAudioTimeBase(IRational.make(1, 1000));
		this.streamInfo.setVideoTimeBase(IRational.make(1, 1000));
	}
	/**
	 * 解析開始
	 * @param node
	 */
	public void analize() {
		for(int i = 0;i < node.getChildNodes().getLength();i ++) {
			analize(node.getChildNodes().item(i));
		}
		name = DomHelper.getNodeValue(node, "name");
	}
	/**
	 * xmlの内容を解析する。
	 * @param node
	 */
	private void analize(Node node) {
		String nodeName = node.getNodeName();
		if(nodeName.equalsIgnoreCase("audio")) {
			logger.info("  audio");
			// 各audio要素があるか確認して、あれば、設定する。
			setHasAudio(true);
			setupAudio(node);
		}
		else if(nodeName.equalsIgnoreCase("video")) {
			logger.info("  video");
			setHasVideo(true);
			setupVideo(node);
		}
		// 解析不可
	}
	/**
	 * 映像の設定データを解析します。
	 * @param node
	 */
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
	/**
	 * 映像解析の内部処理
	 * @param name
	 * @param value
	 * @param node
	 */
	private void setupVideoElement(String name, String value, Node node) {
		if(name == null) {
			return;
		}
		if("properties".equalsIgnoreCase(name)) {
//			logger.info("properties");
			// nodeのさらに子要素について調査する必要あり
			setupProperties(node);
//			logger.info("{}", videoProperties);
		}
		if("flags".equalsIgnoreCase(name)) {
//			logger.info("flags");
			// nodeのさらに子要素について調査する必要あり
			setupFlags(node);
//			logger.info("{}", videoFlags);
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
		if("framerate".equalsIgnoreCase(name)) {
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
			// refの場合は、そのデータがついている場合はnodeのデータとして処理させる。
			ReferenceManager refManager = ReferenceManager.getReferenceData(value);
			if(refManager == null) {
				return;
			}
			setupVideo(refManager.getData());
			return;
		}
	}
	/**
	 * 音声の設定データを解析します。
	 * @param node
	 */
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
	/**
	 * 音声解析の内部処理
	 * @param name
	 * @param value
	 * @param node
	 */
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
			// refの場合は、そのデータがついている場合はnodeのデータとして処理させる。
			ReferenceManager refManager = ReferenceManager.getReferenceData(value);
			if(refManager == null) {
				return;
			}
			setupAudio(refManager.getData());
			return;
		}
	}
	/**
	 * 映像propertiesの内容解析処理
	 * @param node
	 */
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
	/**
	 * 映像flagsの内容解析処理
	 * @param node
	 */
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
	/**
	 * コンバート動作のセットアップを実行する。
	 */
	public abstract boolean setup();
	/**
	 * コンテナを開き直す動作
	 * @param audioFlg 音声があるかどうか
	 * @param videoFlg 映像があるかどうか
	 * @return
	 */
	public abstract boolean resetupContainer();
	/**
	 * コンテナの再生成動作のベース部
	 * @param url
	 * @param ext
	 * @return
	 */
	protected boolean resetupContainer(String url, String ext) {
		int retval = -1;
		IContainer container = IContainer.make();
		ISimpleMediaFile outputInfo = getStreamInfo();
		outputInfo.setURL(url);
		IContainerFormat outputFormat = IContainerFormat.make();
		outputFormat.setOutputFormat(ext, url, null);
		retval = container.open(url, IContainer.Type.WRITE, outputFormat);
		if(retval < 0) {
			throw new RuntimeException("出力用のURLを開くことができませんでした。:" + url);
		}
		// containerを登録しておく。
		this.container = container;
		return false;
	}
	/**
	 * メディアデータのヘッダ情報を書き込みます。
	 */
	public void writeHeader() {
		int retval = -1;
		retval = this.container.writeHeader();
		if(retval < 0) {
			throw new RuntimeException("コンテナのheaderへの書き込みに失敗しました。");
		}
	}
	/**
	 * メディアデータのフッター情報を書き込みます。
	 */
	public void writeTailer() {
		int retval = -1;
		if(this.container != null) {
			retval = this.container.writeTrailer();
			if(retval < 0) {
				throw new RuntimeException("コンテナのtailerへの書き込みに失敗しました。");
			}
		}
	}
	/**
	 * streamInfoを参照します。
	 * @return
	 */
	public ISimpleMediaFile getStreamInfo() {
		return streamInfo;
	}
	/**
	 * 動画プロパティー値を参照します。
	 * @return
	 */
	public Map<String, String> getVideoProperty() {
		return videoProperties;
	}
	/**
	 * 動画フラッグ値を参照します。
	 * @return
	 */
	public Map<IStreamCoder.Flags, Boolean> getVideoFlags() {
		return videoFlags;
	}
	/**
	 * このストリームにつけられた固有の名前
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * コンテナを閉じる
	 */
	public void close() {
		if(container != null) {
			container.close();
			container = null;
		}
	}
}
