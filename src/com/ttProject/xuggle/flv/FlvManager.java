package com.ttProject.xuggle.flv;

import java.nio.ByteBuffer;

import com.flazr.io.flv.FlvAtom;
import com.ttProject.xuggle.ConvertManager;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.SimpleMediaFile;

/**
 * コンバートのデータ取得処理を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここでは元ファイル -> データ取得の部分を担当
 */
public class FlvManager {
	private IContainer inputContainer = null;
	private IStreamCoder inputAudioCoder = null;
	private IStreamCoder inputVideoCoder = null;
	private int audioStreamId = -1;
	public int getAudioStreamId() {
		return audioStreamId;
	}
	private int videoStreamId = -1;
	public int getVideoStreamId() {
		return videoStreamId;
	}
	private FlvDataQueue inputDataQueue = null;
	
	public FlvManager() {
		setup();
		// TODO コンテナについては、たぶんここでつくっても問題ないと思うので(transcoderでは動作threadの冒頭で実施していました。)実行しておく。
		openInputContainer();
	}
	/**
	 * 
	 * @return
	 */
	public boolean setup() {
		ConvertManager convertManager = ConvertManager.getInstance();
		// 変換用のHandlerを準備しておく。
		inputDataQueue = new FlvDataQueue();
		FlvHandler flvHandler = new FlvHandler(inputDataQueue);
		FlvHandlerFactory flvFactory = FlvHandlerFactory.getFactory();
		flvFactory.registerHandler(convertManager.getName(), flvHandler);
		return true;
	}
	/**
	 * flvのデータを受け付ける。
	 * @param flvAtom
	 */
	public void writeData(FlvAtom flvAtom) {
		// bufferデータのコピーを作成。
		ByteBuffer buffer = flvAtom.write().toByteBuffer().duplicate();
		// queueに登録、あとはffmpegに任せる。
		inputDataQueue.putTagData(buffer);
	}
	/**
	 * 入力コンテナを開いておく。
	 * コンストラクタで動作、入力用のffmpegのコンテナをあけておく。
	 */
	public void openInputContainer() {
		String url;
		int retval = -1;
		ConvertManager convertManager = ConvertManager.getInstance();
		
		url = FlvHandlerFactory.DEFAULT_PROTOCOL + ":" + convertManager.getName();
		ISimpleMediaFile inputInfo = new SimpleMediaFile();
		inputInfo.setURL(url);
		inputContainer = IContainer.make();
		IContainerFormat inputFormat = IContainerFormat.make();
		inputFormat.setInputFormat("flv");
		
		// url, read動作, フォーマットはflv, dynamicに動作して, metaデータはなしという指定
		retval = inputContainer.open(url, IContainer.Type.READ, inputFormat, true, false);
		if(retval < 0) {
			throw new RuntimeException("入力用のURLを開くことができませんでした。");
		}
	}
	/**
	 * 入力コーダーを確認、必要なら生成する。
	 * @return true:処理する必要のあるパケット false:処理する必要のないパケット
	 * 
	 * この処理はループでまわっているthread(入力thread)で実行する動作
	 */
	public boolean checkInputCoder(IPacket packet) {
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
}
