package com.ttProject.streaming;

import com.xuggle.xuggler.IVideoPicture;

/**
 * creatorはすべて適当なディレクトリにいったんデータを書き出して、それを全体で共有する形ですすめる。
 * @author taktod
 */
public class JpegSegmentCreator {
	/** 仮のパス */
	private String tmpPath;
	/** 縦横サイズを固定するか(falseならwidthとheightは無視) */
	private boolean fixSize;
	/** 横幅 */
	private int width;
	/** 縦幅 */
	private int height;
	/** １枚の画像に複数データを載せてダウンロードコストを落とすかどうか？ */
	private boolean fourPicBoost;
	public void setTmpPath(String tmpPath) {
		this.tmpPath = tmpPath;
	}
	public void setFixSize(boolean fixSize) {
		this.fixSize = fixSize;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	/**
	 * 現在処理中の位置を保持しておく。(この位置がおかしいんだよな)
	 * @param picture
	 * @param position
	 */
	public void makeFramePicture(IVideoPicture picture, long timestamp) {
		
	}
	public void close() {
		
	}
}
