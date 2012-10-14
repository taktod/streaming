package com.ttProject.streaming.webm.data;

import java.nio.ByteBuffer;

/**
 * webmストリーミングで利用するメディアパケット
 * @author taktod
 */
public class WebMMediaPacket extends WebMPacket {
	public static final int TimecodeId = 0xE7;
	// 先頭はClusterIDであるはず。(currentHeaderは処理途上でbyteBufferの中身がなくなった場合に、あとから参照し直すことがある。)
	private long currentHeader = 0x00L;
	/**
	 * おわったから終わった分とデータがたりなくておわったものと２つある。
	 */
	@Override
	public boolean analize(ByteBuffer buffer) {
		// ここでの処理は３つに分かれる
		// ヘッダ情報取得
		// サイズ情報取得
		// 内容データ取得
		while(buffer.remaining() > 0) {
			// まずヘッダを確認します。
			Boolean result = checkHeader(buffer);
			if(result != null) {
				return result;
			}
			result = checkSize(buffer);
			if(result != null) {
				return result;
			}
			result = checkBody(buffer);
			if(result != null) {
				return result;
			}
		}
		// 足りない
		return false;
	}
	/**
	 * サイズ情報の確認(拡張動作)
	 * @param buffer
	 * @return
	 */
	@Override
	protected Boolean checkSize(ByteBuffer buffer) {
		if(currentHeader == TimecodeId) {
			// timeCodeIdの場合だけ別処理をする必要あり。
			Long size = getEBMLSize(buffer);
			if(size == null) {
				// 足りない
				return false;
			}
			getBuffer(8).put((byte)0x88); // 保存データ量を8バイト固定にしておく。
			setRemainData(size.intValue());
			return null;
		}
		else {
			return super.checkSize(buffer);
		}
	}
	@Override
	protected Boolean checkBody(ByteBuffer buffer) {
		if(currentHeader == TimecodeId) {
			if(buffer.remaining() > getRemainData()) {
				long data = 0;
				byte[] targetData = new byte[getRemainData()];
				buffer.get(targetData);
				for(byte b : targetData) {
					data = data * 0x0100 + (b & 0xFF);
				}
				getBuffer(8).putLong(data);
				setIsHeaderWritten(false);
				resetRemainData();
				return null;
			}
			else {
				// バッファがたりない。
				return false;
			}
		}
		else {
			return super.checkBody(buffer);
		}
	}
}
