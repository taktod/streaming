package com.ttProject.flazr;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.AudioTag;
import com.flazr.io.flv.FlvAtom;
import com.flazr.io.flv.VideoTag;
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
	private boolean audioSaved = false;
	private boolean videoSaved = false;
	
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
			audioSaved = false;
			videoSaved = false;
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
			final ChannelBuffer in = message.encode();
			while(in.readable()) {
				final FlvAtom flvAtom = new FlvAtom(in);
				final int absoluteTime = flvAtom.getHeader().getTime();
				channelTimes[primaryChannel] = absoluteTime;
				write(flvAtom); // 書き込む
			}
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
	private FlvAtom firstAudioAtom = null;
	/**
	 * 書き込みの実際の動作
	 * @param flvAtom
	 */
	private void write(final FlvAtom flvAtom) {
		// firstパケットは保持しておく必要あり。
		RtmpHeader header = flvAtom.getHeader();
		// 処理開始前判定
        if(startTime == -1) { // 処理開始前
            if(flvAtom.getHeader().isAudio()) {
            	if(!audioSaved) { // 初期データ確認済みかチェック
            		AudioTag tag = new AudioTag(flvAtom.getData().duplicate().readByte());
            		audioSaved = true;
            		if(tag.getCodecType() != AudioTag.CodecType.AAC) { // コーデックがAACでないならパス
            			return;
            		}
            		// AACの場合はそのまま書き込みデータ候補にする。(初期AACデータになります。)
            	}
            	else {
            		// 初期音声パケット情報を保持しておく。(第一パケットを音声パケットにするため)
            		firstAudioAtom = flvAtom;
            		return;
            	}
            }
            else if(flvAtom.getHeader().isVideo()) {
            	if(!videoSaved) { // 初期データ確認済みかチェック
            		VideoTag tag = new VideoTag(flvAtom.getData().duplicate().readByte());
            		videoSaved = true;
            		if(tag.getCodecType() != VideoTag.CodecType.AVC) { // コーデックがAVC(h.264)でないならパス
            			return;
            		}
            		// AVC(h.264)の場合はそのまま書き込みデータ候補にする。(初期H.264データになります。)
            	}
            	else {
            		// 興味があるのは、timestampが0以降で動画のキーフレームになっているデータなので、timestampが0の動画データはすべて捨てる
	            	if(header.getTime() == 0) {
	            		return;
	            	}
	            	VideoTag tag = new VideoTag(flvAtom.getData().duplicate().readByte());
	            	if(!tag.isKeyFrame()) { // キーフレームにあたるまでデータを捨て続ける。
	            		return;
	            	}
	            	// timestampが進みだした状態でキーフレームがきたので、処理開始
	            	ConvertManager convertManager = ConvertManager.getInstance();
	            	convertManager.startConvertThread(); // このタイミングでコンバートThreadを開始させる。
	            	// 音声パケットを最初にもってくる。
	            	if(firstAudioAtom != null) {
	            		header = firstAudioAtom.getHeader();
		            	startTime = header.getTime();
		            	header.setTime(0);
	        			convertManager.writeData(firstAudioAtom);

	        			header = flvAtom.getHeader();
	            	}
	            	else {
	            		// 音声パケットが存在しない場合は動画のみのFlvであると思われる、そのまま処理する。
		            	startTime = header.getTime();
	            	}
	        		header.setTime(header.getTime() - startTime);
            	}
            }
            else {
            	// audioでもvideoでもないタグはとりあえず捨てる。
            	return;
            }
        }
        else {
        	// 変換開始後はすべてのデータのtimestampをstartTime分ずらして生成をつづける。
    		header.setTime(header.getTime() - startTime);
        }
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
