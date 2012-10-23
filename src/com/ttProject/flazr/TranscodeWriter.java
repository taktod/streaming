package com.ttProject.flazr;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.flazr.rtmp.RtmpHeader;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpWriter;
import com.ttProject.xuggle.ConvertManager;

/**
 * 受け取ったデータをxuggleに送り込むRtmpWriter(flv形式で保存されていきます。)
 * @author taktod
 * 
 * httpTakStreamingはコンバートなしでも可能なので、queueにかならずしも入れる必要はない。
 */
public class TranscodeWriter implements RtmpWriter {
	/** ロガー */
	private static final Logger logger = LoggerFactory.getLogger(TranscodeWriter.class);
	
	/** 各チャンネルの時刻保持 */
	private final int[] channelTimes = new int[RtmpHeader.MAX_CHANNEL_ID];
	/** 主チャンネル保持 */
	private int primaryChannel = -1;

	/** 動作名 */
	private final String name;

	/** 開始時の動作タイムスタンプ */
	private int startTime = -1;
	
	/**
	 * コンストラクタ
	 */
	public TranscodeWriter(String name) {
		this.name = name;
	}
	/**
	 * 放送が開始したときの動作
	 */
	public void onPublish() {
		// 前の動作がのこっているときには、いったん停止する。
		close();
		
		// 初期化する。
		initialize();
	}
	/**
	 * 放送が停止したときの動作
	 */
	public void onUnpublish() {
		logger.info("unpublishがきました。");
		close();
	}
	/**
	 * 初期化
	 */
	public void initialize() {
		try {
			ConvertManager convertManager = ConvertManager.getInstance();
			convertManager.initialize(name);
			startTime = -1;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 書き込み動作呼び出し
	 */
	@Override
	public void write(RtmpMessage message) {
		final RtmpHeader header = message.getHeader();
		if(header.isAggregate()) {
/*			final ChannelBuffer in = message.encode();
			while(in.readable()) {
				final FlvAtom flvAtom = new FlvAtom(in);
				final int absoluteTime = flvAtom.getHeader().getTime();
				channelTimes[primaryChannel] = absoluteTime;
				write(flvAtom); // 書き込む
			}// */
			int difference = -1;
			final ChannelBuffer in = message.encode();
			while(in.readable()) {
				final FlvAtom flvAtom = new FlvAtom(in);
				final RtmpHeader subHeader = flvAtom.getHeader();
				if(difference == -1) {
					difference = subHeader.getTime() - header.getTime();
				}
				final int absoluteTime = flvAtom.getHeader().getTime();
				channelTimes[primaryChannel] = absoluteTime;
				subHeader.setTime(subHeader.getTime() - difference);
				write(flvAtom); // 書き込む
			}// */
		}
		else { // metadata audio videoの場合
			final int channelId = header.getChannelId();
			channelTimes[channelId] = header.getTime();
            if(primaryChannel == -1 && (header.isAudio() || header.isVideo())) {
            	// 先に見つけたデータをprimaryデータとして扱う。？
                primaryChannel = channelId;
            }
            if(header.getSize() <= 2) { // サイズが小さすぎる場合は不正な命令として無視する？
            	return;
            }
            write(new FlvAtom(header.getMessageType(), channelTimes[channelId], message.encode()));
		}
	}
	/**
	 * 書き込みの実際の動作
	 * @param flvAtom
	 */
	private void write(final FlvAtom flvAtom) {
		// firstパケットは保持しておく必要あり。
		RtmpHeader header = flvAtom.getHeader();
		if(startTime == -1) {
			startTime = header.getTime();
		}
		// 内部の時間設定をずらしておきます。
		header.setTime(header.getTime() - startTime);
		// このコンバートにそった状態になったところで、コンバート管理にデータをまわす。
		// 管理側でflvの情報を抜き出したりすると思うのでまだflazrの影響下においておく。
		ConvertManager convertManager = ConvertManager.getInstance();
		convertManager.writeData(flvAtom);
	}
	/**
	 * 終了動作呼び出し
	 */
	@Override
	public void close() {
	}
}
