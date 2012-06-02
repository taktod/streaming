package com.ttProject.streaming;

/**
 * AppleのHttpLiveStreaming用のsegmentを作成します。
 * segmentを書き込むと同時にm3u8の定義ファイルもかかないとだめ
 * @author taktod
 */
public class TsSegmentCreator {
	/** 各segmenterのduration指定 */
	private static int duration;
	/** 出力ファイルの定義 */
	private static int path;
	public void setDuration(int duration) {
		TsSegmentCreator.duration = duration;
	}
	/**
	 * mpegtsのセグメントをファイルに書き込む
	 * @param buf
	 * @param size
	 * @param timestamp
	 */
	public void writeSegment(byte[] buf, int size, long timestamp) {
		
	}
}
