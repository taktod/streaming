package com.ttProject.streaming.tak;

import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.io.IURLProtocolHandler;

/**
 * HttpTakStreamingの出力を書き出すクラス
 * @author taktod
 */
public class TakHandler implements IURLProtocolHandler {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(TakHandler.class);
	/** 出力ディレクトリ */
	private String outputDirectory;
	private FileOutputStream fos = null;
	public TakHandler(String target) {
		outputDirectory = target;
		try {
			fos = new FileOutputStream(outputDirectory);
		}
		catch (Exception e) {
		}
	}
	/**
	 * 閉じる要求がきたときの処理
	 * @return -1:エラー 0:それ以外
	 */
	@Override
	public int close() {
		if(fos != null) {
			try {
				fos.close();
			}
			catch (Exception e) {
			}
			fos = null;
		}
		return 0;
	}
	/**
	 * ストリームであるか
	 * @param url 入力urlデータ
	 * @param flags 入力フラグ
	 * @return true:ストリームである。 false:ストリームではない。
	 */
	@Override
	public boolean isStreamed(String url, int flags) {
		return true;
	}
	/**
	 * ファイルオープン処理
	 * @param url 入力urlデータ
	 * @param flags 入力フラグ
	 * @return 0以上:成功 -1:エラー
	 */
	@Override
	public int open(String url, int flags) {
		return 0;
	}
	/**
	 * 読み込み処理
	 * @param buf 読み込み実体バッファ。
	 * @param size 要求データサイズ
	 * @return 0:ファイル終了 数値:書き込めたバイト数 -1:エラー
	 */
	@Override
	public int read(byte[] buf, int size) {
		logger.error("出力用クラスに書き込み要求がきた。");
		return -1;
	}
	/**
	 * シーク処理
	 * @param offset データのオフセット
	 * @param whence 元の位置
	 * @return -1:サポートしない、それ以外はwhenceからの相対位置
	 */
	@Override
	public long seek(long offset, int whence) {
		return -1;
	}
	/**
	 * 書き込み
	 * @param buf 書き込み実体バッファ
	 * @param size データ量
	 * @return 0:ファイル終了 数値:書き込めたバッファ量 -1:エラー
	 */
	@Override
	public int write(byte[] buf, int size) {
		if(fos != null) {
			try {
				fos.write(buf, 0, size);
			}
			catch (Exception e) {
			}
		}
		return 0;
	}
}
