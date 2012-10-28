package com.ttProject.xuggle.flv;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;

/**
 * 自力でflvのIPacketデータをつくるためのHandler
 * @author taktod
 * 
 * このクラスでは、flvDataQueueのdataQueueForReaderのデータを読み取って、
 * FlvManagerのexeute処理の冒頭でcontainerからreadNextPacketを実行する代わりに
 * IPacketの生成を実行するためのクラスです。
 */
public class FlvCustomReader {
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(FlvCustomReader.class);
	/** 処理済みデータ量記録 */
	private long passedData;
	/** 入力データqueue */
	private final FlvDataQueue inputDataQueue;
	/** 動作コンテナ */
	private IContainer inputContainer;
	/**
	 * コンストラクタ
	 */
	public FlvCustomReader(FlvDataQueue inputDataQueue) {
		// flvDataQueueからデータを読み込めるようにする。
		passedData = 0; // 処理済みデータ量
		this.inputDataQueue = inputDataQueue;
	}
	/**
	 * コンテナの登録
	 * @param inputContainer
	 */
	public void setInputContainer(IContainer inputContainer) {
		this.inputContainer = inputContainer;
	}
	/**
	 * 次のパケットの読み込みを実行します。
	 * @return true:取得できた。 false:取得できなかった。
	 */
	public boolean readNextPacket(IPacket packet) {
		// inputDataQueueからデータを読み込みます。
		while(true) {
			// inputDataQueueがflvHandlerがpollでうごいてるけどtakeにした方がいいかも・・・
			ByteBuffer data = inputDataQueue.readForReader();
			if(data == null) {
				// データが枯渇したので、次にいく。
				return false;
			}
			if(data.remaining() == 13) {
				// 13バイト保持しているデータは通常header以外ありえない。
				// とりあえず、データがflvheaderであるか確認する。
				if(!checkHeader(data)) {
					throw new RuntimeException("headerのデータがうまく取得できませんでした。");
				}
			}
			else {
				if(analizeData(packet, data)) {
					return true;
				}
			}
		}
	}
	/**
	 * ヘッダデータであるか確認する。
	 * @param header
	 * @return true:headerデータである。 false:headerデータではない。
	 */
	private boolean checkHeader(ByteBuffer header) {
		byte[] data = new byte[13];
		header.get(data);
		if(data[0] != 0x46
		|| data[1] != 0x4C
		|| data[2] != 0x56
		|| data[3] != 0x01) { // とりあえず、先頭だけ確認しておく。
			return false;
		}
		passedData += 13;
		return true;
	}
	/**
	 * データの内容を調査Packetの内容を正しいものにしておく。
	 * @param packet
	 * @param buffer
	 * @return true:調査した結果動作を完了した。 false:調査結果動作が完了していない。
	 */
	private boolean analizeData(IPacket packet, ByteBuffer buffer) {
		byte[] data = new byte[11]; // 11バイト読み込む
		buffer.get(data);
		passedData += 11;
		int size = ((data[1] & 0xFF) << 16) + ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
		int timestamp = ((data[4] & 0xFF) << 16) + ((data[5] & 0xFF) << 8) + (data[6] & 0xFF) + ((data[7] & 0xFF) << 24);
		switch(data[0]) {
		case 0x08: // 音声パケット
			readAudioPacket(packet, buffer, size, timestamp);
			break;
		case 0x09: // 映像パケット
			readVideoPacket(packet, buffer, size, timestamp);
			break;
		default: // その他(とりあえず無視します。)
			passedData += size + 4;
			return false; // 他にデータがあるなら、そっちの処理をする。
		}
		return true;
	}
	/**
	 * 動画パケットを解析し、packetに結びつけておきます。
	 * 結びつけ方法はxuggleの動作を見ながら状況解析で実装したので、まちがってるかもしれない。
	 * @param packet
	 * @param buffer
	 * @param size
	 * @param timestamp
	 */
	private void readVideoPacket(IPacket packet, ByteBuffer buffer, int size, int timestamp) {
		byte data = buffer.get();
		switch((data & 0x0F)) {
		case 1: // Jpeg
		case 2: // H263
		case 3: // Screen
		case 4: // On2Vp6
		case 5: // On2Vp6_Alpha
		case 6: // Screen v2
//		case 7: // AVC
			passedData += 1;
			size -= 1;
			break;
		case 7: // AVC
			buffer.getInt();
			passedData += 5;
			size -= 5;
			break;
		default:
			throw new RuntimeException("判定できないコーデックのデータをみつけました。");
		}
		byte[] mediaData = new byte[size];
		buffer.get(mediaData);
		IBuffer bufData = IBuffer.make(inputContainer, mediaData, 0, size);
		packet.setData(bufData);
		if((data & 0x10) != 0x00) {
			packet.setFlags(1);
			packet.setKeyPacket(true);
		}
		else {
			packet.setFlags(0);
			packet.setKeyPacket(false);
		}
		packet.setPosition(passedData);
		packet.setComplete(true, size);
		packet.setStreamIndex(0);
		packet.setDts(timestamp);
		packet.setPts(timestamp);
		packet.setTimeBase(IRational.make(1, 1000));
		passedData += size + 4;
	}
	/**
	 * 音声パケットを解析し、packetにデータを結びつけておきます。
	 * 結びつけ方法はxuggleの動作を見ながら状況解析で実装したので、まちがってるかもしれない。
	 * @param packet
	 * @param buffer
	 * @param size
	 * @param timestamp
	 */
	private void readAudioPacket(IPacket packet, ByteBuffer buffer, int size, int timestamp) {
		byte data = buffer.get();
		switch((data & 0xF0) >>> 4) {
		case 0: // Raw PCM (osflashのflv記述より)
		case 1: // Adpcm
		case 2: // Mp3
		case 3: // Pcm
		case 4: // Nelly_16
		case 5: // Nelly_8
		case 6: // Nelly
		case 7: // G711_a
		case 8: // G711_u
		case 9: // reserved
//		case 10: // AAC
		case 11: // Speex
		case 12: // unknown
		case 13: // unknown
		case 14: // Mp3_8
		case 15: // DeviceSpecific
			passedData += 1;
			size -= 1;
			break;
		case 10: // AAC
			buffer.get();
			passedData += 2;
			size -= 2;
			break;
		default:
			throw new RuntimeException("判定できないコーデックのデータをみつけました。");
		}
		byte[] mediaData = new byte[size];
		buffer.get(mediaData);
		IBuffer bufData = IBuffer.make(inputContainer, mediaData, 0, size);
		packet.setData(bufData);
		packet.setPosition(passedData);
		packet.setComplete(true, size);
		packet.setKeyPacket(true);
		packet.setFlags(1);
		packet.setStreamIndex(1);
		packet.setDts(timestamp);
		packet.setPts(timestamp);
		packet.setTimeBase(IRational.make(1, 1000));
		passedData += size + 4;
	}
	/**
	 * 閉じます。
	 */
	public void close() {
	}
}
