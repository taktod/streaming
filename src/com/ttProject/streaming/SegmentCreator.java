package com.ttProject.streaming;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * セグメントの作成クラスはにているところが結構あるので、abstractクラスにしてみました。
 * durationやtmpPathの定義をここでstaticの形で実行してしまうと、各クラスことにわけることができなかった
 * abstractとし、各クラスで処理を各ことにしました。ちょっと不明なコードになったか？
 * @author taktod
 */
public abstract class SegmentCreator {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(SegmentCreator.class);
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
	 * ファイル書き込み後実行するコマンド
	 */
	public abstract void setCommand(String command);
	/**
	 * ファイル書き込み後実行するコマンド参照
	 * @return
	 */
	protected abstract String getCommand();
	/**
	 * 登録されているコマンドを実行します。
	 * @param file
	 */
	protected void doComment(String file) {
		try {
			if(file == null) {
				return;
			}
			// commandにfileをつけて実行する。
			ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", getCommand() + " " + file + " >/dev/null 2>&1 &");
			// 実行する。
			processBuilder.start();
		}
		catch (Exception e) {
			logger.error("commandError", e);
		}
	}
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
