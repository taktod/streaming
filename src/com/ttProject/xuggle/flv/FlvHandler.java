package com.ttProject.xuggle.flv;

import java.nio.ByteBuffer;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.xuggle.xuggler.io.IURLProtocolHandler;

/**
 * 実際にffmpegからデータの要求を求められるクラス
 * @author taktod
 */
public class FlvHandler implements IURLProtocolHandler {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(FlvHandler.class);
	/** flvDataQueue */
	private FlvDataQueue inputQueue;
	/** 最後読み込みバッファの残り */
	private ByteBuffer lastReadBuffer = null;
	/**
	 * コンストラクタ
	 * @param queue
	 */
	public FlvHandler(FlvDataQueue queue) {
		inputQueue = queue;
	}
	/**
	 * 閉じる要求がきたときの処理
	 * @return -1:エラー 0:それ以外
	 */
	@Override
	public int close() {
		// 何もしません
		return 0;
	}
	/**
	 * xuggle固有の処理、ストリームをサポートしているか？ストリームをサポートしているとした場合seekイベントが発行されなくなります。
	 * @param url 入力urlデータ
	 * @param flags 入力フラグ
	 * @return true:ストリームである。false:ストリームではない。
	 */
	@Override
	public boolean isStreamed(String url, int flags) {
		// ストリームを名乗っておきます。
		return true;
	}
	/**
	 * ファイルオープン処理(ffmpegから呼び出されます。)
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
	 * この処理では半端なデータをなげて問題ありません。
	 * @param buf 読み込み実体バッファ。ここに書き込んでおけば処理後にffmpegが処理します。
	 * @param size 最大で書き込み可能なサイズです。このサイズを超えたデータをbufに書き込んではいけません。
	 * @return 0:ファイル終了 数値:読み込めたバイト数 -1:エラー
	 */
	@Override
	public int read(byte[] buf, int size) {
		// size分までしか読み込みする必要がないので、byteBufferとして、size分メモリーを準備しておく。
		ByteBuffer readBuffer = ByteBuffer.allocate(size);
		while(readBuffer.hasRemaining()) {
			// 読み込み可能byteがsizeより小さい場合
			// queueからデータを取り出して、応答できる限り応答する。
			ByteBuffer packet = null;
			if(lastReadBuffer != null) {
				packet = lastReadBuffer;
				lastReadBuffer = null;
			}
			else {
				packet = inputQueue.read();
			}
			if(packet == null) {
				// 読み出せるパケットがなくなったら脱出する。
				break;
			}
			if(readBuffer.remaining() < packet.remaining()) {
				byte[] readBytes = new byte[readBuffer.remaining()];
				packet.get(readBytes);
				readBuffer.put(readBytes);
				// 読み込みがあふれる場合
				lastReadBuffer = packet;
				break;
			}
			else {
				// まだ読み込み可能な場合
				byte[] readBytes = new byte[packet.remaining()];
				packet.get(readBytes);
				readBuffer.put(readBytes);
			}
		}
		readBuffer.flip();
		int length = readBuffer.limit();
		readBuffer.get(buf, 0, length);
		return length;
	}
	/**
	 * ffmpegからシーク要求があった場合の処理
	 * @param offset データのオフセット
	 * @param whence 元の位置
	 * @return -1:サポートしない。それ以外の数値はwhenceからの相対位置
	 */
	@Override
	public long seek(long offset, int whence) {
		return -1;
	}
	/**
	 * 書き込み(ffmpegから出力される場合に呼び出されます。)
	 * この処理では半端なデータが届く可能性があります。
	 * @param buf 書き込み実体バッファ
	 * @param size 書き込みサイズ
	 * @return 0:ファイル終了 数値:書き込めたバッファ量 -1:エラー
	 */
	@Override
	public int write(byte[] buf, int size) {
		logger.error("入力用動作、出力要求されました。");
		return -1;
	}
}
