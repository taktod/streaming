package com.ttProject.red5;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.stream.IBroadcastStream;

import com.ttProject.xuggle.TranscodeManager;
import com.ttProject.xuggle.in.flv.FlvInputManager;
import com.ttProject.xuggle.out.mpegts.MpegtsOutputManager;

/**
 * red5のbroadcastされているストリームの変換を定義するマネージャー
 * 今回はここで、すべての出力を定義します。
 * @author taktod
 */
public class Red5TranscodeManager extends TranscodeManager {
	/** httpTakStreamingを生成するかどうか */
	private boolean httpTak = false;
	/** httpLiveStreamingを生成するかどうか */
	private boolean httpLive = false;
	/** jpegMp3Streamingを生成するかどうか */
	private boolean jpegMp3 = false;
	/**
	 * コンストラクタ
	 */
	public Red5TranscodeManager() {
		// 入力はflv一択なので、そうしておく。
		setInputManager(new FlvInputManager());
	}
	public void setHttpTak(boolean httpTak) {
		this.httpTak = httpTak;
	}
	public void setHttpLive(boolean httpLive) {
		this.httpLive = httpLive;
	}
	public void setJpegMp3(boolean jpegMp3) {
		this.jpegMp3 = jpegMp3;
	}
	private static Map<String, Red5TranscodeManager> map = new ConcurrentHashMap<String, Red5TranscodeManager>();
	private String name;
	/**
	 * 個別のストリーム用のクラスの生成
	 * @param name
	 */
	private Red5TranscodeManager(String name) {
		// ストリームに対応したtranscodeManagerを作成する。
		this.name = name;
	}
	private void setJpegMp3Creator() {
		
	}
	private void setHttpLiveCreator() {
		
	}
	private void setHttpTakCreator() {
		
	}
	private void setStreamListener(IBroadcastStream stream) {
	}
	/**
	 * 変換と登録します。s
	 * @param stream
	 */
	public void registerTranscoder(IBroadcastStream stream) {
		// このストリームに対するオブジェクトを生成しておく。
		Red5TranscodeManager manager = new Red5TranscodeManager(stream.getName());
		map.put(stream.getName(), manager);
		// 有効なストリームに対するCreatorを作成する。
		if(httpTak) {
			manager.setHttpTakCreator();
		}
		if(httpLive) {
			manager.setHttpLiveCreator();
		}
		if(jpegMp3) {
			manager.setJpegMp3Creator();
		}
		// StreamListenerを作成しておく。
		manager.setStreamListener(stream);
	}
}
