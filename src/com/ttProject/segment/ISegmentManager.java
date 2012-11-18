package com.ttProject.segment;

/**
 * segment分割のindexデータ用のmanagerのインターフェイス
 * flv用のやつもつくっておきたいところ。ftfだったっけ？
 * @author todatakahiko
 *
 */
public interface ISegmentManager {
	/**
	 * データの書き込み
	 * @param target
	 * @param http
	 * @param duration
	 * @param index
	 * @param endFlg
	 */
	public void writeData(String target, String http, int duration, int index, boolean endFlg);
}
