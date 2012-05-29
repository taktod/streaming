package com.ttProject.streaming.httptakstreaming;

import java.nio.ByteBuffer;

/**
 * httpTakStreamingで利用するfth ftmデータを作成するクラス
 * @author taktod
 */
public class TakSegmentCreator {
	/**
	 * ftmファイルを作成する。
	 * @param buf ヘッダ情報となるbyteデータ
	 */
	public void writeTagData(ByteBuffer buf) {
		// ftmファイルを作成する。入力データはflv形式のおのおののパケット
	}
	/**
	 * fthファイルを作成する。
	 * @param buf 追加するbyteデータ
	 */
	public void writeHeaderPacket(ByteBuffer buf) {
	}
}
