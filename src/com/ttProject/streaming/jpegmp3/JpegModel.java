package com.ttProject.streaming.jpegmp3;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Set;

import javax.imageio.ImageIO;

import com.ttProject.red5.server.plugin.websocket.WebSocketConnection;
import com.ttProject.streaming.Application;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

/**
 * 時間からjpegを作成していくモデル(jpeg - mp3ストリーミング用)
 * @author taktod
 */
public class JpegModel {
	/** 出力サイズ指定、とりあえず容量を減らすために、小さくしておきたい。 */
	private static final int outputWidth = 160, outputHeight = 120;
	/** 現在扱っているストリームのサイズ。ストリームのデータが変更になった場合は、このデータを書き換える必要がある。 */
	private static int width = -1, height = -1;
	/** 出力用のイメージデータ */
	private static final BufferedImage outputImage = new BufferedImage(160, 120, BufferedImage.TYPE_3BYTE_BGR);
	/** コンバート処理用もオブジェクト */
	private static IConverter converter = null;
	/** 出力パス */
	private static String path;
	public static void setPath(String path) {
		JpegModel.path = path;
	}
	/** アクセス用のurl指定 */
	@SuppressWarnings("unused") // とりあえずは、javascript側で自分で計算する。
	private static String urlPath;
	public static void setUrlPath(String path) {
		JpegModel.urlPath = path;
	}
	/** pictureフレームからjpgデータを作成します。ImageIOをつかっていますが、クオリティの設定等を実行した方がいいか？でも処理が遅くなるか？ */
	public static void makeFramePicture(IVideoPicture picture, int position) {
		BufferedImage javaImage = null;
		// 前回とストリームのサイズが変更になっている場合は処理を変更する必要あり。
		if(width == -1 || height == -1 || width != picture.getWidth() || height != picture.getHeight()) {
			width = picture.getWidth();
			height = picture.getHeight();
			javaImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			converter = ConverterFactory.createConverter(javaImage, IPixelFormat.Type.YUV420P);
		}
		try {
			// とりあえず強制160x120変換を実行しておく。
			javaImage = converter.toImage(picture);
			File file = new File(path + ((int)(position / 100)) + ".jpg");
			// サイズ変換を実行する場合はこちら
			Graphics2D g2d = outputImage.createGraphics();
			g2d.drawImage(javaImage, 0, 0, outputWidth, outputHeight, null);
			ImageIO.write(outputImage, "jpeg", file);
			// そのままコピーする場合はjavaImageをベースに書き込めばOK
//			ImageIO.write(javaImage, "jpeg", file);
			
			// webSocket経由でつながっているユーザーのメッセージをおくる。 将来的に別のクラスにもっていく必要あり。
			Set<WebSocketConnection> conns = Application.conns;
			synchronized(conns) {
				for(WebSocketConnection conn : conns) {
					// 読み込み可能になったファイルを教える。
					// 番号のみ扱うことで、相手側で処理ができる部分があるので、そこを考慮しなければいけない。
					conn.send("" + ((int)(position / 100)));
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
