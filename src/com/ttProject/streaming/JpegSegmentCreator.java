package com.ttProject.streaming;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

/**
 * creatorはすべて適当なディレクトリにいったんデータを書き出して、それを全体で共有する形ですすめる。
 * 次のようにします。
 * j3u8というファイルを定義して、そこにデータリストを出力します。
 * duration分のデータを１つのjpegデータとして書き込むようにしておきます。
 * 場所は縦長でとりあえず逝ってみたいとおもいます。
 * [0]
 * [1]
 * [2]
 * [3]
 * という縦長な画像をつくるイメージ
 * 細分化については適当に処理してやるとりあえず10fpsで逝ってみたい。
 * 動作的にもダウンロードサイズ的にも問題はでないっぽいですが、このファイルの準備ができたという情報をなんとかして送り届けてやりたい。
 * でないと描画中のファイルにあたった場合に絵がくずれる。
 * やっぱり細かい部分はwebSocketをつかってやりくりする必要があるのだろうか？
 * 
 * duration分たまったら次のjpegに移動みたいな感じ
 * @author taktod
 */
public class JpegSegmentCreator extends SegmentCreator{
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(JpegSegmentCreator.class);
	/** １画像あたりの長さ(1000とかにしておく。) */
	private static int duration;
	/** データの一時置き場 */
	private static String tmpPath;
	/**
	 * 1画像あたりの長さ指定
	 */
	@Override
	public void setDuration(int value) {
		duration = value;
	}
	/**
	 * 1画像あたりの長さ参照
	 */
	@Override
	protected int getDuration() {
		return duration;
	}
	/**
	 * 一時データ置き場指定
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
	 * 一時データ置き場参照
	 */
	@Override
	protected String getTmpPath() {
		return tmpPath;
	}
	/**
	 * 閉じるときの動作
	 */
	@Override
	public void close() {
	}
	/** 現在扱っている画像のサイズ */
	private int width = -1,height = -1;
	/** とりいそぎ160x1200にして、そこに160x120の画像10個保持する形にして、送りだす。というやり方をとってみる。 */
	// 仮に3x3の形にしてみましたが、サイズ的にあまりかわりませんでした。
	private BufferedImage outputImage = new BufferedImage(160, 1200, BufferedImage.TYPE_3BYTE_BGR);
	/** コンバート処理用オブジェクト */
	private IConverter converter = null;
	/** 画像カウンター(こいつはファイルのデータ) */
	private int counter = 0;
	private byte[] frameStatus = new byte[10]; // 内部フレームの有無保持
	/**
	 * 初期化しておく。
	 * @param name
	 */
	public void initialize(String name) {
		setName(name);
		prepareTmpPath();
		resetFrameStatus();
	}
	private void resetFrameStatus() {
		for(int i = 0;i < 10;i ++) {
			frameStatus[i] = 0;
		}
	}
	/**
	 * 映像の画像要素から画像を取り出して保存する。
	 * @param picture
	 * @param timstamp
	 */
	public void makeFramePicture(IVideoPicture picture, long timestamp) {
		BufferedImage javaImage = null;
		// 前回のデータとビデオサイズが変更になっている場合はコンバート用のオブジェクトを書き換える必要あり。
		if(width == -1 || height == -1 || width != picture.getWidth() || height != picture.getHeight()) {
			width  = picture.getWidth();
			height = picture.getHeight();
			javaImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			converter = ConverterFactory.createConverter(javaImage, IPixelFormat.Type.YUV420P);
		}
		try {
			if((int)(timestamp / 1000) != counter) {
				// あたらしいイメージの処理にはいるので、画像を保存しておく。
				ImageIO.write(outputImage, "jpeg", new File(getTmpTarget() + counter + ".jpg"));
				// すでにおわったデータを書き込みます。
				PrintWriter pw;
				if(counter % 60 == 0) {
					// あたらしいデータの場合
					pw = new PrintWriter(new BufferedWriter(new FileWriter(getTmpTarget() + "index.jpl", false)));
					pw.print("#JPL-X-MEDIA-SEQUENCE:");
					pw.println(counter);
				}
				else {
					pw = new PrintWriter(new BufferedWriter(new FileWriter(getTmpTarget() + "index.jpl", true)));
				}
				pw.print("#JPLINF:");
				for(byte b : frameStatus) {
					pw.print(b);
				}
				pw.print(":");
				pw.print(counter);
				pw.println(".jpg");
				pw.close();
				counter = (int)timestamp / 1000;
				outputImage = new BufferedImage(160, 1200, BufferedImage.TYPE_3BYTE_BGR);
				resetFrameStatus();
			}
			javaImage = converter.toImage(picture);
			Graphics2D g2d = outputImage.createGraphics();
			int pos = (int)(timestamp / 100) % 10;
			g2d.drawImage(javaImage, 0, 120 * pos, 160, 120, null);
			// 画像の書き込みを実行した段階でエレメントの有無は決定する。
			frameStatus[pos] = 1;
		}
		catch (Exception e) {
			logger.error("画像コンバート中にエラーが発生しました。", e);
		}
	}
}
