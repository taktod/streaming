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
	private FileOutputStream outputStream; // 出力ファイル
	private int counter; // segmentのカウンター
	private long nextStartPos; // 次のセグメントの開始位置
	private static int duration;
	private static String tmpPath;
	@Override
	public void setDuration(int value) {
		duration = value;
	}
	@Override
	protected int getDuration() {
		return duration;
	}
	@Override
	public void setTmpPath(String path) {
		if(path.endsWith("/")) {
			tmpPath = path;
		}
		else {
			tmpPath = path + "/";
		}
	}
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
		}
	}
	/**
	 * mpegtsのセグメントをファイルに書き込む
	 * @param buf
	 * @param size
	 * @param timestamp
	 */
	public void writeSegment(byte[] buf, int size, long timestamp, boolean isKey) {
		// ここでは特に考えもなく、bufデータが送られてきてしまうので、それを追記しまくっていく。よってclose処理が必要。
		if(outputStream != null) {
			try {
				if(timestamp > nextStartPos && isKey) {
/*					logger.info("timestamp {}, nextPos {}", new Object[]{
							timestamp,
							nextStartPos
					});*/
					// タイムスタンプが次の開始位置以降の場合
					outputStream.close();
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
					nextStartPos = timestamp + getDuration();
					counter ++;
					outputStream = new FileOutputStream(getTmpTarget() + counter + ext);
				}
				outputStream.write(buf);
			}
			catch (Exception e) {
				// もしかして、書き込みミスが発生している？
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
