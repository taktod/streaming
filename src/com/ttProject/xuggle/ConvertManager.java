package com.ttProject.xuggle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ttProject.xuggle.flv.FlvDataQueue;

/**
 * コンバート動作を管理するマネージャー
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここでは、すべてを管理します。
 * 
 * propertiesのファイルで変換は管理します？
 */
public class ConvertManager {
	/** 各部屋ごとのインスタンス保持 */
	private static final Map<String, ConvertManager> instances = new ConcurrentHashMap<String, ConvertManager>();
	/** 名前保持 */
	private String name;
	/** 動作queue */
	private FlvDataQueue queue;
	/**
	 * コンストラクタ
	 */
	private ConvertManager(String name) {
		this.name = name;
		initialize();
	}
	/**
	 * シングルトンのインスタンス取得
	 * @return
	 */
	public static ConvertManager getInstance(String name) {
		ConvertManager instance = instances.get(name);
		if(instance == null) {
			instance = new ConvertManager(name);
			instances.put(name, instance);
		}
		return instance;
	}
	/**
	 * 初期化
	 */
	private void initialize() {
		// encode.xmlから必要な情報を抜き出しておく。
		EncodeXmlAnalizer analizer = EncodeXmlAnalizer.getInstance();
		analizer.getManagers(); // 作成された、MediaManagerリストを取得する

		// 生データのtakStreamingをつくるかどうか確認する。
		// 作る必要があるなら、処理を追加する。
		// これはコンバートなしでも動作します。
		
		// コンバートすべきか確認する
		// コンバートする必要があるなら、queueをつくっておく。
		// FlvManagerをつくる。
		// AudioResampleManagerをつくる。(変換必須数をしらべてつくる。)
		// VideoResampleManagerをつくる。(変換必須数をしらべてつくる。)
		// AudioEncodeManagerをつくる。
		// VideoEncodeManagerをつくる。
		// 出力コンテナもつくる。
		
		// それぞれのオブジェクトは必要と思われるだけつくっておく。
	}
}
