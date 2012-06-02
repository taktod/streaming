package com.ttProject.output;

/**
 * 複数のターゲットに書き込みをおこなうアペンダー
 * 出力を大量のサイトにftpであげたい場合用
 * @author taktod
 */
public class MultipleAppender implements IDataAppender {
	@Override
	public void writeBytes(byte[] data, int size) {
	}
	@Override
	public void writeText(String data) {
	}
}
