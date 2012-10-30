package com.ttProject.streaming.tak.data;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * ヘッダー情報
 * @author taktod
 */
public class TakHeaderPacket extends TakPacket {
	/** TakPacketのgetBufferは追記しか頭にないので、このHeaderPacket用のデータは自力でなんとかします。 */
	private ByteBuffer buffer;
	/** Flvのヘッダデータ */
	private ByteBuffer flvHeader = null;
	/** 映像シーケンスヘッダ */
	private ByteBuffer videoSequenceHeader = null;
	/** 音声シーケンスヘッダ */
	private ByteBuffer audioSequenceHeader = null;
	/** 保存済みフラグ */
	private boolean isSaved = false;
	/**
	 * 保存済であるか応答する。
	 * @return true:保存済 false:未保存
	 */
	public boolean isSaved() {
		return isSaved;
	}
	/**
	 * ヘッダであるか確認
	 */
	@Override
	public boolean isHeader() {
		return true;
	}
	/**
	 * コンストラクタ
	 */
	public TakHeaderPacket() {
		super(null);
	}
	/**
	 * 解析
	 */
	@Override
	public boolean analize(ByteBuffer buffer) {
		// ここでは、videoタグとaudioタグしかとんでこない。
		byte type = buffer.get();
		buffer.rewind();
		switch(type) {
		case 0x09:
			// 映像タグ
			videoSequenceHeader = buffer;
			isSaved = false;
			System.out.print("video");
			break;
		case 0x08:
			// 音声タグ
			audioSequenceHeader = buffer;
			isSaved = false;
			System.out.print("audio");
			break;
		default:
			// データをうけとったら、Bufferにデータをいれておく。
			System.out.print("default");
			flvHeader = buffer;
			break;
		}
		// データをうけとったら、Bufferにデータをいれておく。
		System.out.println("sequenceHeaderをうけとった。");
		// 保持しているbufferを書き込んで再生成する。
		ByteBuffer data = ByteBuffer.allocate(
				flvHeader.limit() + 
				(videoSequenceHeader == null ? 0 : videoSequenceHeader.limit()) + 
				(audioSequenceHeader == null ? 0 : audioSequenceHeader.limit())
				);
		data.put(flvHeader);
		flvHeader.rewind(); // 読み込んだら巻き戻しておく。
		if(videoSequenceHeader != null) {
			data.put(videoSequenceHeader);
			videoSequenceHeader.rewind();
		}
		if(audioSequenceHeader != null) {
			data.put(audioSequenceHeader);
			audioSequenceHeader.rewind();
		}
		buffer = data;
		return true;
	}
	// 保存するときには、flvHeaderを書き込み後にvideoSequenceHeaderとaudioSequenceHeaderを書き込めばよい(この２つに関しては存在すれば書き込むことにする。)
	@Override
	public void writeData() {
		System.out.println("データの書き込みを実行します。");
		try {
			WritableByteChannel channel = Channels.newChannel(new FileOutputStream("/home/taktod/デスクトップ/xtest/mario.header.flv"));
			buffer.flip();
			channel.write(buffer);
		}
		catch (Exception e) {
		}
		finally {
			isSaved = true;
		}
	}
}
