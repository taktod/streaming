package com.ttProject.streaming;

import java.io.File;

/**
 * セグメントの作成クラスはにているところが結構あるので、abstractクラスにしてみました。
 * @author taktod
 */
public abstract class SegmentCreator {
	/** 名前データ */
	private String name;
	/** 保存対象ディレクトリ */
	private String tmpTarget;
	/**
	 * durationの設定
	 * @param duration
	 */
	public abstract void setDuration(int duration);
	/**
	 * duraitonの参照
	 * @return
	 */
	protected abstract int getDuration();
	/**
	 * 一時パスの設定
	 * @param path
	 */
	public abstract void setTmpPath(String path);
	/**
	 * 一時パスの設定
	 * @return
	 */
	protected abstract String getTmpPath();
	/**
	 * 名前を保存
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
		this.tmpTarget = getTmpPath() + name + "/";
	}
	/**
	 * 保存ディレクトリの取得
	 * @return
	 */
	protected String getTmpTarget() {
		return tmpTarget;
	}
	/**
	 * 対象名を取得
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * 初期準備を実行しておく。
	 */
	public void prepareTmpPath() {
		File targetDirectory = new File(tmpTarget);
		if(!targetDirectory.mkdirs()) {
			// すでにディレクトリが存在している可能性があるので、空にしておきたい。
			for(String name : targetDirectory.list()) {
				File file = new File(tmpTarget + name);
				// とりあえずfileのみ削除しておく。
				if(file.isFile()) {
					file.delete();
				}
			}
		}
	}
	/**
	 * 停止動作
	 */
	public abstract void close();
}
