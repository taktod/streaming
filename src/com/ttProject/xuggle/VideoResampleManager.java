package com.ttProject.xuggle;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;

/**
 * コンバートの動作を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここではリサンプルの部分を担当
 * pixelType width heightをみて違う場合は変換する必要あり。
 * リサンプラーの作成には、変換対象と変換元の両方のデータが必要。
 */
public class VideoResampleManager {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(VideoResampleManager.class);
	private IVideoResampler resampler = null;
	private Set<VideoEncodeManager> encodeManagers = new HashSet<VideoEncodeManager>();
	// 変換目標
	private IPixelFormat.Type pixelType;
	private int width;
	private int height;
	public VideoResampleManager(VideoEncodeManager encodeManager) {
		// encodeManagerから必要なリサンプルマネージャーを作成する。
		IStreamCoder videoCoder = encodeManager.getVideoCoder();
		encodeManagers.add(encodeManager);
		setPixelType(videoCoder.getPixelType());
		setWidth(videoCoder.getWidth());
		setHeight(videoCoder.getHeight());
		logger.info(width + ":" + height);
	}
	public boolean addEncodeManager(VideoEncodeManager encodeManager) {
		IStreamCoder videoCoder = encodeManager.getVideoCoder();
		if(videoCoder.getPixelType().equals(getPixelType())
		&& videoCoder.getWidth() == getWidth()
		&& videoCoder.getHeight() == getHeight()) {
			encodeManagers.add(encodeManager);
			return true;
		}
		return false;
	}
	/**
	 * 画像をリサンプルします。
	 * @param picture
	 * @return
	 */
	public IVideoPicture resampleVideo(IVideoPicture picture) {
		checkResampler(picture);
		IVideoPicture result = IVideoPicture.make(getPixelType(), getWidth(), getHeight());
		int retval = -1;
		retval = resampler.resample(result, picture);
		if(retval < 0) {
			throw new RuntimeException("videoのリサンプルに失敗しました。");
		}
		return result;
	}
	/**
	 * 入力画像データの確認
	 * @param picture
	 */
	private void checkResampler(IVideoPicture picture) {
		// 入力データの確認
		if(resampler == null
		|| picture.getPixelType() != resampler.getInputPixelFormat()
		|| picture.getWidth()     != resampler.getInputWidth()
		|| picture.getHeight()    != resampler.getInputHeight()) {
			// どれか１つの条件に当てはまった場合はresamplerを作り直す必要がある。
			setupResampler(picture);
		}
	}
	/**
	 * リサンプラーの再構成
	 * @param picture
	 */
	private void setupResampler(IVideoPicture picture) {
		// 多分閉じなくても自動的にしまると思う。メモリーリークがおきるようだったら、コメントを解除する感じ
//		if(resampler != null) {
//			resampler.delete();
//		}
		resampler = IVideoResampler.make(getWidth(), getHeight(), getPixelType(), picture.getWidth(), picture.getHeight(), picture.getPixelType());
		if(resampler == null) {
			throw new RuntimeException("videoリサンプラーの作成に失敗しました。");
		}
	}
	public Set<VideoEncodeManager> getEncodeManagers() {
		return encodeManagers;
	}
	private IPixelFormat.Type getPixelType() {
		return pixelType;
	}
	private void setPixelType(IPixelFormat.Type pixelType) {
		this.pixelType = pixelType;
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
}
