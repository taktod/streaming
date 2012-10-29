package com.ttProject.streaming.hls.data;

import java.nio.ByteBuffer;

/**
 * httpLiveStreamingで利用するヘッダ情報パケット
 */
public class HlsHeaderPacket extends HlsPacket {
	@Override
	public boolean isHeader() {
		return true;
	}
	@Override
	public boolean analize(ByteBuffer buffer) {
		// header処理
		while(buffer.remaining() >= 188) {
//			int position = buffer.position();
			boolean isPmtChecked = false;
			int pid = getPid(buffer);
			// pmtがくるまで取得しなければいけない。
			if(pid == PATId) {
				analizePat(buffer);
			}
			else if(isPmtId(pid)) {
				analizePmt(buffer);
				isPmtChecked = true;
			}
			// 188バイトのデータを追記します。
			byte[] data = new byte[188];
			buffer.get(data);
			getBuffer(188).put(data);
			if(isPmtChecked) {
				return true;
			}
		}
		// データが足りなくておわった。
		return false;
	}
}
