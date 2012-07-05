package com.ttProject.streaming;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.ICodec.ID;

/**
 * 自作のjpegmp3ストリーム用のmp3のsegmentを作成します。
 * 中途のみの抜き出し方式でつくるとaudio.currentTimeのjavascriptでとれる値が再生してからの時刻になります。
 * 全部のパケットデータを書き出して実行した場合は、中途のデータはdurationで定義したものがはいっているとして仮定された値になるみたいです。
 * 
 * なのでいまのところこのmp3のsegmentの出力では、頭からのすべてのデータを書き出す方向でつくってあります。
 * @author taktod
 */
public class Mp3SegmentCreator extends SegmentCreator{
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(Mp3SegmentCreator.class);
	/** コーデック情報があっているか確認 */
	private boolean isCodecOk = false;
	/** 無音時のDummy mp3 */
	private static final byte[] noSoundMp3 = {
		(byte)0xff, (byte)0xfb, (byte)0x52, (byte)0x64, (byte)0xa9, (byte)0x0f, (byte)0xf0, (byte)0x00, (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x0d, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0xa4, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x34, (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x4c, (byte)0x41, (byte)0x4d, (byte)0x45, (byte)0x33, (byte)0x2e, (byte)0x39, (byte)0x38, (byte)0x2e, (byte)0x34, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55};
	/** 処理済みパケット数保持(パケット数から計算したら正確な進行時間が計算できるため。) */
	private int decodedPackets = 0;
	/** 1セグメントの長さ定義 */
	private static int duration = 2000; // とりあえず2秒固定
	/** 一時保存場所定義 */
	private static String tmpPath;
	
	private FileOutputStream mp3File;
	/** カウンター */
	private int counter;
	/** 次のセグメントの開始位置 */
	private long nextStartPos;
	/**
	 * duration設定
	 */
	@Override
	public void setDuration(int value) {
//		duration = value;
	}
	/**
	 * duration参照
	 */
	@Override
	protected int getDuration() {
		return duration;
	}
	/**
	 * 一時置き場設定
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
	 * 一時置き場参照
	 */
	@Override
	protected String getTmpPath() {
		return tmpPath;
	}
	private static String command = null;
	@Override
	public void setCommand(String command) {
		Mp3SegmentCreator.command = command;
	}
	@Override
	protected String getCommand() {
		return command;
	}
	/**
	 * 初期化
	 * @param name
	 * @param streamInfo
	 */
	public void initialize(String name, ISimpleMediaFile streamInfo) {
		// mpegtsの出力マネージャーから、mp3の種類を確認しておく。
		// 無音mp3の関係から、64kb/s 2ch 44100Hzでないと動作できない？
		// 処理拡張しをmp3に変更
		// tsSegmentCreatorの初期化実施
		setName(name);
		prepareTmpPath();
		reset();
		// mp3として動作可能なデータか確認
		checkMp3Setting(streamInfo);
	}
	/**
	 * 内部データをリセットします。
	 */
	private void reset() {
		close();
		counter = 1; // カウンターは1からにしておく。
		decodedPackets = 0; // 処理済みパケット数の記録動作をリセットするのをわすれてた。
		nextStartPos = getDuration();
		try {
			mp3File = new FileOutputStream(getTmpTarget() + counter + ".mp3");
		}
		catch (Exception e) {
			logger.error("初期化操作でエラーが発生しました。", e);
		}
	}
	/**
	 * セグメントの内容を書き込みます。
	 * @param buf
	 * @param size
	 * @param timestamp
	 */
	private void _writeSegment(byte[] buf, int size, long timestamp) {
		if(mp3File != null) {
			try {
				// タイムスタンプの確認と、バッファがキーであるか確認。
				if(timestamp > nextStartPos) {
					// 以前のファイル出力を停止する。
					mp3File.close();
					// 出力用のm3u8ファイルの準備
					// TODO hoge.m3u8固定になっているので、名前を変更しておきたい。
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(getTmpTarget() + "index.m3u8", false)));
					pw.println("#EXTM3U");
					pw.println("#EXT-X-ALLOW-CACHE:NO");
					pw.println("#EXT-X-TARGETDURATION:2");
					int start = 16 * (int)((counter - 3) / 16) + 1;
					pw.print("#EXT-X-MEDIA-SEQUENCE:");
					pw.println(start);
					for(int i = start;i <= counter;i ++) {
						pw.println("#EXTINF:2");
						pw.print(i);
						pw.println(".mp3");
					}
					pw.close();
					pw = null;
					// 次の切断場所を定義
					nextStartPos = timestamp + getDuration();
					// カウンターのインクリメント
					counter ++;
					// データ出力先のストリームを開いておく。
					mp3File = new FileOutputStream(getTmpTarget() + counter + ".mp3");
				}
				// データの追記
				mp3File.write(buf);
			}
			catch (Exception e) {
				logger.error("セグメント書き込み中にエラーが発生しました。", e);
			}
		}
	}
	/**
	 * エンコードの方式が合致するか確認する。
	 * @param streamInfo
	 */
	private void checkMp3Setting(ISimpleMediaFile streamInfo) {
		isCodecOk = streamInfo.getAudioCodec() == ID.CODEC_ID_MP3 // mp3である。
			&& streamInfo.getAudioBitRate() == 64000 // 64k
			&& streamInfo.getAudioChannels() == 2 // 2channel
			&& streamInfo.getAudioSampleRate() == 44100; // 44.1 kHz
		if(!isCodecOk) {
			logger.error("mp3SegmentCreatorが対応できないフォーマットが出力されています。");
		}
	}
	/**
	 * セグメントの書き込みをおこないます。
	 * @param buf
	 * @param size
	 * @param position
	 */
	public void writeSegment(byte[] buf, int size, long timestamp) {
		if(!isCodecOk) {
			return;
		}
		// 現在までに消化したpacket数から正確なタイムスタンプをだして、設定されているタイムスタンプ以下になる場合は、前の部分に無音mp3を追記してやる。
		fillNoSound(timestamp);
		int position = getPosition();
		_writeSegment(buf, size, position);
		decodedPackets ++;
	}
	/**
	 * 他のデータの進行状況にあわせて無音部の書き込みを実施します。
	 * @param position
	 */
	public void updateSegment(long timestamp) {
		if(!isCodecOk) {
			return;
		}
		// ここでは、現行動作している部分から、0.5秒 or 1秒程度おくれたところまで追記を実施します。(映像と音声がずれる懸念があるため。)
		fillNoSound(timestamp - 1000); // １秒前まで仮にうめておくものとします。
	}
	/**
	 * 無音部の追記を実際に実行します。
	 * @param position
	 */
	private void fillNoSound(long timestamp) {
		// 自分のいる位置が、１パケット以上分の差がない場合は、無音パケットでうめなければ、いけない。
		// (decodedPackets * 11520 / 441) // パケット量から換算する経過時間
		while(true) { // 0.027秒以上余っている場合は、無音パケットが入る余地あり
			int position = getPosition();
			if(timestamp - position <= 30) {
				break;
			}
			_writeSegment(noSoundMp3, noSoundMp3.length, position); // mp3はすべてキーパケット扱いにできる。
			decodedPackets ++;
		}
	}
	/**
	 * 閉じる
	 */
	@Override
	public void close() {
		if(mp3File != null) {
			try {
				mp3File.close();
			}
			catch (Exception e) {
				logger.error("ストリームを閉じる際にエラーが発生しました。", e);
			}
		}
	}
	public int getPosition() {
		return (int)(decodedPackets * 11520L / 441);
	}
}
