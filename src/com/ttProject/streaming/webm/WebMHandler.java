package com.ttProject.streaming.webm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.io.IURLProtocolHandler;

/**
 * WebMのストリーミングの出力を書き出すクラス
 * @author taktod
 */
public class WebMHandler implements IURLProtocolHandler {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(WebMHandler.class);
	@Override
	public int close() {
		return 0;
	}
	@Override
	public boolean isStreamed(String url, int flags) {
		return true;
	}
	@Override
	public int open(String url, int flags) {
		return 0;
	}
	@Override
	public int read(byte[] buf, int size) {
		logger.error("出力用クラスに書き込み要求がきた。");
		return -1;
	}
	@Override
	public long seek(long offset, int whence) {
		return -1;
	}
	@Override
	public int write(byte[] buf, int size) {
		return 0;
	}
}
