package com.ttProject.xuggle.flv;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.ttProject.xuggle.ConvertManager;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;

/**
 * コンバートのデータ取得処理を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここでは元ファイル -> データ取得の部分を担当
 */
public class FlvManager {
	/** 動作ロガー */
	private final Logger logger = LoggerFactory.getLogger(FlvManager.class);
	private IContainer inputContainer = null;
	private IStreamCoder inputAudioCoder = null;
	private IStreamCoder inputVideoCoder = null;
	private int audioStreamId = -1;
	private int videoStreamId = -1;
	private FlvDataQueue inputDataQueue = null;
	private FlvCustomReader customReader = null;
	public FlvManager() {
		logger.info("flvManagerを初期化します。");
		setup();
	}
	/**
	 * 
	 * @return
	 */
	public boolean setup() {
		logger.info("セットアップの開始");
		ConvertManager convertManager = ConvertManager.getInstance();
		// 変換用のHandlerを準備しておく。
		inputDataQueue = new FlvDataQueue();
		FlvHandler flvHandler = new FlvHandler(inputDataQueue);
		FlvHandlerFactory flvFactory = FlvHandlerFactory.getFactory();
		flvFactory.registerHandler(convertManager.getName(), flvHandler);
		customReader = new FlvCustomReader(inputDataQueue);
		inputDataQueue.putHeaderData(FlvAtom.flvHeader().toByteBuffer());
		return true;
	}
	/**
	 * flvのデータを受け付ける。
	 * @param flvAtom
	 */
	public void writeData(ByteBuffer buffer) {
		// queueに登録、あとはffmpegに任せる。
		inputDataQueue.putTagData(buffer);
	}
	/**
	 * 入力コンテナを開いておく。
	 * コンストラクタで動作、入力用のffmpegのコンテナをあけておく。
	 */
	public void openInputContainer() {
		logger.info("入力コンテナを開きます。");
		String url;
		int retval = -1;
		ConvertManager convertManager = ConvertManager.getInstance();

		url = FlvHandlerFactory.DEFAULT_PROTOCOL + ":" + convertManager.getName();
		inputContainer = IContainer.make();
		IContainerFormat inputFormat = IContainerFormat.make();
		inputFormat.setInputFormat("flv");

		// url, read動作, フォーマットはflv, dynamicに動作して, metaデータはなしという指定
		retval = inputContainer.open(url, IContainer.Type.READ, inputFormat, true, false);
		if(retval < 0) {
			logger.info("入力コンテナの開くのに失敗しました。");
			throw new RuntimeException("入力用のURLを開くことができませんでした。");
		}
	}
	/**
	 * 入力コーダーを確認、必要なら生成する。
	 * @return true:処理する必要のあるパケット false:処理する必要のないパケット
	 * 
	 * この処理はループでまわっているthread(入力thread)で実行する動作
	 */
	private boolean checkInputCoder(IPacket packet) {
		IStream stream = inputContainer.getStream(packet.getStreamIndex());
		if(stream == null) {
			// ストリームが取得できませんでした。(欠損しているデータがある場合があるので、発生してもおかしくないです。)
			// 処理続行は無理
			return false;
		}
		IStreamCoder coder = stream.getStreamCoder();
		if(coder == null) {
			// coderが取得できませんでした。(こちらも欠損しているかのヌセイあるので、発生してもおかしくない。)
			// 処理続行は無理
			return false;
		}
		if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
			// 音声コーデック
			if(inputAudioCoder == null) {
				// 音声コーダーが設定されていないということは初めてのデータなので、処理する必要あり。
				// 出力コンテナを作り直す
				ConvertManager convertManager = ConvertManager.getInstance();
				convertManager.resetupOutputContainer(true, videoStreamId != -1);
			}
			else if(inputAudioCoder.hashCode() == coder.hashCode()) {
				// コーダーが一致する場合は、このままコーダーをつかって処理をすれば問題ない。
				return true;
			}
			else {
				// いままでつかっていたコーダーと違うパケットがきた場合は、再初期化が必要。
				// いままでつかっていたものは閉じておく。
				inputAudioCoder.close();
				inputAudioCoder = null;
			}
			audioStreamId = packet.getStreamIndex();
			if(coder.open(null, null) < 0) {
				throw new RuntimeException("audio入力用のデコーダーを開くのに失敗しました。");
			}
			inputAudioCoder = coder;
		}
		else if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
			// 映像コーデック
			if(inputVideoCoder == null) {
				// 映像コーダーが設定されていないということは初めてのデータなので、処理する必要あり。
				// 出力コンテナを作り直す
				ConvertManager convertManager = ConvertManager.getInstance();
				convertManager.resetupOutputContainer(audioStreamId != -1, true);
			}
			else if(inputVideoCoder.hashCode() == coder.hashCode()) {
				// コーダーが一致する場合は、このままコーダーをつかって処理をすれば問題ない。
				return true;
			}
			else {
				// いままでつかっていたコーダーと違う場合は、再生成させる。
				inputVideoCoder.close();
				inputVideoCoder = null;
			}
			videoStreamId = packet.getStreamIndex();
			if(coder.open(null, null) < 0) {
				throw new RuntimeException("video入力用のデコーダーを開くのに失敗しました。");
			}
			inputVideoCoder = coder;
		}
		else {
			// 映像でも音声でもないデータは処理しません。
			return false;
		}
		return true;
	}
	/**
	 * 閉じます。
	 */
	public void close() {
		if(inputAudioCoder != null) {
			inputAudioCoder.close();
			inputAudioCoder = null;
		}
		if(inputVideoCoder != null) {
			inputVideoCoder.close();
			inputVideoCoder = null;
		}
		if(inputContainer != null) {
			inputContainer.close();
			inputContainer = null;
		}
	}
	/**
	 * コンバート処理をはじめる。
	 * @param target
	 * @return
	 */
	public boolean execute() {
		int retval = -1;
		// 入力コンテナからデータを引き出す。
		IPacket packet = IPacket.make(); // 使い回しで実行することも可能だが、作り直した方が安定するっぽい。
		ConvertManager convertManager = ConvertManager.getInstance();
		if(convertManager.isProcessingFlvHandler()) {
			retval = inputContainer.readNextPacket(packet);
			if(retval < 0) {
				logger.error("パケット取得エラー: {}, {}", IError.make(retval), retval);
				if("Resource temporarily unavailable".equals(IError.make(retval).getDescription())) {
					// リソースが一時的にない場合は、このまま続けていれば動作可能になるので、スルーする。
					return true;
				}
				return false;
			}
			if(packet.getDts() + 10000 < packet.getPts()) {
//				logger.warn("元データ動作が停止しました。");
				convertManager.setProcessingFlvHandler(false);
			}
/*			try {
				logger.info(packet.toString());
				byte[] bufCheck = new byte[32];
				packet.getByteBuffer().duplicate().get(bufCheck);
				logger.info("a:" + Utils.toHex(bufCheck));
			}
			catch (Exception e) {
			}*/
		}
/*		else {
			logger.info("■");
		}*/
		boolean result = customReader.readNextPacket(packet);
/*		try {
			logger.info(packet.toString());
			byte[] bufCheck = new byte[32];
			packet.getByteBuffer().duplicate().get(bufCheck);
			logger.info("b:" + Utils.toHex(bufCheck));
		}
		catch (Exception e) {
//			logger.info("エラー", e);
		}*/
		if(!result) {
			return true; // 次にいく。(スルー)
		}

		// 入力コーダーを確認します。
		if(!checkInputCoder(packet)) {
//			logger.info("処理すべきコーダーではないみたいです。");
			return true;
		}
		int index = packet.getStreamIndex();
		if(index == audioStreamId) {
			executeAudio(packet);
		}
		else if(index == videoStreamId) {
			executeVideo(packet);
		}
		else {
			// ここは一応dead code.
			return false;
		}
		return true;
	}
	/**
	 * 音声の処理を実施する。
	 * @param targetPacket
	 */
	private void executeAudio(IPacket targetPacket) {
		int retval = -1;
		IAudioSamples inSamples = IAudioSamples.make(1024, inputAudioCoder.getChannels());
		int offset = 0;
		while(offset < targetPacket.getSize()) {
			// デコード実行
			retval = inputAudioCoder.decodeAudio(inSamples, targetPacket, offset);
			if(retval <= 0) {
				logger.warn("デコードに失敗するパケットがきました。(ただしスルーして次に進む)");
				return;
			}
			offset += retval;
			
			if(inSamples.isComplete()) {
				// パケットがコンプリートしているなら、次にすすめます。
				ConvertManager convertManager = ConvertManager.getInstance();
				convertManager.executeAudio(inSamples);
			}
		}
	}
	/**
	 * 映像の処理を実施する。
	 * @param targetPacket
	 */
	private void executeVideo(IPacket targetPacket) {
		int retval = -1;
		IVideoPicture inPicture = IVideoPicture.make(inputVideoCoder.getPixelType(), inputVideoCoder.getWidth(), inputVideoCoder.getHeight());
		int offset = 0;
		while(offset < targetPacket.getSize()) {
			retval = inputVideoCoder.decodeVideo(inPicture, targetPacket, offset);
			if(retval <=0) {
				logger.warn("デコードに失敗するパケットがきました。(ただしスルーして次にすすみます。)");
				return;
			}
			offset += retval;
			
			if(inPicture.isComplete()) {
				// 処理が完了している場合は、次のりサンプルとエンコードに進む。
				// ここから先はコンバート情報に依存するので、別のところで処理すべき。
				ConvertManager convertManager = ConvertManager.getInstance();
				convertManager.executeVideo(inPicture);
			}
		}
	}
}
