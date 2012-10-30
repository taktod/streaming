package com.ttProject.streaming.tak.data;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import com.ttProject.streaming.data.IMediaPacket;

/*
 * このクラスはそれぞれの情報を読み取るための手段をいれておく必要あり。
 * 先頭のflvのヘッダデータはまぁ適当にうっちゃけておく。
 * metaデータと動画および音声の第一パケットは保持しておく。(ただし、AVCとAACだけでいいかも。)
 * メディアパケットがあたらしく追記されたときは、ヘッダデータを作り直す必要あり。
 */
public abstract class TakPacket implements IMediaPacket {
	private final TakHeaderPacket headerPacket;
//	private int timePassed = 0; // 取得秒数
	public TakPacket(TakHeaderPacket headerPacket) {
		this.headerPacket = headerPacket; 
//		timePassed = 0;
	}
	/** パケットの実データ保持 */
	private ByteBuffer buffer = null;
	protected ByteBuffer getBuffer(int size) {
		if(buffer == null)  {
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
	/** ヘッダまわり */
	private boolean isHeaderWritten;
	protected void setIsHeaderWritten(boolean flg) {
		isHeaderWritten = flg;
	}
	protected boolean isHeaderWritten() {
		return isHeaderWritten;
	}
	/** 残り書き込みデータ量 */
	private int remainData = -1; // 残りデータ量
	protected void setRemainData(int data) {
		remainData = data;
	}
	protected int getRemainData() {
		return remainData;
	}
	protected void resetRemainData() {
		remainData = -1;
	}
	private final byte AUDIO_TAG = 0x08;
	private final byte VIDEO_TAG = 0x09;
	private final byte META_TAG  = 0x12;
	private final byte FLV_TAG   = 0x46;
	private byte[] FlvHeader = {0x46, 0x4C, 0x56, // flv
			0x01, // version
			0x05, /* 1:video + 4:audio */
			0x00, 0x00, 0x00, 0x09,
			0x00, 0x00, 0x00, 0x00};
	/**
	 * 内容を解析します。
	 * @return
	 */
	public boolean analize(ByteBuffer buffer) {
		while(buffer.remaining() > 0) {
			// タグの解析がきちんとできたら、次にいく。
			// できなかったら巻き戻して応答を返し、再度やりなおし。
			int position = buffer.position();
			Boolean result = null;
			// bufferの頭の部分を確認
			byte header = buffer.get();
			buffer.position(position);
			switch(header) {
			case AUDIO_TAG:
				// firstタグのみ、Headerへ
//				System.out.println("audio");
				result = analizeAudioData(buffer);
				break;
			case VIDEO_TAG:
				// firstタグのみ、Headerへ
//				System.out.println("video");
				result = analizeVideoData(buffer);
				break;
			case META_TAG:
				// 基本Header行き(とりいそぎ、無視する方向で実行します。)
//				System.out.println("meta");
				result = analizeMetaData(buffer);
				break;
			case FLV_TAG:
				// FLV_TAGデータ、これ以降のデータはHEADERパケットにはいるべき
//				System.out.println("flv");
				result = analizeFlvHeader(buffer);
				break;
			default:
				throw new RuntimeException("解析できないデータがきました。");
			}
			if(result != null) {
				// 読み込み位置を元に戻す
				buffer.position(position);
				return result;
			}
		}
		return false;
	}
	private int getSizeFromHeader(byte[] header) {
		return (((header[1] & 0xFF) << 16) + ((header[2] & 0xFF) << 8) + (header[3] & 0xFF));
	}
	@SuppressWarnings("unused")
	private long getTimeFromHeader(byte[] header) {
		// とりいそぎ、ヘッダタイムを取得しなくてはいけない要素はないっぽい。
		return (((header[4] & 0xFF) << 16) + ((header[5] & 0xFF) << 8) + (header[6] & 0xFF) + ((header[7] & 0xFF) << 24));
	}
	/**
	 * オーディオデータを解析する。
	 * @param buffer
	 * @return
	 */
	private Boolean analizeAudioData(ByteBuffer buffer) {
		if(buffer.remaining() < 11) {
			// header部分取得に満たない場合
			return false;
		}
		// ヘッダ情報を取得
		byte[] header = new byte[11];
		buffer.get(header);
		// データサイズを確認する。
		int size = getSizeFromHeader(header);
		if(buffer.remaining() < size + 4) {
			// 十分な量のデータがない。
			return false;
		}
		// データを取り出す
		byte[] body = new byte[size];
		buffer.get(body);
		// 4byte終端データを確認する。
		byte[] tail = new byte[4];
		buffer.get(tail);
		// コーデック確認
		switch((body[0] & 0xF0) >>> 4) {
		case 10: // AAC
			// AACなら次のパケットを確認して、headerであるか確認する。
			if(body[1] == 0x00) {
				// headerだった
				ByteBuffer sequenceHeader = ByteBuffer.allocate(size + 4 + 11);
				sequenceHeader.put(header);
				sequenceHeader.put(body);
				sequenceHeader.put(tail);
				sequenceHeader.flip();
				headerPacket.analize(sequenceHeader);
			}
			break;
		default: // その他
			break;
		}
		// シーケンスヘッダも書き込んでおく。(書き込んでおかないと、中途で変更があったときに困る。)
		ByteBuffer saveBuffer = getBuffer(size+ 4 + 11);
		saveBuffer.put(header);
		saveBuffer.put(body);
		saveBuffer.put(tail);
		return null;
	}
	private Boolean analizeVideoData(ByteBuffer buffer) {
		if(buffer.remaining() < 11) {
			// header部分取得に満たない場合
			return false;
		}
		// ヘッダ情報を取得
		byte[] header = new byte[11];
		buffer.get(header);
		// データサイズを確認する。
		int size = getSizeFromHeader(header);
		if(buffer.remaining() < size + 4) {
			// 十分な量のデータがない。
			return false;
		}
		// データを取り出す
		byte[] body = new byte[size];
		// データの先頭1文字目を確認することでコーデック情報とかがわかります。
		// 0xF0でキーフレームかどうか判定できる。
		// 0x0Fで、フレームタイプがわかる。
		// H.264の場合は、先頭の4バイトは、拡張データになります。
		// TODO 本当にそうなっているか確認しなければいけない。0:AVCのヘッダデータ
		buffer.get(body);
		// 4byte終端データを確認する。
		byte[] tail = new byte[4];
		buffer.get(tail);
		boolean isSequenceHeader = false;
		// コーデック確認
		switch((body[0] & 0x0F)) {
		case 7: // AVC
			// AVCなら次のパケットを確認して、headerであるか確認する。
			if((body[0] & 0x10) == 0x10 && body[1] == 0x00) {
				// headerだった
				ByteBuffer sequenceHeader = ByteBuffer.allocate(size + 4 + 11);
				sequenceHeader.put(header);
				sequenceHeader.put(body);
				sequenceHeader.put(tail);
				sequenceHeader.flip();
				headerPacket.analize(sequenceHeader);
				isSequenceHeader = true;
			}
			break;
		default: // その他
			break;
		}
		// sequenceデータではなく
		// キーフレームだった場合はパケットの境目と判定しなければいけない。
		if((body[0] & 0x10) == 0x10 && !isSequenceHeader) {
			if(getBufferSize() != 0) {
				// バッファサイズがたまっている場合は、終端がきたことになるので、分割する。
				System.out.println("分割ポイントがきました。");
				return true;
			}
		}
		// ここでバッファにカキコする。
		ByteBuffer saveBuffer = getBuffer(size+ 4 + 11);
		saveBuffer.put(header);
		saveBuffer.put(body);
		saveBuffer.put(tail);
		return null;
	}
	/**
	 * metaタグ解析動作
	 * @param buffer 解析するバッファ(flvデータの先頭であることを期待します。)
	 * @return true:今回のパケットの解析が完了した。false:中途でデータがたりなくなった。null:解析は完了し、次のデータを解析する必要がある場合
	 */
	private Boolean analizeMetaData(ByteBuffer buffer) {
		if(buffer.remaining() < 11) {
			// header部分取得に満たない場合
			return false;
		}
		// ヘッダ情報を取得
		byte[] header = new byte[11];
		buffer.get(header);
		// データサイズを確認する。
		int size = getSizeFromHeader(header);
		if(buffer.remaining() < size + 4) {
			// 十分な量のデータがない。
			return false;
		}
		// データを取り出す
		byte[] body = new byte[size];
		// metaデータは特に興味ないので、すてておく。
		buffer.get(body);
		// 4byte終端データを確認する。
		byte[] tail = new byte[4];
		buffer.get(tail);
		return null;
	}
	/**
	 * Flvのヘッダであるか確認します。
	 * @return
	 */
	private Boolean analizeFlvHeader(ByteBuffer buffer) {
		// バッファにデータがきちんと存在しているか確認
		if(buffer.remaining() < 13) {
			// 足りない。
			return false;
		}
		byte[] data = new byte[13];
		buffer.get(data);
		for(int i = 0;i < 13;i ++) {
			if(i != 4) {
				if(data[i] != FlvHeader[i]) {
					throw new RuntimeException("flvHeaderデータが不正です。");
				}
			}
			else {
				if(data[i] != 1 && data[i] != 4 && data[i] != 5) {
					throw new RuntimeException("flvHeaderデータのメディア指定が不正です。");
				}
			}
		}
		ByteBuffer headerBuffer = ByteBuffer.allocate(FlvHeader.length);
		headerBuffer.put(FlvHeader);
		headerBuffer.flip();
		headerPacket.analize(headerBuffer);
		// MediaTagには書き込まない。
		return null;
	}

	private static int num = 0;
	@Override
	public void writeData() {
		System.out.println("データの書き込みを実行します。");
		try {
			WritableByteChannel channel = Channels.newChannel(new FileOutputStream("/home/taktod/デスクトップ/xtest/mario." + num + ".flv"));
			buffer.flip();
			channel.write(buffer);
		}
		catch (Exception e) {
			// TODO: handle exception
		}
		finally {
			num ++;
		}
	}
}
