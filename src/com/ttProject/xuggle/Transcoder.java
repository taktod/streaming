package com.ttProject.xuggle;

import java.util.HashMap;
import java.util.Map;

import org.python.modules.synchronize;

import com.ttProject.xuggle.in.flv.FlvHandlerFactory;
import com.ttProject.xuggle.out.mpegts.MpegtsHandlerFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.SimpleMediaFile;

/**
 * Xuggleでメディアデータのコンバートを実行します。
 * @author taktod
 *
 */
public class Transcoder implements Runnable {
	private IContainer inputContainer = null;
	private IContainer outputContainer = null;
	private ISimpleMediaFile outputStreamInfo;
	private Map<String, String> videoProperties = new HashMap<String, String>();
	private Map<IStreamCoder.Flags, Boolean> videoFlags = new HashMap<IStreamCoder.Flags, Boolean>();
	private int audioStreamId = -1;
	private int videoStreamId = -1;
	/**
	 * インプット要素がなんであるか指定が必要
	 */
	public Transcoder() {
		// 作成するファイルについての詳細をつくりあげる必要がある。
		// コンバートのcodecを入手したときに、変換後のデータ用のコーデックとして必要なものを記述しておかないといけない。
		// TODO とりあえず一回つくってみよう。
		// 出力ファイルの設定が必要。(実際に存在するかは、あとで決める。)
		outputStreamInfo = new SimpleMediaFile();
		outputStreamInfo.setAudioBitRate(64000);
		outputStreamInfo.setAudioChannels(2);
		outputStreamInfo.setAudioSampleRate(44100);
		outputStreamInfo.setAudioCodec(ICodec.ID.CODEC_ID_MP3);
		
		outputStreamInfo.setVideoWidth(320);
		outputStreamInfo.setVideoHeight(240);
		outputStreamInfo.setVideoBitRate(500000);
		outputStreamInfo.setVideoFrameRate(IRational.make(1, 15));
		outputStreamInfo.setVideoCodec(ICodec.ID.CODEC_ID_H264);
		outputStreamInfo.setVideoGlobalQuality(0);

		videoProperties.put("coder", "0");
		videoProperties.put("me_method", "hex");
		videoProperties.put("subq", "7");
		videoProperties.put("bf", "0");
		videoProperties.put("level", "13");
		videoProperties.put("me_range", "16");
		videoProperties.put("qdiff", "3");
		videoProperties.put("g", "150");
		videoProperties.put("qmin", "12");
		videoProperties.put("qmax", "30");
		videoProperties.put("refs", "3");
		videoProperties.put("qcomp", "0");
		videoProperties.put("maxrate", "600k");
		videoProperties.put("bufsize", "2000k");
		videoFlags.put(IStreamCoder.Flags.FLAG_LOOP_FILTER, true);
	}
	@Override
	public void run() {
		// 読み込み用のコンテナをオープン
		openReadContainer();
		// 変換を実行
		transcode();
		// 終わったら変換を止める。
	}
	/**
	 * 読み込み用のコンテナを開く
	 */
	private void openReadContainer() {
		String url;
		int retval = -1;
		url = FlvHandlerFactory.DEFAULT_PROTOCOL + ":test";
		ISimpleMediaFile inputInfo = new SimpleMediaFile();
		inputInfo.setURL(url);
		inputContainer = IContainer.make();
		IContainerFormat inputFormat = IContainerFormat.make();
		inputFormat.setInputFormat("flv"); // 形式をflvにしておく。
		retval = inputContainer.open(url, IContainer.Type.READ, inputFormat, true, false);
		if(retval < 0) {
			throw new RuntimeException("入力用のURLを開くことができませんでした。" + url);
		}
		url = MpegtsHandlerFactory.DEFAULT_PROTOCOL + ":test";
		outputStreamInfo.setURL(url);
		outputContainer = IContainer.make();
		IContainerFormat outputFormat = IContainerFormat.make();
		outputFormat.setOutputFormat("mpegts", url, null);
		retval = outputContainer.open(url, IContainer.Type.WRITE, outputFormat);
		if(retval < 0) {
			throw new RuntimeException("出力用のURLを開くことができませんでした。" + url);
		}
		// コーデック等の詳細はcheckInputCoderで実行します。
	}
	/**
	 * 変換を実行
	 */
	private void transcode() {
		int retval = -1;
		synchronized (this) {
			notifyAll();
		}
		// 動作パケットの受け皿を準備しておく。
		IPacket packet = IPacket.make();
		while(true) {
			// パケットの入力を取得する。
			retval = inputContainer.readNextPacket(packet);
			if(retval < 0) {
				// データの受け取りに失敗したので、おわる。
				break;
			}
			IPacket decodePacket = packet;
			// 入力コーダーを開きます。
			checkInputCoder(decodePacket);
			break;
		}
	}
	/**
	 * パケット情報から、動作に必要なコーダーを開きます。
	 * また映像or音声のあたらしいパケットを入手した場合は出力ファイルを変更する必要がでてくるので、そっちの処理も実行します。
	 * とりあえず始めはaddNewStreamがあるので、そっちでできることを期待します。
	 * @param packet
	 */
	private boolean checkInputCoder(IPacket packet) {
		// どうやらContainerにaddNewStreamをしない限り、動作できるらしい。(あとから追加が可能？っぽい。)
		IStreamCoder audioCoder = null;
		IStreamCoder videoCoder = null;
//		int numStreams = inputContainer.getNumStreams();
		IStream stream = inputContainer.getStream(packet.getStreamIndex());
		if(stream == null) {
			// ストリームが取得できませんでした。
			return false;
		}
		IStreamCoder coder = stream.getStreamCoder();
		if(coder == null) {
			// coderが取得できませんでした。
			return false;
		}
		if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
			// audioCodec
			// いままでのコーデックとつきあわせて変更になっていない場合はスキップする。
			// いままでにAudioのフレームがなかった場合はoutputContainerに追加する必要あり.
		}
		else if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
			// videoCodec
			// いままでにVideoのフレームがなかった場合はoutputContainerに追加する必要あり。
		}
		else {
			// 音声でも映像でもないものは処理しない。
			return false;
		}
		return true;
	}
	private boolean checkResampler() {
		return true;
	}
}
