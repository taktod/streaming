package com.ttProject.streaming;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * httpTakStreamingで利用するfth ftmデータを作成するクラス
 * @author taktod
 */
public class TakSegmentCreator extends SegmentCreator{
	/** ロガー */
	private static final Logger logger = LoggerFactory.getLogger(TakSegmentCreator.class);
	/** 各segmentのdurationの値 */
	private static int duration;
	/** 一時出力する場所 */
	private static String tmpPath;
	/** 出力ファイルのインクリメント番号 */
	private int counter = 0;
	/** firstTimestamp */
	private int firstTimestamp;
	/** 出力ファイルストリーム */
	private FileOutputStream ftmFile = null;
	// こっちでpacketデータを保持しないのは、単にByteBufferのみおくってくるので、audioかvideoか判定しにくい。
	// 可能だが、Red5もしくはFlazr依存動作になってしまう。
	/**
	 * segmentの長さの定義
	 */
	@Override
	public void setDuration(int value) {
		duration = value;
	}
	/**
	 * segmentの長さ参照
	 */
	@Override
	protected int getDuration() {
		return duration;
	}
	/**
	 * 一時出力場所を定義
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
	 * 一時定義場所を参照
	 */
	@Override
	protected String getTmpPath() {
		return tmpPath;
	}
	/**
	 * 初期化
	 * @param name
	 */
	public void initialize(String name) {
		setName(name);
		prepareTmpPath();
	}
	public void reset() {
		close();
		counter = 0; // 0に戻す。
		firstTimestamp = -1; // タイムスタンプを未設置にしておく。
		try {
			ftmFile = new FileOutputStream(getTmpTarget() + counter + ".ftm");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * fthファイルを作成する。
	 * @param buf 追加するbyteデータ
	 * @param video 映像のfirstパケットデータ (AVC用:他のコーデックでは必要ないが、送ります。)
	 * @param audio 音声のfirstパケットデータ (AAC用:他のコーデックでは必要ないが、送ります。)
	 */
	public void writeHeaderPacket(ByteBuffer buf, ByteBuffer video, ByteBuffer audio) {
		try {
			FileOutputStream fthFile = new FileOutputStream(getTmpTarget() + "index.fth");
			byte[] flvHeader = new byte[buf.limit()];
			buf.get(flvHeader);
			fthFile.write(flvHeader);
			// videoデータがある場合は書き込む
			if(video != null && video.limit() >= 11) {
				byte[] firstVideo = new byte[video.limit()];
				video.get(firstVideo);
				fthFile.write(firstVideo);
			}
			if(audio != null && audio.limit() >= 11) {
				byte[] firstAudio = new byte[audio.limit()];
				audio.get(firstAudio);
				fthFile.write(firstAudio);
			}
			if(fthFile != null) {
				fthFile.flush();
				fthFile.close();
				fthFile = null;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * ftmファイルを作成する。
	 * @param buf ヘッダ情報となるbyteデータ
	 */
	public void writeTagData(ByteBuffer buf, int timestamp) {
		// ftmファイルを作成する。入力データはflv形式のおのおののパケット
		if(firstTimestamp == -1) {
			firstTimestamp = timestamp;
		}
		int passed = timestamp - firstTimestamp;
		
	}
	/**
	 * ストリームが止まったときの動作
	 */
	@Override
	public void close() {
		if(ftmFile != null) {
			try {
				ftmFile.flush();
				ftmFile.close();
				ftmFile = null;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
