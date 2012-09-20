package com.ttProject.streaming.tak;

import org.w3c.dom.Node;

import com.ttProject.streaming.MediaManager;
import com.ttProject.util.DomHelper;
import com.xuggle.xuggler.IContainer;

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
	private IContainer container = null;
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
	public void setup() {
		// 生データの場合は、handlerをつくっておき、データをうけとったら、そのままhandlerに流すという処置が必要。
	}
}
