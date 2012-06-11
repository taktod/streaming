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
 * segmentを書き込むと同時にm3u8の定義ファイルもかかないとだめ、このあたりの動作はtsSegmentCreatorとほぼ同じ
 *  この部分の動作はTsSegmentCreatorとそっくりなので(そもそもtsSegmentをつくるのと同義だし)そっちを継承した方がいい感じがした。
 * TODO やってみたところ、どうやらaudioタグのcurrentTimeは再生してからの経過時間しかとれないみたいです。
 * 中途からはじめた、セグメントの場合、再生開始したところからの経過時間しかとれないので、有用ではありませんでした。
 * いまのところ、始めから全データがはいっているデータとして、設置しておかないと動作できないみたいです。
 * ダウンロードしなければいけないファイルサイズがおおきくなるので、オーバーヘッドが大きくなってしまいそうですが、まぁ仕方ない。
 * playしたまま放置という状況になると、再開したときに位置情報がリセットされずに、動作することがあるみたい。(追記扱い？)
 * どうしたものか・・・
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
	private static int duration;
	/** 一時保存場所定義 */
	private static String tmpPath;
	
	private FileOutputStream outputStream;
	private int counter;
	private long nextStartPos;
	/**
	 * duration設定
	 */
	@Override
	public void setDuration(int value) {
		duration = value;
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
//		super.initialize(name);
		// mp3として動作可能なデータか確認
		checkMp3Setting(streamInfo);
	}
	public void reset() {
		close();
		counter = 0;
		nextStartPos = getDuration();
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(getTmpTarget() + "hoge.m3u8", true)));
			pw.println("#EXTM3U");
			pw.println("#EXT-X-ALLOW-CACHE:NO"); // キャッシュするかどうかの指定
			pw.print("#EXT-X-TARGETDURATION:"); // 何個前のデータから読み出すかの指定
			pw.println((int)(getDuration() / 1000)); // この値は間違ってる。最低でも2以上いれないとだめっぽい。
			pw.println("#EXT-X-MEDIA-SEQUENCE:0"); // このファイルの先頭がどこであるかの指定
			pw.flush();
			pw.close();
			outputStream = new FileOutputStream(getTmpTarget() + counter + ".mp3");
		}
		catch (Exception e) {
		}
	}
	private void _writeSegment(byte[] buf, int size, long timestamp) {
		if(outputStream != null) {
			try {
				// タイムスタンプの確認と、バッファがキーであるか確認。
				if(timestamp > nextStartPos) {
					// 以前のファイル出力を停止する。
					outputStream.close();
					// 出力用のm3u8ファイルの準備
					// TODO hoge.m3u8固定になっているので、名前を変更しておきたい。
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(getTmpTarget() + "hoge.m3u8", true)));
/*					pw.println("#EXTM3U");
					pw.println("#EXT-X-ALLOW-CACHE:NO");
					pw.print("#EXT-X-TARGETDURATION:");
					pw.println((int)(getDuration() / 1000));
					if(counter - 2 >= 0) {
						pw.print("#EXT-X-MEDIA-SEQUENCE:");
						pw.println(counter - 2);
						// この部分に、いままでのdurationすべてを含むデータをいれてやる必要がある。
						pw.print("#EXTINF:");
//						pw.println((int)(getDuration() / 1000)); // このdurationを正しい形にしておく。
						pw.println((counter - 1) * (getDuration() / 1000)); // このdurationを正しい形にしておく。
						pw.print(counter - 2);
						pw.println(".mp3");
//						pw.println("unknown.mp3");
						pw.print("#EXTINF:");
						pw.println((int)(getDuration() / 1000));
						pw.print(counter - 1);
						pw.println(".mp3");
					}
					else if(counter - 1 >= 0) { 
						pw.print("#EXT-X-MEDIA-SEQUENCE:");
						pw.println(counter - 1);
						pw.print("#EXTINF:");
						pw.println((int)(getDuration() / 1000));
						pw.print(counter - 1);
						pw.println(".mp3");
					}
					else {
						pw.println("#EXT-X-MEDIA-SEQUENCE:0");
					}*/
					pw.print("#EXTINF:");
					pw.println((int)(getDuration() / 1000));
					pw.print(counter);
					pw.println(".mp3");
					pw.close();
					pw = null;
					// 次の切断場所を定義
					nextStartPos = timestamp + getDuration();
					// カウンターのインクリメント
					counter ++;
					// データ出力先のストリームを開いておく。
					outputStream = new FileOutputStream(getTmpTarget() + counter + ".mp3");
				}
				// データの追記
				outputStream.write(buf);
			}
			catch (Exception e) {
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
		int position = (int)(decodedPackets * 11520 / 441);
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
			int position = (int)(decodedPackets * 11520 / 441);
//			if(timestamp - position <= 27) { // パケットの長さは正しくは0.026...になるのですが、たまに送れることがあるようなので余裕をみて0.03にしておきます。
			if(timestamp - position <= 30) {
				break;
			}
			logger.info("無音部を足して調整しました。");
			_writeSegment(noSoundMp3, noSoundMp3.length, position); // mp3はすべてキーパケット扱いにできる。
			decodedPackets ++;
		}
	}
	@Override
	public void close() {
//		super.close();
		if(outputStream != null) {
			try {
				outputStream.close();
			}
			catch (Exception e) {
			}
		}
	}
}
