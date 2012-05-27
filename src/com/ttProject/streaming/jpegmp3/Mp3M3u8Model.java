package com.ttProject.streaming.jpegmp3;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * mp3ベースのm3u8ストリームを作成するモデル(音声がない場合でも勝手に無音のmp3を作成していきます。)
 * @author taktod
 */
public class Mp3M3u8Model {
	// m3u8を作成し、かつmp3ファイルも作成していく。
	// mp3１ファイルにつきどのくらいのdurationを持たせるか
	private static int duration = 2; // 2秒ごとのmp3を作成することにする。
	// デコード済みのパケット数を保持しておく。(時間を正確に出力するために必要)
	private static int decodedPackets = 0;
	// 出力パス
//	private static String path = "/Users/todatakahiko/Sites/stest/mp3/";
	private static String path;
	/**
	 * @param path the path to set
	 */
	public static void setPath(String path) {
		Mp3M3u8Model.path = path;
	}
	private static String urlPath;
	public static void setUrlPath(String path) {
		Mp3M3u8Model.urlPath = path;
	}
	// 出力FileOutputStreamオブジェクト TODO finalizeを考慮して、きちんと閉じる必要あり。
	private static FileOutputStream mp3File = null;
	// ファイルの番号インデックス
	private static int num = 0;
	// 無音用のmp3データ(無音がつづくならいいけど、フレームドロップとかでこれが混入するとプチっという音がはいると思う。)
	private static final byte[] noSoundMp3 = {
		(byte)0xff, (byte)0xfb, (byte)0x52, (byte)0x64, (byte)0xa9, (byte)0x0f, (byte)0xf0, (byte)0x00, (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x0d, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0xa4, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x34, (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x4c, (byte)0x41, (byte)0x4d, (byte)0x45, (byte)0x33, (byte)0x2e, (byte)0x39, (byte)0x38, (byte)0x2e, (byte)0x34, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55};
	public static void updatePacketMp3(int position) {
		// 動画側が更新されたときもpositionを確認して、必要であればすぐに無音のmp3を挿入する。
		try {
			// mp3Fileの記述がはじまっていない場合ははじめる。
			if(mp3File == null) {
				mp3File = new FileOutputStream(path + num + ".mp3");
				try {
					// m3u8の開始タグ挿入
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path + "test.m3u8", false)));
					pw.println("#EXTM3U");
					pw.println("#EXT-X-TARGETDURATION:" + duration);
					pw.close();
					pw = null;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				num ++;
			}
			// 読み込み済みのpacketsの数
			while(position - (int)(decodedPackets * 11520 / 441) > 27) { // 0.027秒以上余分なデータがある場合は、空白パケットをいれる余裕があるので、挿入する。
				mp3File.write(noSoundMp3);
				decodedPackets ++;
				// ２秒のdurationになった場合mp3をカットします。
				if((int)(decodedPackets * 1440 / 441) > num * duration * 1000) {
					// mp3を切って、次のmp3に書き込みます。
					try {
						PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path + "test.m3u8", true)));
						pw.println("#EXTINF:" + duration);
						pw.println(urlPath + (num - 1) + ".mp3");
						pw.close();
						pw = null;
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					mp3File.close();
					mp3File = new FileOutputStream(path + num + ".mp3");
					num ++;
				}
			}
		}
		catch (Exception e) {
		}
	}
	// ヘッダ解析を実行して、mp3の長さを計算します。
	public static void makePacketMp3(byte[] packet, int position) {
		try {
			// 今回のmp3パケットを注入する。いまのところ１パケットずつ処理がとんでくることを期待します。
			updatePacketMp3(position);
			// TODO パケットを監視して、２パケット以上同時にきたらすくなくともalertを出して対処を考える必要がある。
			mp3File.write(packet);
			decodedPackets ++;
			// ２秒のdurationになった場合mp3をカットします。
			if((int)(decodedPackets * 11520 / 441) > num * duration * 1000) {
				// mp3を切って、次のmp3に書き込みます。
				try {
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path + "test.m3u8", true)));
					pw.println("#EXTINF:" + duration);
					pw.println(urlPath + (num - 1) + ".mp3");
					pw.close();
					pw = null;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				mp3File.close();
				mp3File = new FileOutputStream(path + num + ".mp3");
				num ++;
			}
		}
		catch (Exception e) {
		}
	}
}
