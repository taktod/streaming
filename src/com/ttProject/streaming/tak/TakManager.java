package com.ttProject.streaming.tak;

import org.w3c.dom.Node;

import com.ttProject.streaming.MediaManager;
import com.ttProject.util.DomHelper;
import com.ttProject.xuggle.ConvertManager;

/**
 * コンバートの動作を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここではコンテナに突っ込むの部分を担当
 */
public class TakManager extends MediaManager{
	/** rawStreamがtrueの場合は生入力データをそのままTakHandlerに渡しています。 */
	private boolean isRawStream = false;
	private TakHandler handler; // 処理Handler
	/**
	 * nodeからデータを解析してマネージャーを作成します。
	 * @param node
	 */
	public TakManager(Node node) {
		// 使い回せる部分がある場合はそれをMediaManagerにもっていく。	
		super(node);
		// 属性として、nodeがrawdata = trueをもっている場合はエンコードなしの動作になります。
		isRawStream = "true".equalsIgnoreCase(DomHelper.getNodeValue(node, "rawdata"));
	}
	/**
	 * 生データのストリーミングがあるかどうか
	 * @return
	 */
	public boolean isRawStream() {
		return this.isRawStream;
	}
	/**
	 * コンバート動作のセットアップを実行しておく。
	 */
	@Override
	public boolean setup() {
		// ここで実行することは、streamURLの登録とcontainerを開くこと(コンテナを開く動作はしなくてもいいかも)
		handler = new TakHandler("~/test" + getName() + ".flv"); // handlerをつくっておく。
		if(!isRawStream()) {
			// 生ストリームでない場合はTakHandlerの登録とFactoryの作成が必要。
			TakHandlerFactory factory = TakHandlerFactory.getFactory();
			ConvertManager convertManager = ConvertManager.getInstance();
			factory.registerHandler(convertManager.getName() + "_" + getName(), handler);
		}
		return true;
	}
	@Override
	public boolean resetupContainer() {
		ConvertManager convertManager = ConvertManager.getInstance();
		String url = TakHandlerFactory.DEFAULT_PROTOCOL + ":" + convertManager.getName() + "_" + getName();
		return resetupContainer(url, "flv");
	}
}
