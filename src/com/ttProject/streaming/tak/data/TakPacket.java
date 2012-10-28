package com.ttProject.streaming.tak.data;

import java.nio.ByteBuffer;

import com.ttProject.streaming.data.IMediaPacket;

/*
 * このクラスはそれぞれの情報を読み取るための手段をいれておく必要あり。
 * 先頭のflvのヘッダデータはまぁ適当にうっちゃけておく。
 * metaデータと動画および音声の第一パケットは保持しておく。(ただし、AVCとAACだけでいいかも。)
 * メディアパケットがあたらしく追記されたときは、ヘッダデータを作り直す必要あり。
 */
public abstract class TakPacket implements IMediaPacket {
	private final TakHeaderPacket headerPacket;
	public TakPacket(TakHeaderPacket headerPacket) {
		this.headerPacket = headerPacket; 
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
	private final byte META_TAG = 0x12;
	private final byte FLV_TAG = 0x46;
	@SuppressWarnings("unused")
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
			int position = buffer.position();
			Boolean result = null;
			// bufferの頭の部分を確認
			byte header = buffer.get();
			buffer.position(position);
			switch(header) {
			case AUDIO_TAG:
				// firstタグのみ、Headerへ
				System.out.println("audio");
				return false;
			case VIDEO_TAG:
				// firstタグのみ、Headerへ
				System.out.println("video");
				return false;
			case META_TAG:
				// 基本Header行
				System.out.println("meta");
				result = analizeMetaData(buffer);
				break;
			case FLV_TAG:
				// FLV_TAGデータ、これ以降のデータはHEADERパケットにはいるべき
				System.out.println("flv");
				result = analizeFlvHeader(buffer);
				break;
			default:
				throw new RuntimeException("解析できないデータがきました。");
			}
			if(result != null) {
				return result;
			}
		}
		return false;
	}
	private int getSizeFromHeader(byte[] header) {
		return ((header[1] << 16) + (header[2] << 8) + header[3]) & 0x00FFFFFF;
	}
	@SuppressWarnings("unused")
	private long getTimeFromHeader(byte[] header) {
		// とりいそぎ、ヘッダタイムを取得しなくてはいけない要素はないっぽい。
		return ((header[1] << 16) + (header[2] << 8) + header[3] + header[4] << 24) & 0xFFFFFFFF;
	}
	private Boolean analizeMetaData(ByteBuffer buffer) {
		if(buffer.remaining() < 11) {
			// header部分取得に満たない場合
			return false;
		}
		int position = buffer.position();
		// ヘッダ情報を取得
		byte[] header = new byte[11];
		buffer.get(header);
		// データサイズを確認する。
		int size = getSizeFromHeader(header);
		if(buffer.remaining() < size + 4) {
			// 十分な量のデータがない。
		}
		// データを取り出す
		return false;
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
		headerPacket.analize(ByteBuffer.wrap(FlvHeader));
		// MediaTagには書き込まない。
		return null;
	}
	
	
	
	
	
	@Override
	public void writeData() {
		System.out.println("データの書き込みを実行します。");
	}
}