package com.ttProject.streaming.webm.data;

import java.nio.ByteBuffer;

/**
 * webmストリーミングで利用するヘッダ情報用パケット
 * とりあえず分割して保存するだけなら、始めから取得→ClusterIDで次のデータでOK
 * 中抜きする場合は時間のところだけ切り詰め設定の部分をintにでもやっておけばいいだろう。
 * もうめんどくせぇ
 * @author taktod
 */
public class WebMHeaderPacket extends WebMPacket{
	// 本来はここで、header情報として、timecodeScaleを確認して、unitが何秒であるか確認したり、やっておきたいところ。
	// とりあえず、ヘッダーをつくるにあたって必要ないので、無視しておく。
	/*
	 * EBMLデータとSegmentId、TrackIDまでを含んでおきます。
	 */
	@Override
	public boolean analize(ByteBuffer buffer) {
		while(buffer.remaining() > 0) {
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
		return false;
	}
}
