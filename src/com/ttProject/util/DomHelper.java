package com.ttProject.util;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * DOMを扱う上での補助動作
 * @author taktod
 */
public class DomHelper {
	/**
	 * <a test="b">
	 * と
	 * <a>
	 *  <test value="b" />
	 * </a>
	 * を同等にする操作
	 * @param node
	 * @return
	 */
	public static String getNodeValue(Node node, String target) {
		// 属性にtargetがあるか確認する。
		NamedNodeMap attributes = node.getAttributes();
		if(attributes != null) {
			for(int i=0;i < attributes.getLength();i ++) {
				Node item = attributes.item(i);
				if(item.getNodeName().equalsIgnoreCase(target)) {
					// targetがある場合はその値を応答する。
					return item.getNodeValue();
				}
			}
		}
		// targetがなかった場合は、子要素のなかから、targetとnode名が一致するものをみつけて、その値を取得する。
		NodeList children = node.getChildNodes();
		if(children != null) {
			for(int i=0;i < children.getLength();i ++) {
				Node item = children.item(i);
				switch(item.getNodeType()) {
				case Node.ELEMENT_NODE:
					if(item.getNodeName().equalsIgnoreCase(target)) {
						return getNodeValue(item);
					}
					break;
				default:
					break;
				}
			}
		}
		return null;
	}
	/**
	 * <a value="b" />
	 * と
	 * <a>b</a>
	 * を同等にする操作
	 * @return
	 */
	public static String getNodeValue(Node node) {
		// 属性から取得を試みる。
		NamedNodeMap attributes = node.getAttributes();
		if(attributes != null) {
			for(int i=0;i < attributes.getLength();i ++) {
				Node item = attributes.item(i);
				if(item.getNodeName().equalsIgnoreCase("value")) {
					return item.getNodeValue();
				}
			}
		}
		NodeList children = node.getChildNodes();
		if(children != null) {
			for(int i=0;i < children.getLength();i ++) {
				Node item = children.item(i);
				switch(item.getNodeType()) {
				case Node.TEXT_NODE:
					return item.getNodeValue().trim();
				default:
					break;
				}
			}
		}
		return null;
	}
}
