package com.ttProject.xuggle;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.streaming.MediaManager;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.IPixelFormat.Type;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;

/**
 * コンバートの動作を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここではコーデック変換を担当
 */
public class VideoEncodeManager {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(VideoEncodeManager.class);
	/** 映像コーダー */
	private IStreamCoder videoCoder = null;
	/** 処理対象コンテナ */
	private Set<IContainer> containers = new HashSet<IContainer>();

	private ICodec.ID codec;
	private int width = -1;
	private int height = -1;
	private Type pixelFormat;
	private int globalQuality;
	private int bitRate;
	private IRational frameRate;
	private int groupsOfPictures;
	private Map<String, String> properties;
	private Map<IStreamCoder.Flags, Boolean> flags;
	private IRational timeBase;
	/**
	 * コンストラクタ
	 * @param mediaManager
	 */
	public VideoEncodeManager(MediaManager mediaManager) {
		ISimpleMediaFile streamInfo = mediaManager.getStreamInfo();
		addContainer(mediaManager.getContainer());
		setCodec(streamInfo.getVideoCodec());
		setHeight(streamInfo.getVideoHeight());
		setWidth(streamInfo.getVideoWidth());
		setPixelFormat(streamInfo.getVideoPixelFormat());
		setGlobalQuality(streamInfo.getVideoGlobalQuality());
		setBitRate(streamInfo.getVideoBitRate());
		setFrameRate(streamInfo.getVideoFrameRate());
		setGroupsOfPictures(streamInfo.getVideoNumPicturesInGroupOfPictures());
		setProperties(mediaManager.getVideoProperty());
		setFlags(mediaManager.getVideoFlags());
		setTimeBase(streamInfo.getVideoTimeBase());
		logger.info(width + ":" + height);
	}
	/**
	 * 対象のメディアマネージャーが合致する場合は登録する。
	 * @param mediaManager 登録するmediaManager
	 * @return true:合致した場合 false:合致しない場合
	 */
	public boolean addMediaManager(MediaManager mediaManager) {
		ISimpleMediaFile streamInfo = mediaManager.getStreamInfo();
		logger.info("a:{}", streamInfo.getVideoFrameRate());
		logger.info("b:{}", getFrameRate());
		// データが一致するか確認する。
		if(streamInfo.getVideoCodec().equals(getCodec())
		&& streamInfo.getVideoHeight() == getHeight()
		&& streamInfo.getVideoWidth() == getWidth()
		&& streamInfo.getVideoPixelFormat().equals(getPixelFormat())
		&& streamInfo.getVideoGlobalQuality() == getGlobalQuality()
		&& streamInfo.getVideoBitRate() == getBitRate()
		&& streamInfo.getVideoFrameRate().equals(getFrameRate())
		&& streamInfo.getVideoNumPicturesInGroupOfPictures() == getGroupsOfPictures()
		&& mediaManager.getVideoProperty().equals(getProperties())
		&& mediaManager.getVideoFlags().equals(getFlags())
		&& streamInfo.getVideoTimeBase().equals(getTimeBase())) {
			// 一致した。
			addContainer(mediaManager.getContainer());
			return true;
		}
		// 一致しない。
		return false;
	}
	/**
	 * 登録されている情報でcoderを作成する。
	 */
	public void setupCoder() {
		logger.info("コーダーをセットアップします。" + width + ":" + height);
		ICodec outCodec = ICodec.findEncodingCodec(getCodec());
		if(outCodec == null) {
			throw new RuntimeException("video出力用のコーデックを取得することができませんでした。");
		}
		for(IContainer container : getContainers()) {
			IStream outStream = null;
			logger.info("container" + container.toString());
			if(videoCoder != null) {
				logger.info("コーダーから作成する。");
				outStream = container.addNewStream(videoCoder);
			}
			else {
				logger.info("ICodecから生成する。");
				outStream = container.addNewStream(outCodec);
			}
			if(outStream == null) {
				throw new RuntimeException("コンテナ用のストリーム作成失敗");
			}
			if(videoCoder == null) {
				IStreamCoder outCoder = outStream.getStreamCoder();
				outCoder.setCodec(getCodec());
				outCoder.setWidth(getWidth());
				outCoder.setHeight(getHeight());
				outCoder.setPixelType(getPixelFormat());
				outCoder.setGlobalQuality(getGlobalQuality());
				outCoder.setBitRate(getBitRate());
				outCoder.setFrameRate(getFrameRate());
				outCoder.setNumPicturesInGroupOfPictures(getGroupsOfPictures());
				for(String key : getProperties().keySet()) {
					outCoder.setProperty(key, getProperties().get(key));
				}
				for(IStreamCoder.Flags flag : getFlags().keySet()) {
					outCoder.setFlag(flag, getFlags().get(flag));
				}
				outCoder.setTimeBase(getTimeBase());
				outCoder.open(null, null);
				videoCoder = outCoder;
			}
		}
	}
	public void encodeVideo(IVideoPicture picture) {
		logger.info("映像のエンコードを実行します。");
		int retval = -1;
		IPacket outPacket = IPacket.make();
		
		int numBytesConsumed = 0;
		if(picture.isComplete()) {
			retval = videoCoder.encodeVideo(outPacket, picture, 0);
			if(retval <= 0) {
				logger.info("videoコーダーの実行したところ、0以下が応答かえってきました。:" + retval);
//				logger.info(IError.make(retval).toString());
				return;
			}
			numBytesConsumed += retval;
			if(outPacket.isComplete()) {
				logger.info("パケットができあがりました。");
				for(IContainer container : getContainers()) {
					logger.info("コンテナにパケットを書き込みます。" + container.toString());
					container.writePacket(outPacket);
				}
			}
			else {
				logger.info("packetができあがってません。");
			}
		}
	}
	/**
	 * コンテナを追加する。
	 * @param container
	 */
	public void addContainer(IContainer container) {
		if(container == null) {
			logger.info("コンテナを追加します。nullだった・・");
		}
		else {
			logger.info("コンテナを追加します。" + container.toString());
		}
		containers.add(container);
	}
	public IStreamCoder getVideoCoder() {
		return videoCoder;
	}
	private Set<IContainer> getContainers() {
		return containers;
	}
	private ICodec.ID getCodec() {
		return codec;
	}
	private void setCodec(ICodec.ID codec) {
		this.codec = codec;
	}
	private int getWidth() {
		return width;
	}
	private void setWidth(int width) {
		this.width = width;
	}
	private int getHeight() {
		return height;
	}
	private void setHeight(int height) {
		this.height = height;
	}
	private Type getPixelFormat() {
		return pixelFormat;
	}
	private void setPixelFormat(Type pixelFormat) {
		this.pixelFormat = pixelFormat;
	}
	private int getGlobalQuality() {
		return globalQuality;
	}
	private void setGlobalQuality(int globalQuality) {
		this.globalQuality = globalQuality;
	}
	private int getBitRate() {
		return bitRate;
	}
	private void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}
	private IRational getFrameRate() {
		return frameRate;
	}
	private void setFrameRate(IRational frameRate) {
		this.frameRate = frameRate;
	}
	private int getGroupsOfPictures() {
		return groupsOfPictures;
	}
	private void setGroupsOfPictures(int groupsOfPictures) {
		this.groupsOfPictures = groupsOfPictures;
	}
	private Map<String, String> getProperties() {
		return properties;
	}
	private void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	private Map<IStreamCoder.Flags, Boolean> getFlags() {
		return flags;
	}
	private void setFlags(Map<IStreamCoder.Flags, Boolean> flags) {
		this.flags = flags;
	}
	private IRational getTimeBase() {
		return timeBase;
	}
	private void setTimeBase(IRational timeBase) {
		this.timeBase = timeBase;
	}
}
