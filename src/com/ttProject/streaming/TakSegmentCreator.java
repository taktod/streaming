package com.ttProject.streaming;

import java.nio.ByteBuffer;

/**
 * httpTakStreamingで利用するfth ftmデータを作成するクラス
 * @author taktod
 */
public class TakSegmentCreator extends SegmentCreator{
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
	 * fthファイルを作成する。
	 * @param buf 追加するbyteデータ
	 * @param video 映像のfirstパケットデータ (AVC用:他のコーデックでは必要ないが、送ります。)
	 * @param audio 音声のfirstパケットデータ (AAC用:他のコーデックでは必要ないが、送ります。)
	 */
	public void writeHeaderPacket(ByteBuffer buf, ByteBuffer video, ByteBuffer audio) {
	}
	/**
	 * ftmファイルを作成する。
	 * @param buf ヘッダ情報となるbyteデータ
	 */
	public void writeTagData(ByteBuffer buf) {
		// ftmファイルを作成する。入力データはflv形式のおのおののパケット
	}
	/**
	 * ストリームが止まったときの動作
	 */
	@Override
	public void close() {
	}
}
