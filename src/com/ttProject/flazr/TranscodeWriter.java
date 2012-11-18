package com.ttProject.flazr;

import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.Queue;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.AudioTag;
import com.flazr.io.flv.FlvAtom;
import com.flazr.io.flv.VideoTag;
import com.flazr.rtmp.RtmpHeader;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpWriter;
import com.ttProject.process.ConvertProcessHandler;

/**
 * 変換用のwriter動作
 * @author taktod
 */
public class TranscodeWriter implements RtmpWriter {
	/** ロガー */
	private static final Logger logger = LoggerFactory.getLogger(TranscodeWriter.class);
	private final int[] channelTimes = new int[RtmpHeader.MAX_CHANNEL_ID];
	private int primaryChannel = -1;
	private final String name;

	// コンバートを開始したときの先頭の時刻保持(この時刻分だけデータがずれます。)
	private int startTime = -1; // 出力開始timestamp
	private int playTime = -1; // データ取得開始時刻
	private boolean isPlaying = false;
	
	// コンバート中のコーデック情報を保持しておく。
	private CodecType videoCodec = null;
	private CodecType audioCodec = null;

	// コンバート処理のハンドラー
	private ConvertProcessHandler convertHandler = null;

	// 最終処理時刻(処理がなくなったと判定するのに必要)
	private long lastAccessTime = -1;
	// audioデータのqueue、整列させるのに利用
	private final Queue<FlvAtom> audioAtomQueue = new LinkedList<FlvAtom>();
	private FlvAtom lastAudioAtom = null;

	// sequenceHeaderがあるコーデック用のデータ
	private FlvAtom aacMediaSequenceHeader = null;
	private FlvAtom avcMediaSequenceHeader = null;

	// ファイル書き込み用(本当はいらない)
	private FileChannel writeChannel = null;

	// コーデック情報定義
	public static enum CodecType {
		NONE,
		JPEG,H263,SCREEN,ON2VP6,ON2VP6_ALPHA,SCREEN_V2,AVC,
		ADPCM,MP3,PCM,NELLY_16,NELLY_8,NELLY,G711_A,G711_U,RESERVED,AAC,SPEEX,MP3_8,DEVICE_SPECIFIC
	};
	/**
	 * コンストラクタ
	 * @param name
	 */
	public TranscodeWriter(String name) {
		this.name = name;
		// 監視スレッドをつくっておいて、２秒間データがこなかったらとまったと判定する。
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(true) {
						if(isPlaying) {
							// 最終アクセスを確認し、最終アクセス時刻から1秒以上たっていたら、ストリームがなんらかの原因でとまったと推測する。
							if(System.currentTimeMillis() - lastAccessTime > 3000) {
								logger.info("アクセスが3秒強ないので、とまったと判定しました。");
								stop();
							}
						}
						Thread.sleep(1000);
					}
				}
				catch (Exception e) {
					logger.error("変換の監視処理で例外発生", e);
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}
	/**
	 * unpublishを検知した場合
	 */
	public void onUnpublish() {
		stop();
	}
	/**
	 * データのダウンロードがおわったときの処理
	 */
	private void stop() {
		logger.info("止めます。");
		startTime = -1;
		playTime = -1;
		isPlaying = false;
		videoCodec = null;
		audioCodec = null;
		lastAudioAtom = null;
		audioAtomQueue.clear();
		
		/** 以下のファイル書き込み処理は本来はいらない。socketで通信して、別のプロセスに渡せばよい */
		try {
			if(writeChannel != null) {
				writeChannel.close();
				writeChannel = null;
			}
		}
		catch (Exception e) {
			logger.error("ファイルのクローズに失敗", e);
		}
		if(convertHandler != null) {
			convertHandler.close();
			convertHandler = null;
		}
	}
	/**
	 * データのダウンロードがはじまったときの処理
	 */
	private void start(RtmpHeader header) {
		logger.info("始めます。");
		// ファイルの書き込みチャンネルを開いてとりあえず、書き込みテストを実行します。
		/** 以下のファイルオープン処理は本来必要ない。 */
		try {
			writeChannel = new FileOutputStream(name + "_" + System.currentTimeMillis() + ".flv").getChannel();
			writeChannel.write(FlvAtom.flvHeader().toByteBuffer());
		}
		catch (Exception e) {
			logger.error("ファイルのオープンに失敗", e);
		}
		/** ここまでいらない */
		isPlaying = true;
		startTime = header.getTime();
		// このタイミングでprocessサーバーとかを作成する。
		convertHandler = new ConvertProcessHandler(audioCodec != CodecType.NONE, videoCodec != CodecType.NONE, name);
		convertHandler.getFlvDataQueue().putData(FlvAtom.flvHeader());
		// mediaSequenceHeaderがあるコーデックの場合は情報を書き込む
		if(audioCodec == CodecType.AAC && aacMediaSequenceHeader != null) {
			aacMediaSequenceHeader.getHeader().setTime(0);
			write(aacMediaSequenceHeader);
		}
		if(videoCodec == CodecType.AVC && avcMediaSequenceHeader != null) {
			avcMediaSequenceHeader.getHeader().setTime(0);
			write(avcMediaSequenceHeader);
		}
		if(videoCodec != CodecType.NONE) {
			// 動画の場合は開始header以前にあるデータは必要ないので、音声queueからデータを削除します。
			FlvAtom audioAtom = lastAudioAtom;
			lastAudioAtom = null;
			do {
				if(audioAtom == null) {
					audioAtom = audioAtomQueue.poll();
				}
				if(audioAtom == null) {
					// queueにすでにデータがなければ、抜けます。
					break; // ループをぬける。
				}
				if(audioAtom.getHeader().getTime() > header.getTime()) {
					// audioAtomのデータが現状のvideoのデータより後のデータである場合
					lastAudioAtom = audioAtom;
					break;
				}
				// audioAtomのデータが現状のvideoのデータより前のデータである場合
				audioAtom = null; // 破棄して次のループに進む。
			} while(true);
		}
		else {
			// 音声データのみの場合はaudioQueueは必要ないので破棄します。
			audioAtomQueue.clear();
		}
	}
	/**
	 * 締め処理
	 */
	@Override
	public void close() {
		stop();
	}
	/**
	 * 書き込み処理(主体)
	 */
	@Override
	public void write(RtmpMessage message) {
		final RtmpHeader header = message.getHeader();
		if(header.isAggregate()) { // aggregate
/*			final ChannelBuffer in = message.encode();
			while(in.readable()) {
				final FlvAtom flvAtom = new FlvAtom(in);
				final int absoluteTime = flvAtom.getHeader().getTime();
				channelTimes[primaryChannel] = absoluteTime;
//				write(flvAtom); // 書き込む
				writeHook(flvAtom);
			}// */
			int difference = -1;
			final ChannelBuffer in = message.encode();
			while(in.readable()) {
				final FlvAtom flvAtom = new FlvAtom(in);
				final RtmpHeader subHeader = flvAtom.getHeader();
				if(difference == -1) {
					difference = subHeader.getTime() - header.getTime();
				}
				final int absoluteTime = subHeader.getTime();
				channelTimes[primaryChannel] = absoluteTime;
				subHeader.setTime(subHeader.getTime() - difference);
				writeHook(flvAtom);
			}
		}
		else { // meta audio video
			final int channelId = header.getChannelId();
			channelTimes[channelId] = header.getTime();
			if(primaryChannel == -1 && (header.isAudio() || header.isVideo())) {
				primaryChannel = channelId;
			}
			if(header.getSize() <= 2) {
				return;
			}
			writeHook(new FlvAtom(header.getMessageType(), channelTimes[channelId], message.encode()));
		}
	}
	/**
	 * 音声のコーデックタイプを判定します。
	 * @param tag
	 * @return
	 */
	private CodecType getCodecType(AudioTag tag) {
		switch(tag.getCodecType()) {
		case ADPCM:           return CodecType.ADPCM;
		case MP3:             return CodecType.MP3;
		case PCM:             return CodecType.PCM;
		case NELLY_16:        return CodecType.NELLY_16;
		case NELLY_8:         return CodecType.NELLY_8;
		case NELLY:           return CodecType.NELLY;
		case G711_A:          return CodecType.G711_A;
		case G711_U:          return CodecType.G711_U;
		case RESERVED:        return CodecType.RESERVED;
		case AAC:             return CodecType.AAC;
		case SPEEX:           return CodecType.SPEEX;
		case MP3_8:           return CodecType.MP3_8;
		case DEVICE_SPECIFIC: return CodecType.DEVICE_SPECIFIC;
		default:
			return CodecType.NONE;
		}
	}
	/**
	 * 映像のコーデックタイプを判定します。
	 * @param tag
	 * @return
	 */
	private CodecType getCodecType(VideoTag tag) {
		switch(tag.getCodecType()) {
		case JPEG:         return CodecType.JPEG;
		case H263:         return CodecType.H263;
		case SCREEN:       return CodecType.SCREEN;
		case ON2VP6:       return CodecType.ON2VP6;
		case ON2VP6_ALPHA: return CodecType.ON2VP6_ALPHA;
		case SCREEN_V2:    return CodecType.SCREEN_V2;
		case AVC:          return CodecType.AVC;
		default:           return CodecType.NONE;
		}
	}
	/**
	 * rtmpから取得するデータはtimestampが前後することがあるので、音声パケットがきたらcacheしておき、映像パケットとソートしておく。
	 * 書き込み処理
	 * @param flvAtom
	 */
	private void writeHook(final FlvAtom flvAtom) {
		RtmpHeader header = flvAtom.getHeader();
		ChannelBuffer dataBuffer = flvAtom.getData().duplicate();
		if(!header.isAudio() && !header.isVideo() || dataBuffer.capacity() == 0) {
			// 音声でも映像でもない、データ量0のパケットは捨てます
			return;
		}
		// 最終アクセス時刻の記録(1秒強アクセスがなければストリームが停止したと判定させる。)
		lastAccessTime = System.currentTimeMillis();
		if(header.isAudio()) {
			executeAudio(flvAtom);
		}
		else {
			executeVideo(flvAtom);
		}
	}
	/**
	 * 書き込み処理最終
	 * @param flvAtom
	 */
	private void write(final FlvAtom flvAtom) {
		try {
			// ここでの書き込みをやめて、queueに登録するようにする。
			// writeを２度実行すると壊れる(バッファのポインタがうごく。)
			ChannelBuffer buffer = flvAtom.write();
			convertHandler.getFlvDataQueue().putData(buffer.duplicate());
			writeChannel.write(buffer.toByteBuffer());
		}
		catch (Exception e) {
			logger.error("ファイル書き込みに失敗しました。", e);
		}
	}
	/**
	 * 音声用の処理
	 * @param flvAtom
	 */
	private void executeAudio(final FlvAtom flvAtom) {
		RtmpHeader header = flvAtom.getHeader();
		ChannelBuffer dataBuffer = flvAtom.getData().duplicate();
		boolean sequenceHeader = false;
		// audio
		AudioTag tag = new AudioTag(dataBuffer.readByte());
		// コーデックを確認コーデック状態がかわっていることを確認した場合は、やりなおしにする必要があるので、有無をいわさず処理やり直しにする。
		if(audioCodec == null) {
			audioCodec = getCodecType(tag);
		}
		if(audioCodec != getCodecType(tag)) {
			stop();
		}
		if(tag.getCodecType() == AudioTag.CodecType.AAC) {
			if(dataBuffer.readByte() == 0x00) {
				aacMediaSequenceHeader = flvAtom; // 開始時に必ず送る必要があるので、保持しておく。
				sequenceHeader = true;
			}
		}
		if(!isPlaying) {
			// 初メディアデータであるか確認。初だったらplayTimeに現在のタイムスタンプを保持しておく。
			if(playTime == -1) {
				playTime = header.getTime();
			}
			if(sequenceHeader) {
				return;
			}
			// timestampが0に関しては無視する。
			if(header.getTime() == 0) {
				return;
			}
			if(header.getTime() - playTime > 1000 && videoCodec == null) {
				videoCodec = CodecType.NONE;
				// 動作を開始する。
				start(header);
			}
			else {
				return;
			}
		}
		if(videoCodec == CodecType.NONE) {
			// videoCodecが存在しないと判定された場合は、audioデータ単体で次のデータ化してよくなるため、そのまま追記するようにする。
			header.setTime(header.getTime() - startTime);
			write(flvAtom);
		}
		else {
			// videoCodecが存在している場合、もしくは、判定前の場合はaudioAtomはソートしたら利用する可能性があるので、queueにいれて保存しておく。
			audioAtomQueue.add(flvAtom);
		}
	}
	/**
	 * 動画用の処理
	 * @param flvAtom
	 */
	private void executeVideo(final FlvAtom flvAtom) {
		RtmpHeader header = flvAtom.getHeader();
		ChannelBuffer dataBuffer = flvAtom.getData().duplicate();
		boolean sequenceHeader = false;
		// video
		VideoTag tag = new VideoTag(dataBuffer.readByte());
		if(videoCodec == null) {
			videoCodec = getCodecType(tag);
		}
		if(videoCodec != getCodecType(tag)) {
			stop();
		}
		if(tag.getCodecType() == VideoTag.CodecType.AVC) {
			if(dataBuffer.readByte() == 0x00) {
				avcMediaSequenceHeader = flvAtom;
				sequenceHeader = true;
			}
		}
		if(!isPlaying) {
			// 初メディアデータであるか確認。初だったらplayTimeに現在のタイムスタンプを保持しておく。(ここにいれる理由は、コーデック違いにより、前の処理の部分で書き換えが発生する可能性があるため。)
			if(playTime == -1) {
				playTime = header.getTime();
			}
			if(sequenceHeader) {
				return;
			}
			// timestampが0に関しては無視する。
			if(header.getTime() == 0) {
				// タイムスタンプ0の通常データはとりいそぎ無視する。(FlashMediaServerのaggregateMessage対策)
				return;
			}
			// キーフレームを取得したタイミングでaudioCodecがきまっていない場合はaudioCodecなしとして動作開始してやる。
			if(tag.isKeyFrame() && // キーフレームの時のみ判定したい。
				(audioCodec != null || // audioCodecがきまっている場合
				header.getTime() - playTime > 1000)) { // もしくはaudioCodecはきまっていないが1秒たった場合
				if(audioCodec == null) {
					audioCodec = CodecType.NONE;
				}
				// 動作を開始する。
				start(header);
			}
			else {
				return;
			}
		}
		// 書き込む
		FlvAtom audioAtom = lastAudioAtom;
		lastAudioAtom = null;
		do {
			if(audioAtom == null) {
				audioAtom = audioAtomQueue.poll();
			}
			if(audioAtom == null) {
				break;
			}
			if(audioAtom.getHeader().getTime() > header.getTime()) {
				lastAudioAtom = audioAtom;
				break;
			}
			// 処理を続ける。
			audioAtom.getHeader().setTime(audioAtom.getHeader().getTime() - startTime);
			write(audioAtom);
			// 書き込み実施
			audioAtom = null;
		} while(true);
		// 動画データも書き込む
		header.setTime(header.getTime() - startTime);
		write(flvAtom);
	}
}
