package com.ttProject.xuggle.out.mpegts;

import com.xuggle.xuggler.io.IURLProtocolHandler;

public class MpegtsHandler implements IURLProtocolHandler {
	/**
	 * 閉じる要求がきたときの処理
	 * @return -1エラー 0以上それ以外
	 */
	@Override
	public int close() {
		return 0;
	}
	/**
	 * xuggler固有の処理、ストリームサポートしているか？ストリームを名乗った場合はseekイベントがこなくなる。
	 * @param url 入力urlデータ
	 * @param flags 入力フラグ
	 * @return trueストリームである、falseストリームでない
	 */
	@Override
	public boolean isStreamed(String url, int flags) {
		return true;
	}
	/**
	 * ファイルオープン処理(ffmpegから呼び出されます。)
	 * @param url 入力urlデータ(たぶんredfile:xxxx)
	 * @param flags 入力フラグ
	 * @return 0以上で成功 -1エラー
	 */
	@Override
	public int open(String url, int flags) {
		return 0;
	}
	/**
	 * 読み込み(ffmpegから読み込みする場合に呼び出されます。)
	 * この処理では、半端なデータをなげて問題ありません。
	 * @param buf 読み込み実体バッファです。ここに書き込めば、ffmpegに渡されます。参照わたしみたいなものです。
	 * @param size 最大で書き込み可能なサイズです、このサイズをこえてはいけません。
	 * @return 0ファイル終了 数値読み込めたバイト数 -1エラー
	 */
	@Override
	public int read(byte[] buf, int size) {
		return 0;
	}
	/**
	 * ffmpegからシークの要求があった場合の処理
	 * @param offset データのオフセット
	 * @param whence 元の位置
	 * @return -1:サポートしない。 それ以外の数値はwhenceからの相対位置
	 */
	@Override
	public long seek(long offset, int whence) {
		return 0;
	}
	/**
	 * 書き込み(ffmpegから出力される場合に呼び出されます。)
	 * この処理では、半端なデータが届く可能性があります。
	 * @param buf 書き込み実体バッファ
	 * @param size 書き込みサイズ
	 * @return 0ファイル終了 数値書き込めたバッファ量 -1エラー
	 */
	@Override
	public int write(byte[] buf, int size) {
		// 強制的に入力されたデータが書き込みできたことにします。
		return size;
	}
}
