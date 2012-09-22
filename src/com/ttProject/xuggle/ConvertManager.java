package com.ttProject.xuggle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ttProject.streaming.MediaManager;
import com.ttProject.streaming.tak.TakManager;
import com.ttProject.xuggle.flv.FlvDataQueue;

/**
 * コンバート動作を管理するマネージャー
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここでは、すべてを管理します。
 * 
 * propertiesのファイルで変換は管理します？
 * convertManagerって１つじゃね？
 */
public class ConvertManager {
	/** 各部屋ごとのインスタンス保持 */
//	private static final Map<String, ConvertManager> instances = new ConcurrentHashMap<String, ConvertManager>();
	private static final ConvertManager instance = new ConvertManager();
	/** 名前保持 */
	private String name;
	/** 動作queue */
	private FlvDataQueue queue;
	/** 生データのtakStreaming用のmanager */
	private TakManager rawTakManager = null;
	/**
	 * コンストラクタ
	 */
	private ConvertManager() {
	}
	/**
	 * シングルトンのインスタンス取得
	 * @return
	 */
	public static ConvertManager getInstance() {
		if(instance == null) {
			throw new RuntimeException("インスタンスが消滅しました。");
		}
		return instance;
	}
	/**
	 * 初期化
	 */
	public void initialize(String name) {
		// 名前を保持しておく。
		this.name = name;
		// encode.xmlから必要な情報を抜き出しておく。
		EncodeXmlAnalizer analizer = EncodeXmlAnalizer.getInstance();
		// このマネージャーリストにそって、コンバートを実行する必要がある。
		List<MediaManager> mediaManagers = analizer.getManagers(); // 作成された、MediaManagerリストを取得する
		
		boolean needConvert = false;
		// managerの内容を確認する。
		for(MediaManager manager : mediaManagers) {
			manager.analize();
			if(manager instanceof TakManager) {
				TakManager takManager = (TakManager)manager;
				if(takManager.isRawStream()) {
					// 生データのストリーミングが存在する。
					rawTakManager = takManager;
				}
				else {
					needConvert = true;
				}
			}
			else {
				needConvert = true;
			}
		}
		if(!needConvert) {
			// コンバートする必要がないので、ここでおわる。
			return;
		}
		// コンバートを実行する必要がある。
		// flvデータを保持するqueueが必要。
		// FlvManagerをつくる。
		// AudioResampleManagerをつくる。(変換必須数をしらべてつくる。)
		// VideoResampleManagerをつくる。(変換必須数をしらべてつくる。)
		// AudioEncodeManagerをつくる。
		// VideoEncodeManagerをつくる。
		// 出力コンテナもつくる。
	}
}
