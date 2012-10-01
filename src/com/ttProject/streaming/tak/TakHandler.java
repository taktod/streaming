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
	/** takStreamingの出力先 */
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
//		logger.info("書き込みがきました。" + outputDirectory);
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
