package com.ttProject.output;

/**
 * ftp経由で特定の場所にデータを書き出す動作
 * @author taktod
 */
public class FtpAppender implements IDataAppender{
	// ftpによるデータのアップロードに必要なもの。
	@Override
	public void writeBytes(byte[] data, int size) {
	}
	@Override
	public void writeText(String data) {
	}
}
