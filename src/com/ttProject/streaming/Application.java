package com.ttProject.streaming;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamCodecInfo;

import com.ttProject.red5.server.plugin.websocket.IWebSocketDataListener;
import com.ttProject.red5.server.plugin.websocket.WebSocketConnection;
import com.ttProject.red5.server.plugin.websocket.WebSocketScopeManager;
import com.ttProject.streaming.iphone.TsM3u8Model;
import com.ttProject.streaming.jpegmp3.JpegModel;
import com.ttProject.streaming.jpegmp3.Mp3M3u8Model;
import com.ttProject.xuggle.Transcoder;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.SimpleMediaFile;

/**
 * とりあえずやっておきたいこと。
 * １：streamのデータをうけとってしばらくしてから、(10パケットとか20パケットとか)コンバートを開始する。(でないと音声や映像のどちらかがないものがあったらこまる。)
 * ２：途中でトラックが追加された場合は、自動的にデータを追記する。(できたらtsパケットをとめずに対処したい。)
 * ３：出力先等の設定データをMBean設定で済ませる。(すぐにできそう。) 済み
 * ４：再度放送しなおした場合は、別のストリームを開始する。
 * ５：shutdownHookをいれておき、停止するときに、少なくともファイルはきちんとcloseしておきたい。
 * 
 * １を実施するには、動作を大幅に変更する必要あり。(開始部分など)
 * ２を実施するには、中途でコンテナを切り替える実験が必要。できたら中途から別のストリームを挟む込めるとなおよし。
 * @author taktod
 */
public class Application extends ApplicationAdapter implements IWebSocketDataListener{
	// とりあえず複数の変換コーダーを持つことは考えないことにしたい。
	final private Map<String, Transcoder> mTranscoders = new HashMap<String, Transcoder>();
	// 各モデルにデータを設置するSetter動作
	public void setMp3Path(String path) {
		Mp3M3u8Model.setPath(path);
	}
	public void setMp3UrlPath(String path) {
		Mp3M3u8Model.setUrlPath(path);
	}
	public void setJpegPath(String path) {
		JpegModel.setPath(path);
	}
	public void setJpegUrlPath(String path) {
		JpegModel.setUrlPath(path);
	}
	public void setTsPath(String path) {
		TsM3u8Model.setPath(path);
	}
	public void setTsUrlPath(String path) {
		TsM3u8Model.setUrlPath(path);
	}
	@Override
	public boolean appStart(IScope app) {
		// コネクトがなくても初期化されるみたいです。よかったよかった
		WebSocketScopeManager wssm = new WebSocketScopeManager();
		wssm.addPluginedApplication(app.getName(), this);
		return super.appStart(app);
	}
	/**
	 * ストリーム開始時
	 */
	@Override
	public synchronized void streamPublishStart(IBroadcastStream stream) {
		// testというストリーム以外は処理を許可しない。
		if(!"test".equals(stream.getPublishedName())) {
			// たしかここでstopはかけれなかった気がする。(とりあえずトランスコードしない。(listenしない、でいいと思う。))
			return;
		}
		super.streamPublishStart(stream);
		// ここでストリームの各データの有無を確認しているが、ここでやってしまうと、はじめのパケットがながれてきていないので、両方ないことになってしまう。
		IStreamCodecInfo info = stream.getCodecInfo();
		if(info.hasAudio()) {
			System.out.println("has Audio!!!!");
		}
		if(info.hasVideo()) {
			System.out.println("has Video!!!!");
		}
		// transcodeを実行させる。
		ISimpleMediaFile outputStreamInfo = new SimpleMediaFile();
		outputStreamInfo.setHasAudio(true);
		outputStreamInfo.setAudioBitRate(64000);
		outputStreamInfo.setAudioChannels(2);
		outputStreamInfo.setAudioSampleRate(44100);
		outputStreamInfo.setAudioCodec(ICodec.ID.CODEC_ID_MP3);

		outputStreamInfo.setHasVideo(true);
		outputStreamInfo.setVideoWidth(320);
		outputStreamInfo.setVideoHeight(240);
		outputStreamInfo.setVideoBitRate(500000);
		outputStreamInfo.setVideoFrameRate(IRational.make(1, 15));
		outputStreamInfo.setVideoCodec(ICodec.ID.CODEC_ID_H264);
		outputStreamInfo.setVideoGlobalQuality(0);

		Transcoder transcoder = new Transcoder(stream, outputStreamInfo);
		Thread transcoderThread = new Thread(transcoder);
		transcoderThread.setDaemon(true);
		mTranscoders.put(stream.getPublishedName(), transcoder);
		transcoderThread.start();
	}
	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		Transcoder transcoder = mTranscoders.remove(stream.getPublishedName());
		if(transcoder != null) {
			// 放送がおわったらtranscoderを破壊する。
			transcoder.stop();
		}
		super.streamBroadcastClose(stream);
	}
	/**
	 * 以下webSocket用
	 */
	public static final Set<WebSocketConnection> conns = new HashSet<WebSocketConnection>();
	@Override
	public void connect(WebSocketConnection conn) {
		synchronized(conns) {
			conns.add(conn);
		}
	}
	@Override
	public void leave(WebSocketConnection conn) {
		synchronized(conns) {
			conns.remove(conn);
		}
	}
	@Override
	public void receiveData(WebSocketConnection conn, Object data) {
	}
}
