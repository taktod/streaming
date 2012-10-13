package com.ttProject.streaming.webm.data;

import java.nio.ByteBuffer;

import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
// */
import com.ttProject.streaming.data.IMediaPacket;

/**
 * webMのパケット動作ベース
 * @author taktod
 */
public abstract class WebMPacket implements IMediaPacket {
	/** パケットの実データ保持 */
	private ByteBuffer buffer = null;
	/**
	 * バッファデータを参照する。
	 * @param size 要求するデータサイズ
	 * @return ByteBufferデータ
	 */
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
	/**
	 * バッファデータサイズ応答
	 * @return 処理済み数値
	 */
	protected int getBufferSize() {
		if(buffer == null) {
			return 0;
		}
		return buffer.position();
	}
	/** ヘッダ情報書き込み済みフラグ */
	private boolean isHeaderWritten; // ヘッダデータの書き込みをおこなったかフラグ
	protected void setIsHeaderWritten(boolean flg) {
		isHeaderWritten = flg;
	}
	protected boolean isHeaderWritten() {	                                     
		return isHeaderWritten;
	}
	/** 残りデータ量 */
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
	/** 最終EBMLデータ参照時のデータ量 */
	private static byte[] lastEBMLBytes = null;
	protected static byte[] getLastEBMLBytes() {
		return lastEBMLBytes;
	}
	/**
	 * byteBufferを解析します。
	 * @param buffer 解析するネタ
	 * @return true:解析完了パケットが書き込みreadyになっています。false:解析途上もっとデータが必要。
	 */
	public abstract boolean analize(ByteBuffer buffer);
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
	/**
	 * ヘッダー情報の確認(デフォルト動作)
	 * @param buffer
	 */
	protected Boolean checkHeader(ByteBuffer buffer) {
		if(!isHeaderWritten()) {
			int position = buffer.position();
			Long header = getEBMLId(buffer);
			if(header == null) {
				// データ量が足りない
				return false;
			}
			// TODO ClusterIDの設定で切り分けるという部分は書き換えてもいいかもしれない。
			if(header == WebMPacketManager.ClusterId && getBufferSize() != 0) {
				buffer.position(position);
				return true;
			}
			byte[] saveData = getLastEBMLBytes();
			getBuffer(1024).put(saveData);
			setIsHeaderWritten(true);
		}
		return null;
	}
	/**
	 * サイズ情報の確認(デフォルト動作)
	 * @param buffer
	 * @return
	 */
	protected Boolean checkSize(ByteBuffer buffer) {
		// 残り書き込みデータがある場合は、sizeの処理が完了している。
		if(getRemainData() == -1) {
			Long size = getEBMLSize(buffer);
			if(size == null) {
				return false;
			}
			byte[] saveData = getLastEBMLBytes();
			getBuffer(8).put(saveData);
			setRemainData(size.intValue());
		}
		return null;
	}
	/**
	 * 実データの確認(デフォルト動作)
	 * @param buffer
	 * @return
	 */
	protected Boolean checkBody(ByteBuffer buffer) {
		if(buffer.remaining() >getRemainData()) {
			// 残りデータより、bufferが大きい場合は書き込む
			if(getRemainData() > 0) {
				int saveSize = getRemainData();
				byte[] saveData = new byte[saveSize];
				buffer.get(saveData);
				getBuffer(saveSize).put(saveData);
			}
			// 完了
			setIsHeaderWritten(false);
			resetRemainData();
			return null;
		}
		else {
			// 残りデータの方がbufferより大きい場合
			int size = getRemainData();
			int saveSize = buffer.remaining();
			getBuffer(saveSize).put(buffer);
			setRemainData(size - saveSize);
			return false;
		}
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
		byte firstByte = buffer.get();
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





	private static int num = 0;
	/**
	 * ファイルにデータを書き込む(とりあえずテスト)
	 */
	@Override
	public void writeData() {
		System.out.println("書き込み実行します。" + buffer.position());
		try {
			WritableByteChannel channel = Channels.newChannel(new FileOutputStream("/home/xxx/download/test640." + num + ".webm"));
			buffer.flip();
			channel.write(buffer);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			num ++;
		}// */
	}
}
