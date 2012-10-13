package com.ttProject.streaming.webm.data;

import java.nio.ByteBuffer;

/**
 * webmストリーミングで利用するメディアパケット
 * @author taktod
 */
public class WebMMediaPacket extends WebMPacket {
	// 先頭はClusterIDであるはず。
	private long currentHeader = 0x00L;
	public static int counter = 0;
	/**
	 * おわったから終わった分とデータがたりなくておわったものと２つある。
	 */
	@Override
	public boolean analize(ByteBuffer buffer) {
		while(buffer.remaining() > 0) {
			// まずヘッダを確認します。
			if(!isHeaderWrittern()) {
				// ヘッダがまだな場合
				int position = buffer.position();
				Long header = getEBMLId(buffer);
				if(header == null) {
					// 足りない
					return false;
				}
				currentHeader = header;
//				System.out.println("header:" + Long.toHexString(currentHeader));
				if(header == WebMPacketManager.ClusterId && getBufferSize() != 0) {
					counter ++;
					System.out.println("clusterIdCounter:" + counter);
					buffer.position(position);
					// クラスターIDを取得したので、headerデータは作成完了。
					// おわった
					return true;
				}
				byte[] saveData = getLastEBMLBytes();
				getBuffer(1024).put(saveData);
				// ヘッダーデータ完了。
				setIsHeaderWritten(true);
			}
			// 残り書き込みデータ量があるか確認
			if(getRemainData() == -1) {
				// 残り書き込みデータ量が確定していない
				// サイズデータを取得する。
				Long size = getEBMLSize(buffer);
				if(size == null) {
					// 足りない
					return false;
				}
				byte[] saveData;
				if(currentHeader == WebMPacketManager.TimecodeId) {
					// 88をhexの応答にしておく。
					saveData = new byte[1];
					saveData[0] = (byte)0x88; // longでいれたい
				}
				else {
					saveData = getLastEBMLBytes();
				}
				getBuffer(8).put(saveData);
				// 読み込むべきサイズデータを保存
				setRemainData(size.intValue()); // ここでサイズの扱いが-1になるので、次の部分にすすまなくなる。
			}
			
			if(currentHeader == WebMPacketManager.TimecodeId) {
				// データ量があるか確認
				if(buffer.remaining() > getRemainData()) {
					// 必要なデータ量があったら、取得する
					long data = 0;
					byte[] targetData = new byte[getRemainData()];
					buffer.get(targetData);
					for(byte b : targetData) {
						data = data * 0x0100 + (b & 0xFF);
					}
//					System.out.println("timeCodeのhex確認:" + Long.toHexString(data));
					getBuffer(8).putLong(data);
//					System.out.println("知りたいサイズ:" + data);
					// データを書き込む必要あり。
					// 解析おわった。
					setIsHeaderWritten(false);
					resetRemainData();
					// おわった。
					continue;
				}
				System.out.println("timecodeIdの処理でバッファがたりない。");
				break;
			}
			else {
				while(buffer.remaining() > 0 && isHeaderWrittern()) {
					int size = getRemainData();
					if(size > 0) {
						// 残りデータがある場合は読み込んでいく。
						int saveSize = (size > buffer.remaining() ? buffer.remaining() : size);
						byte[] saveData = new byte[saveSize];
						buffer.get(saveData);
						getBuffer(saveSize).put(saveData);
						setRemainData(size - saveSize);
					}
					else {
						// 解析おわった。
						setIsHeaderWritten(false);
						resetRemainData();
						// 終わった
						continue;
					}
				}
				if(!isHeaderWrittern()) {
					// bufferデータが存在しているのに、headerの書き込みがある場合は処置しないといけない。
					continue;
				}
			}
			break;
		}
		// 足りない
		return false;
	}
}
