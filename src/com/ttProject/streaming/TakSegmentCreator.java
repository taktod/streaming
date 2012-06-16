package com.ttProject.streaming;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * httpTakStreamingで利用するfth ftmデータを作成するクラス
 * audio初期パケットとvideo初期パケットはこちらで判定すべきかと思うが、flazrとred5どちらでも依存動作になってしまうため、このクラスでは実行しないことにしました。
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
	/** segmentの次の切れ目 */
	private int nextStartPos;
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
		reset();
	}
	/**
	 * リセット動作
	 */
	private void reset() {
		close();
		counter = 0; // 0に戻す。
		firstTimestamp = -1; // タイムスタンプを未設置にしておく。
		nextStartPos = getDuration();
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
			// TODO index.fth固定にしてあります。
			FileOutputStream fthFile = new FileOutputStream(getTmpTarget() + "index.fth");
			byte[] flvHeader = new byte[buf.limit()];
			buf.get(flvHeader);
			fthFile.write(flvHeader);
			// videoデータがある場合は書き込む
			ByteBuffer buffer = null;
			if(video != null) {
				buffer = video.duplicate();
				if(buffer.limit() >= 11) {
					byte[] firstVideo = new byte[buffer.limit()];
					buffer.get(firstVideo);
					fthFile.write(firstVideo);
				}
			}
			if(audio != null) {
				buffer = audio.duplicate();
				if(buffer.limit() >= 11) {
					byte[] firstAudio = new byte[buffer.limit()];
					buffer.get(firstAudio);
					fthFile.write(firstAudio);
				}
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
	public synchronized void writeTagData(ByteBuffer buf, int timestamp, boolean isKey) {
		// ftmファイルを作成する。入力データはflv形式のおのおののパケット
		if(firstTimestamp == -1) {
			firstTimestamp = timestamp;
		}
		int passed = timestamp - firstTimestamp;
		try {
			if(passed > nextStartPos && isKey) {
				// 次のファイルへの切り替えがきた場合切り替える。
				ftmFile.close();
				// TODO index.ftl固定にしてあります。
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(getTmpTarget() + "index.ftl")));
				pw.println("#FTH:index.fth");
				// TODO 動作をよりリアルタイムにするために、載せるセグメント数を2つにした。
/*				if(counter - 2 >= 0) {
					pw.print("#FTM-X-MEDIA-SEQUENCE:");
					pw.println(counter - 2);

					pw.print("#FTMINF:");
					pw.print(counter - 2);
					pw.println(".ftm");
					
					pw.print("#FTMINF:");
					pw.print(counter - 1);
					pw.println(".ftm");
				}
				else*/ if(counter - 1 >= 0) {
					pw.print("#FTM-X-MEDIA-SEQUENCE:");
					pw.println(counter - 1);
					
					pw.print("#FTMINF:");
					pw.print(counter - 1);
					pw.println(".ftm");
				}
				else {
					pw.println("#FTM-X-MEDIA-SEQUENCE:0");
				}
				pw.print("#FTMINF:");
				pw.print(counter);
				pw.println(".ftm");

				pw.close();
				pw = null;
				nextStartPos = passed + getDuration();
				counter ++;
				ftmFile = new FileOutputStream(getTmpTarget() + counter + ".ftm");
			}
			buf.position(4);
			buf.put((byte)(0xFF & (passed >> 16)));
			buf.put((byte)(0xFF & (passed >> 8)));
			buf.put((byte)(0xFF & passed));
			buf.put((byte)(0xFF & (passed >> 24)));
			buf.rewind();
			byte[] data = new byte[buf.limit()];
			buf.get(data);
			ftmFile.write(data);
		}
		catch (Exception e) {
			logger.error("書き込み失敗", e);
		}
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
