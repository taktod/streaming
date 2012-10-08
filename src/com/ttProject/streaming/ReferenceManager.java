package com.ttProject.streaming;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * refはnodeをそのまま保持しておけばいいかも。
 * @author taktod
 */
public class ReferenceManager {
	/** データを保持するマップ */
	private static Map<String, ReferenceManager> referenceMap = new HashMap<String, ReferenceManager>();
	/** リファレンスのノードデータ */
	private Node node;
	/**
	 * リファレンスデータを参照する。
	 * @param name
	 * @return
	 */
	public static ReferenceManager getReferenceData(String name) {
		// nodeを保持するだけにしておく。
		return referenceMap.get(name);
	}
	/**
	 * コンストラクタ
	 * @param node
	 */
	public ReferenceManager(Node node) {
		// nodeについているデータを参照する。
		NamedNodeMap attributes = node.getAttributes();
		Node n = attributes.getNamedItem("id");
		if(n == null) {
			// 設定がおかしい。
			return;
		}
		this.node = node;
		referenceMap.put(n.getNodeValue(), this);
	}
	/**
	 * presetに登録されているデータを参照する。
	 * @return
	 */
	public Node getData() {
		return node;
	}
}
