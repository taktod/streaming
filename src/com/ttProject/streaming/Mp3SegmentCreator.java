package com.ttProject.streaming;

import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.xuggle.out.mpegts.MpegtsOutputManager;

/**
 * 自作のjpegmp3ストリーム用のmp3のsegmentを作成します。
 * segmentを書き込むと同時にm3u8の定義ファイルもかかないとだめ、このあたりの動作はtsSegmentCreatorとほぼ同じ
 * @author taktod
 */
public class Mp3SegmentCreator {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(Mp3SegmentCreator.class);
	/** duration */
	private static int duration;
	/** 一時出力パス */
	private static String tmpPath;
	/**
	 * durationを設定する。
	 * @param duration
	 */
	public void setDuration(int duration) {
		Mp3SegmentCreator.duration = duration;
	}
	/**
	 * 一時ファイル置き場
	 * @param tmpPath
	 */
	public void setTmpPath(String tmpPath) {
		if(tmpPath.endsWith("/")) {
			Mp3SegmentCreator.tmpPath = tmpPath;
		}
		else {
			Mp3SegmentCreator.tmpPath = tmpPath + "/";
		}
	}
	/** 対象名 */
	private String tmpTarget;
	/**
	 * 初期化
	 */
	public void initialize(String name, MpegtsOutputManager outputManager) {
		this.tmpTarget = tmpPath + name + "/";
		if(!(new File(tmpTarget)).mkdirs()) {
			throw new RuntimeException("mpegts用の一時ディレクトリの作成に失敗");
		}
		// カウンターをリセットしておく。
		counter = 0;
		nextStartPos = duration;
		// outputStreamの準備をしておく。
		try {
			outputStream = new FileOutputStream(tmpTarget + counter + ".mp3");
			counter ++;
		}
		catch (Exception e) {
		}
	}
	private FileOutputStream outputStream; // 出力ファイル
	private int counter; // segmentのカウンター
	private long nextStartPos; // 次のセグメントの開始位置
	// タイムスタンプは設定されているデータから、自分で計算する必要あり(mp3の生データを扱うため。)
	// 64kb/s 2ch 44100 Hz
	private static final byte[] noSoundMp3 = {
		(byte)0xff, (byte)0xfb, (byte)0x52, (byte)0x64, (byte)0xa9, (byte)0x0f, (byte)0xf0, (byte)0x00, (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x0d, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0xa4, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x34, (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x4c, (byte)0x41, (byte)0x4d, (byte)0x45, (byte)0x33, (byte)0x2e, (byte)0x39, (byte)0x38, (byte)0x2e, (byte)0x34, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55};
	/**
	 * 
	 * @param buf
	 * @param size
	 * @param position
	 */
	public void writeSegment(byte[] buf, int size, long position) {
		try {
			// とりあえず書き込んでファイルをつくる。
			outputStream.write(buf);
		}
		catch (Exception e) {
		}
	}
	/**
	 * 
	 * @param position
	 */
	public void updateSegment(long position) {
		
	}
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
