package com.ttProject.xuggle.in.flv;

import java.nio.ByteBuffer;

import org.red5.io.utils.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.io.IURLProtocolHandler;

/**
 * 実際にffmpegからデータの要求等を実行されるクラス
 * @author taktod
 */
public class FlvHandler implements IURLProtocolHandler {
	private final Logger logger = LoggerFactory.getLogger(FlvHandler.class);
	/** flvDataQueue */
	private FlvDataQueue inputQueue;
	/** 最終読み込みバッファの残り */
	private ByteBuffer lastReadBuffer = null;
	/**
	 * コンストラクタ
	 * @param manager
	 */
	public FlvHandler(FlvDataQueue queue) {
		inputQueue = queue;
	}
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
		logger.info("openのトライ:" + url);
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
		logger.info("読み込みのトライ:" + size);
		// size分までしか読み込みする必要がないので、byteBufferとして、size分メモリーを準備しておく。
		ByteBuffer readBuffer = ByteBuffer.allocate(size);
		while(readBuffer.hasRemaining()) {
			// 読み込み可能byteがsizeより小さい場合
			// queueからデータを取り出して、応答できる限り応答する。
			ByteBuffer packet = null;
			if(lastReadBuffer != null) {
				packet = lastReadBuffer;
			}
			else {
				packet = inputQueue.read();
			}
			if(packet == null) {
				// 読み出せるパケットがなくなったら脱出する。
				System.out.println("読み出せるパケットがなくなった。");
				break;
			}
			System.out.println("packet pos" + packet.position());
			System.out.println("packet" + packet.limit());
			if(readBuffer.remaining() < packet.limit()) {
				System.out.println("たりないから、端数のみ読み込み処理する。");
/*				byte[] readBytes = new byte[readBuffer.remaining()];
				packet.get(readBytes);
				readBuffer.put(readBytes);
				// 読み込みがあふれる場合
				lastReadBuffer = packet;*/
				lastReadBuffer = packet;
				break;
			}
			else {
				// まだ読み込み可能な場合
//				System.out.println("余裕があるので、そのまま書き込む");
				readBuffer.put(packet);
			}
		}
		readBuffer.flip();
		int length = readBuffer.limit();
		logger.info("実際の読み込みデータ量:" + length);
		readBuffer.get(buf, 0, length);
//		System.out.println(HexDump.toHexString(buf));
		return length;
	}
	/**
	 * ffmpegからシークの要求があった場合の処理
	 * @param offset データのオフセット
	 * @param whence 元の位置
	 * @return -1:サポートしない。 それ以外の数値はwhenceからの相対位置
	 */
	@Override
	public long seek(long offset, int whence) {
		return -1;
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
		return 0;
	}
}
