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
//		while(buffer.remaining() > 0) {
			// まずヘッダを確認します。
			if(!isHeaderWrittern()) {
				// ヘッダがまだな場合
				int position = buffer.position();
				Long header = getEBMLId(buffer);
				if(header == null) {
					return false;
				}
				if(header == WebMPacketManager.ClusterId) {
	//				System.out.println("header is complete...");
					buffer.position(position);
					// クラスターIDを取得したので、headerデータは作成完了。
					return true;
				}
				byte[] saveData = getLastEBMLBytes();
				getBuffer(1024).put(saveData);
	//			System.out.println("header:" + Utils.toHex(saveData));
				// ヘッダーデータ完了。
				setIsHeaderWritten(true);
			}
			// 残り書き込みデータ量があるか確認
			if(getRemainData() == -1) {
				// 残り書き込みデータ量が確定していない
				// サイズデータを取得する。
				Long size = getEBMLSize(buffer);
				if(size == null) {
					return false;
				}
				byte[] saveData = getLastEBMLBytes();
				getBuffer(8).put(saveData);
	//			System.out.println("size:" + Utils.toHex(saveData));
	//			System.out.println("処理すべき大きさ:" + size.intValue());
				// 読み込むべきサイズデータを保存
				setRemainData(size.intValue()); // ここでサイズの扱いが-1になるので、次の部分にすすまなくなる。
			}
			while(buffer.remaining() > 0) {
				int size = getRemainData();
				if(size > 0) {
					// 残りデータがある場合は読み込んでいく。
					int saveSize = (size > buffer.remaining() ? buffer.remaining() : size);
					byte[] saveData = new byte[saveSize];
					buffer.get(saveData);
	//				System.out.println("書き込みデータ:" + Utils.toHex(saveData));
					getBuffer(saveSize).put(saveData);
					setRemainData(size - saveSize);
				}
				else {
					// 解析おわった。
					setIsHeaderWritten(false);
					resetRemainData();
					return false;
				}
			}
//		}
		return false;
	}
}
