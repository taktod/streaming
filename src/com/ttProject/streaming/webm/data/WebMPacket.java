package com.ttProject.streaming.webm.data;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import com.flazr.util.Utils;

public abstract class WebMPacket {
	// サイズはどうでもいいや。bufferの中身が書き込むべきデータ
	private ByteBuffer buffer = null; // そのパケットにかかわる実データ
	protected void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}
	protected ByteBuffer getBuffer() {
		return buffer;
	}
	protected ByteBuffer getBuffer(int size) {
		if(buffer == null) {
			buffer = ByteBuffer.allocate(size);
		}
		if(buffer.remaining() >= size) {
			return buffer;
		}
		ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() + size);
		buffer.flip(); // 読み込みモードに変更
		newBuffer.put(buffer); // 追記する。
		buffer = newBuffer;
		return buffer;
	}
	protected int getBufferSize() {
		if(buffer == null) {
			return 0;
		}
		return buffer.position();
	}
	private boolean isHeaderWritten; // ヘッダデータの書き込みをおこなったかフラグ
	protected void setIsHeaderWritten(boolean flg) {
		isHeaderWritten = flg;
	}
	protected boolean isHeaderWrittern() {	                                     
		return isHeaderWritten;
	}
	private int remainData = -1; // 残りのデータ量(この値があるかどうかでsizeデータを書き込んだか判定できる。)
	protected void setRemainData(int data) {
		remainData = data;
	}
	protected int getRemainData() {
		return remainData;
	}
	protected void resetRemainData() {
		remainData = -1;
	}
	/**
	 * byteBufferを解析します。
	 * @param buffer 解析するネタ
	 * @return true:解析完了パケットが書き込みreadyになっています。false:解析途上もっとデータが必要。
	 */
	public abstract boolean analize(ByteBuffer buffer);
	private static byte[] lastEBMLBytes = null;
	protected static byte[] getLastEBMLBytes() {
		return lastEBMLBytes;
	}
	/**
	 * EBMLのデータを解析する。
	 * @param buffer
	 * @param flg
	 * @return
	 */
	private static Long getEBMLData(ByteBuffer buffer, int flg) {
		if(buffer.remaining() < 1) {
			// バッファがない。
			return null;
		}
		int position = buffer.position();
//		byte firstByte = buffer.get();
		byte[] b = new byte[1];
		buffer.get(b);
		byte firstByte = b[0];
		int numBytes = 0;
		int mask = 0x80;
		for(int i = 0;i < 8;i ++) { // 最大で8ビットしか移動できない。
			if((firstByte & mask) == mask) {
				numBytes = i;
				break;
			}
			mask >>>= 1;
		}
		if(buffer.remaining() < numBytes) {
			buffer.position(position);
			return null;
		}
		lastEBMLBytes = new byte[numBytes + 1];
		lastEBMLBytes[0] = firstByte;
		long result = (flg == 1) ? firstByte & (mask - flg) : (firstByte & 0xFF);
		for(int i = 0;i < numBytes; i ++) {
			byte next = buffer.get();
			lastEBMLBytes[i + 1] = next;
			result = result * 0x0100 + (next & 0xFF);
		}
		return result;
	}
	/**
	 * EBMLIdを応答する。
	 * @param buffer 解析元のデータ
	 * @return null:エラー long:IDの値 エラー時にはbufferのpositionを元の場所に戻してあります。
	 */
	public static Long getEBMLId(ByteBuffer buffer) {
		return getEBMLData(buffer, 0);
	}
	/**
	 * EBMLのsizeを応答する。
	 * @param buffer 解析元のデータ
	 * @return null:エラー long:サイズ
	 */
	protected static Long getEBMLSize(ByteBuffer buffer) {
		return getEBMLData(buffer, 1);
	}
	private static int num = 0;
	public void writeData() {
		System.out.println("書き込み実行します。" + buffer.position());
		try {
			WritableByteChannel channel = Channels.newChannel(new FileOutputStream("/home/local/download/test640." + num + ".webm"));
			buffer.flip();
			channel.write(buffer);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			num ++;
		}
	}
}
