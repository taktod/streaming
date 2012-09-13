package com.ttProject.streaming.tak;

import com.xuggle.xuggler.IContainer;

/**
 * コンバートの動作を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここではコンテナに突っ込むの部分を担当
 */
public class TakManager {
	/** rawStreamがtrueの場合は生入力データをそのままTakHandlerに渡しています。 */
	private boolean isRawStream = false;
	private IContainer container = null;
	private TakHandler handler; // 処理Handler
	public TakManager() {
		
	}
}
