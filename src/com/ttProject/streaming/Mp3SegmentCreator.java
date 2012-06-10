package com.ttProject.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.ICodec.ID;

/**
 * 自作のjpegmp3ストリーム用のmp3のsegmentを作成します。
 * segmentを書き込むと同時にm3u8の定義ファイルもかかないとだめ、このあたりの動作はtsSegmentCreatorとほぼ同じ
 *  この部分の動作はTsSegmentCreatorとそっくりなので(そもそもtsSegmentをつくるのと同義だし)そっちを継承した方がいい感じがした。
 * @author taktod
 */
public class Mp3SegmentCreator extends TsSegmentCreator{
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
	 * tsSegmentで定義されている名前のみの初期化は禁止
	 */
	@Deprecated
	public void initialize(String name) {
		throw new RuntimeException("mp3SegmentCreatorのinitialize(String name)はつかってはいけません。");
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
		setExt(".mp3");
		// tsSegmentCreatorの初期化実施
		super.initialize(name);
		// mp3として動作可能なデータか確認
		checkMp3Setting(streamInfo);
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
		writeSegment(buf, size, position, true);
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
			writeSegment(noSoundMp3, noSoundMp3.length, position, true); // mp3はすべてキーパケット扱いにできる。
			decodedPackets ++;
		}
	}
	@Override
	public void close() {
		super.close();
	}
}
