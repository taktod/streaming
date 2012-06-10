package com.ttProject.streaming;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AppleのHttpLiveStreaming用のsegmentを作成します。
 * segmentを書き込むと同時にm3u8の定義ファイルもかかないとだめ
 * とりあえず、segmentは1.ts 2.tsみたいにして、m3u8はdata.m3u8という形固定
 * 出力パスはtmpPath/名前に固定しておきたい。
 * @author taktod
 */
public class TsSegmentCreator extends SegmentCreator{
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(TsSegmentCreator.class);
	/** 動作対象拡張子 */
	private String ext = ".ts";
	/** 出力ファイルストリーム */
	private FileOutputStream outputStream;
	/** segmentのカウンター */
	private int counter;
	/** 次のセグメントの開始位置(タイムスタンプ) */
	private long nextStartPos;
	/** 動作duration */
	private static int duration;
	/** 一時データ保持場所 */
	private static String tmpPath;
	/**
	 * duration設定
	 */
	@Override
	public void setDuration(int value) {
		duration = value;
	}
	/**
	 * duration取得
	 */
	@Override
	protected int getDuration() {
		return duration;
	}
	/**
	 * 一時ファイル置き場設定
	 */
	@Override
	public void setTmpPath(String path) {
		if(path.endsWith("/")) {
			tmpPath = path;
		}
		else {
			tmpPath = path + "/";
		}
	}
	/**
	 * 一時ファイル置き場取得
	 */
	@Override
	protected String getTmpPath() {
		return tmpPath;
	}
	/**
	 * 拡張子の変更
	 * @param ext
	 */
	protected void setExt(String ext) {
		this.ext = ext;
	}
	/**
	 * 初期化
	 */
	public void initialize(String name) {
		setName(name);
		prepareTmpPath();
		reset();
	}
	/**
	 * 始めからやりなおします。
	 */
	public void reset() {
		close();
		// カウンターをリセットしておく。
		counter = 0;
		nextStartPos = getDuration();
		// outputStreamの準備をしておく。
		try {
			outputStream = new FileOutputStream(getTmpTarget() + counter + ext);
		}
		catch (Exception e) {
			logger.error("出力ストリームを開くのに失敗しました。", e);
		}
	}
	/**
	 * mpegtsのセグメントをファイルに書き込む
	 * @param buf
	 * @param size
	 * @param timestamp
	 */
	public void writeSegment(byte[] buf, int size, long timestamp, boolean isKey) {
		if(outputStream != null) {
			try {
				// タイムスタンプの確認と、バッファがキーであるか確認。
				if(timestamp > nextStartPos && isKey) {
					// 以前のファイル出力を停止する。
					outputStream.close();
					// 出力用のm3u8ファイルの準備
					// TODO hoge.m3u8固定になっているので、名前を変更しておきたい。
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(getTmpTarget() + "hoge.m3u8")));
					pw.println("#EXTM3U");
					pw.println("#EXT-X-ALLOW-CACHE:NO");
					pw.print("#EXT-X-TARGETDURATION:");
					pw.println((int)(getDuration() / 1000));
					if(counter - 2 >= 0) {
						pw.print("#EXT-X-MEDIA-SEQUENCE:");
						pw.println(counter - 2);
						pw.print("#EXTINF:");
						pw.println((int)(getDuration() / 1000));
						pw.print(counter - 2);
						pw.println(ext);
						pw.print("#EXTINF:");
						pw.println((int)(getDuration() / 1000));
						pw.print(counter - 1);
						pw.println(ext);
					}
					else if(counter - 1 >= 0) { 
						pw.print("#EXT-X-MEDIA-SEQUENCE:");
						pw.println(counter - 1);
						pw.print("#EXTINF:");
						pw.println((int)(getDuration() / 1000));
						pw.print(counter - 1);
						pw.println(ext);
					}
					else {
						pw.println("#EXT-X-MEDIA-SEQUENCE:0");
					}
					pw.print("#EXTINF:");
					pw.println((int)(getDuration() / 1000));
					pw.print(counter);
					pw.println(ext);
					pw.close();
					pw = null;
					// 次の切断場所を定義
					nextStartPos = timestamp + getDuration();
					// カウンターのインクリメント
					counter ++;
					// データ出力先のストリームを開いておく。
					outputStream = new FileOutputStream(getTmpTarget() + counter + ext);
				}
				// データの追記
				outputStream.write(buf);
			}
			catch (Exception e) {
				logger.error("書き込みに失敗しました。", e);
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
				logger.error("出力ストリームを停止したところエラーが発生しました。", e);
			}
			outputStream = null;
		}
	}
}
