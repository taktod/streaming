package com.ttProject.streaming.hls.data;

import java.nio.ByteBuffer;

public class HlsMediaPacket extends HlsPacket {
	@Override
	public boolean isHeader() {
		return false;
	}
	@Override
	public boolean analize(ByteBuffer buffer) {
		while(buffer.remaining() >= 188) {
			int position = buffer.position();
			System.out.println(position);
			int pid = getPid(buffer);
			if(isH264Pid(pid)) {
				// h.264のパケットの場合
				System.out.println("h264のパケットきたー");
				// キーパケットであるか確認する。
				if(isH264KeyPacket(buffer) && getBufferSize() > 0) {
					// バッファがある状態でキーパケットがきたら。次のパケットに進む。
					System.out.println(getBufferSize());
					// データを解析します。pidがh.264のものだったら、解析する。
					// pidがそれ以外だったら、単に追記する
					// 書き込みが一段落した
//					System.exit(0);
//					break;
					return true;
				}
			}
//			else {
//				// その他のパケットの場合
//				System.out.println("その他パケットきたー");
//			}
//			break;
			// 188バイトのデータを追記します。
			byte[] data = new byte[188];
			buffer.get(data);
			getBuffer(188).put(data);
		}
		// パケットが不足したはず。
		return false;
	}
	// h.264パケットとして解析してみる。
	protected boolean isH264KeyPacket(ByteBuffer buffer) {
		int position = buffer.position();
		buffer.position(position + 4);
		int data = buffer.getShort();
		if(data != 0x0750) {
			// キーパケットではないみたい。
			buffer.position(position);
			return false;
		}
		buffer.position(position + 12);
		data = buffer.getInt();
		if(data != 0x000001E0) {
			buffer.position(position);
			return false;
		}
		// キーパケットのようです。
		buffer.position(position);
		return true;
	}
}
