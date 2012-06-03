package com.ttProject.streaming;

import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AppleのHttpLiveStreaming用のsegmentを作成します。
 * segmentを書き込むと同時にm3u8の定義ファイルもかかないとだめ
 * とりあえず、segmentは1.ts 2.tsみたいにして、m3u8はdata.m3u8という形固定
 * 出力パスはtmpPath/名前に固定しておきたい。
 * @author taktod
 */
public class TsSegmentCreator {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(TsSegmentCreator.class);
	/** duration指定(ミリ秒) */
	private static int duration;
	/** 一時出力パス */
	private static String tmpPath;
	/**
	 * durationを設定する。
	 * @param duration
	 */
	public void setDuration(int duration) {
		TsSegmentCreator.duration = duration;
	}
	/**
	 * 一時ファイル置き場
	 * @param tmpPath
	 */
	public void setTmpPath(String tmpPath) {
		if(tmpPath.endsWith("/")) {
			TsSegmentCreator.tmpPath = tmpPath;
		}
		else {
			TsSegmentCreator.tmpPath = tmpPath + "/";
		}
	}
	/** 対象名 */
	private String tmpTarget;
	/**
	 * 初期化
	 */
	public void initialize(String name) {
		this.tmpTarget = tmpPath + name + "/";
		if(!(new File(tmpTarget)).mkdirs()) {
			throw new RuntimeException("mpegts用の一時ディレクトリの作成に失敗");
		}
		// カウンターをリセットしておく。
		counter = 0;
		nextStartPos = duration;
		// outputStreamの準備をしておく。
		try {
			outputStream = new FileOutputStream(tmpTarget + counter + ".ts");
			counter ++;
		}
		catch (Exception e) {
		}
	}
	private FileOutputStream outputStream; // 出力ファイル
	private int counter; // segmentのカウンター
	private long nextStartPos; // 次のセグメントの開始位置
	/**
	 * mpegtsのセグメントをファイルに書き込む
	 * @param buf
	 * @param size
	 * @param timestamp
	 */
	public void writeSegment(byte[] buf, int size, long timestamp) {
		// ここでは特に考えもなく、bufデータが送られてきてしまうので、それを追記しまくっていく。よってclose処理が必要。
		if(outputStream != null) {
			try {
				if(timestamp > nextStartPos) {
					logger.info("timestamp {}, nextPos {}", new Object[]{
							timestamp,
							nextStartPos
					});
					// タイムスタンプが次の開始位置以降の場合
					outputStream.close();
					outputStream = new FileOutputStream(tmpTarget + counter + ".ts");
					nextStartPos += duration;
					counter ++;
				}
				outputStream.write(buf);
			}
			catch (Exception e) {
			}
		}
	}
	/**
	 * 閉じる
	 */
	public void close() {
		if(outputStream != null) {
			try {
				outputStream.close();
			}
			catch (Exception e) {
			}
			outputStream = null;
		}
	}
}
