package com.ttProject.streaming.hls.data;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.Set;

import com.ttProject.streaming.data.IMediaPacket;

/**
 * HttpLiveStreamingのパケットを解析するプログラム
 * http://www.arib.or.jp/english/html/overview/doc/2-STD-B10v4_7.pdf
 * @author taktod
 */
public abstract class HlsPacket implements IMediaPacket {
	/** PATのID */
	public static final int PATId = 0x0000;
	/** パケットの実データ保持 */
	private ByteBuffer buffer;
	private static Set<Integer> pmtIdSet = new HashSet<Integer>();
	/**
	 * pmtIDが確認済みか調べる。
	 * @return
	 */
	public static boolean isPmtChecked() {
		return pmtIdSet.size() > 0;
	}
	protected static boolean isPmtId(int pid) {
		return pmtIdSet.contains(pid);
	}
	private static void addPmtId(int pid) {
		pmtIdSet.add(pid);
	}
	private static Set<Integer> mediaPidSet = new HashSet<Integer>();
	private static Set<Integer> h264PidSet = new HashSet<Integer>();
	protected boolean isH264Pid(int pid) {
		return h264PidSet.contains(pid);
	}
	/**
	 * バッファデータを参照する。
	 * @param size 要求されているサイズ
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
		buffer.flip();
		newBuffer.put(buffer);
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
	// 解析に必要な情報をあつめることができるようにしておく。
	protected int getPid(ByteBuffer buffer) {
		// なにかあったときに巻き戻すための位置取得
		int position = buffer.position();
		// 先頭を確認
		if(buffer.get() != 0x47) {
			throw new RuntimeException("先頭が0x47になっていないとmpegtsとして成立していない。");
		}
		// pid取得
		int pid = buffer.getShort() & 0x1FFF;
		// buffer巻き戻し
		buffer.position(position);
		return pid;
	}
	/** 特に使わないであろうデータたち sizeはつかうか・・・ */
	@SuppressWarnings("unused")
	private int type; // 識別子
	private int size; // データ長
	@SuppressWarnings("unused")
	private int versionNumber;
	@SuppressWarnings("unused")
	private byte currentNextOrder;
	@SuppressWarnings("unused")
	private int sectionNumber;
	@SuppressWarnings("unused")
	private int lastSectionNumber;
	/**
	 * たいていのフレームがもってる
	 * @return
	 */
	private boolean analizeHeader(ByteBuffer buffer, byte tableSignature) {
		if(buffer.get() != tableSignature) {
			// テーブルシグネチャが合いません。
			return false;
		}
		int data = buffer.getShort() & 0xFFFF;
		if(data >>> 12 != Integer.parseInt("1011", 2)) {
			// セクションシンタクス指示が一致しません。
			return false;
		}
		size = data &0x0FFF;
		type = buffer.getShort() & 0xFFFF;
		data = buffer.get() & 0xFF;
		if(data >>> 6 != Integer.parseInt("11", 2)) {
			// 形式がおかしい。
			return false;
		}
		versionNumber = (data & 0x3F) >>> 1;
		currentNextOrder = (byte)(data & 0x01);
		sectionNumber = buffer.get() & 0xFF;
		lastSectionNumber = buffer.get() & 0xFF;
		size -= 5;
		return true;
	}
	// patとして解析してみる。
	protected boolean analizePat(ByteBuffer buffer) {
		int position = buffer.position();
		buffer.position(position + 5); // 5すすめる。
		if(!analizeHeader(buffer, (byte)0x00)) {
			buffer.position(position);
			throw new RuntimeException("ヘッダ部読み込み時に不正なデータを検出しました。");
		}
		// ディテール読み込み
		while(size > 4) {
			size -= 4;
			// 放送番組識別 16bit
			// 111 3bit (固定)
			// PIDデータ 13bit
			int data = buffer.getInt(); // ４倍と読み込む
			if((data & 0xF000) >>> 13 != Integer.parseInt("111", 2)) {
				// 固定bitが一致しない。
				buffer.position(position);
				throw new RuntimeException("ビットフラグが一致しない。");
			}
			if(data >>> 16 != 0) {
				// PMT PID
				// pmtなので、保持させる。
				addPmtId(data & 0x1FFF);
			}
			else {
				// ネットワークPID
			}
		}
		// bufferの位置を戻しておく。
		buffer.position(position);
 		return true;
	}
	// pmtとして解析してみる。
	protected boolean analizePmt(ByteBuffer buffer) {
		int position = buffer.position();
		buffer.position(position + 5); // 5すすめる。

		if(!analizeHeader(buffer, (byte)0x02)) {
			buffer.position(position);
			throw new RuntimeException("ヘッダ部の読み込み時に不正なデータを検出しました。");
		}
		int data;
		// 111 3bit(固定)
		// PCR_PID 13ビット 時刻主体になるパケット情報(とりあえず必要ない。)
		data = buffer.getShort() & 0xFFFF;
		if(data >>> 13 != Integer.parseInt("111", 2)) {
			buffer.position(position);
			throw new RuntimeException("PCRPID用の指示ビットがおかしいです。");
		}
		// 1111 4bit(固定)
		// 番組情報長 12bit
		data = buffer.getShort() & 0xFFFF;
		if(data >>> 12 != Integer.parseInt("1111", 2)) {
			buffer.position(position);
			throw new RuntimeException("番組情報長用の指示ビットがおかしいです。");
		}
		int skipLength = data & 0x0FFF;
		// 番組情報
		int pos = buffer.position();
		buffer.position(pos + skipLength);
		size -= (4 + skipLength);
		// 以降ストリームデータ
		while(size > 4) {
			// ストリーム形式 8bit
			byte type = buffer.get();
			// 111 3bit
			// エレメンタリーPID 13bit
			data = buffer.getShort() & 0xFFFF;
			if(data >>> 13 != Integer.parseInt("111", 2)) {
				buffer.position(position);
				throw new RuntimeException("エレメンタリーID用の指示ビットがおかしいです。");
			}
			int pid = data & 0x1FFF;
			if(type == 0x1B) {
				// h.264用のトラック
				h264PidSet.add(pid);
			}
			mediaPidSet.add(pid);
			// 1111 4bit
			// ES情報長 12bit
			data = buffer.getShort() & 0xFFFF;
			if(data >>> 12 != Integer.parseInt("1111", 2)) {
				buffer.position(position);
				throw new RuntimeException("ES情報長用の指示ビットがおかしいです。");
			}
			skipLength = data & 0x0FFF;
			// 任意
			pos = buffer.position();
			buffer.position(pos + skipLength);
			size -= (5 + skipLength);
		}
		// ポジションを戻しておく。
		buffer.position(position);
		return true;
	}
	
	
	private static int num = 0;
	/**
	 * とりあえずテスト
	 */
	@Override
	public void writeData() {
		System.out.println("書き込みを実行します。" + buffer.position());
		try {
			WritableByteChannel channel = Channels.newChannel(new FileOutputStream("/home/xxxx/download/test640." + num + ".ts"));
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
