package com.ttProject.xuggle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ttProject.streaming.MediaManager;
import com.ttProject.streaming.ReferenceManager;
import com.ttProject.streaming.hls.HlsManager;
import com.ttProject.streaming.tak.TakManager;
import com.ttProject.streaming.webm.WebMManager;

/**
 * encodeXmlの内容を解析するクラス
 * @author taktod
 */
public class EncodeXmlAnalizer {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(EncodeXmlAnalizer.class);
	/** シングルトンインスタンス */
	private static EncodeXmlAnalizer instance = new EncodeXmlAnalizer();
	/** マネージャーリスト */
	private List<MediaManager> managers = new ArrayList<MediaManager>();
	/** シングルトン取得 */
	public static EncodeXmlAnalizer getInstance() {
		if(instance == null) {
			throw new RuntimeException("シングルトンインスタンスが消滅していました。");
		}
		return instance;
	}
	/**
	 * コンストラクタ
	 */
	private EncodeXmlAnalizer() {
		setupXml();
	}
	/**
	 * xmlの内容を読み込み直す。
	 */
	private void setupXml() {
		try {
			// この部分でxmlを解析しておき、必要な情報が拾えるようにしておく。
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Node root = builder.parse(new File("encode.xml"));
			String nodeName = root.getFirstChild().getNodeName();
			if(!nodeName.equals("streaming")) {
				logger.info("streamingのxmlではない。");
			}
			// 内容を解析しておく。
			NodeList data = root.getFirstChild().getChildNodes();
			for(int i = 0;i < data.getLength();i ++) {
				analize(data.item(i));
			}
			// 各ストリーミング用のデータをまとめておく。
			// 全部読み込みおわったら、データのセットアップをすすめていく。
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("encode.xmlのパースに失敗しました。");
		}
	}
	private void analize(Node node) {
		String nodeName = node.getNodeName();
		// preset定義
		if(nodeName.equalsIgnoreCase("preset")) {
			// preset定義があとからきても問題ないようにしておきたい。
			new ReferenceManager(node); // これはもうこのまま放置でOK
		}
		// ストリーム定義
		else if(nodeName.equalsIgnoreCase("tak")) {
			// takのみ属性として、rawDataというのが許されます。
			logger.info("tak");
			managers.add(new TakManager(node));
		}
		else if(nodeName.equalsIgnoreCase("hls")) {
			logger.info("hls");
			managers.add(new HlsManager(node));
		}
		else if(nodeName.equalsIgnoreCase("webM")) {
			logger.info("webm");
			managers.add(new WebMManager(node));
		}
	}
	/**
	 * 作り上げたリストを応答する。
	 * @return
	 */
	public List<MediaManager> getManagers() {
		return new ArrayList<MediaManager>(managers);
	}
	/*
	private void check2(Node node, String space) {
		String nodeName = node.getNodeName();
		// 各ストリームの場合
		if(nodeName.equals("tak")) {
			
		}
		else if(nodeName.equals("hls")) {
			
		}
		else if(nodeName.equals("webM")) {
			
		}
		// メディアデータ詳細の場合
		if(nodeName.equals("preset")) {
			// プリセットデータ処理(videoもしくはaudioの必要データを設定している。)
		}
		else if(nodeName.equals("video")) {
			// ビデオプロパティ
		}
		else if(nodeName.equals("audio")) {
			// オーディオプロパティ
		}
		System.out.print(space);
		logger.info(node.getNodeName());
		NamedNodeMap attributes = node.getAttributes();
		// 属性を検索
		if(attributes != null) {
			for(int i=0;i < attributes.getLength();i ++) {
				Node item = attributes.item(i);
				System.out.print(space);
				logger.info(item.getNodeName() + ":" + item.getNodeValue());
			}
		}
		// 子要素を検索
		NodeList children = node.getChildNodes();
		if(children != null) {
			for(int i=0;i < children.getLength();i ++) {
				Node item = children.item(i);
				switch(item.getNodeType()) {
				case Node.ELEMENT_NODE:
//					logger.info(item.getNodeName());
					check2(item, space + "  ");
					break;
				case Node.TEXT_NODE:
					if(item.getNodeValue() == null || "".equals(item.getNodeValue().trim())) {
						continue;
					}
					System.out.print(space);
					System.out.print("data:");
					logger.info(item.getNodeValue());
					break;
				default:
					break;
				}
			}
		}
	}
	private void check2_bku(Node node, String space) {
		System.out.print(space);
		logger.info(node.getNodeName());
		NamedNodeMap attributes = node.getAttributes();
		// 属性を検索
		if(attributes != null) {
			for(int i=0;i < attributes.getLength();i ++) {
				Node item = attributes.item(i);
				System.out.print(space);
				logger.info(item.getNodeName() + ":" + item.getNodeValue());
			}
		}
		// 子要素を検索
		NodeList children = node.getChildNodes();
		if(children != null) {
			for(int i=0;i < children.getLength();i ++) {
				Node item = children.item(i);
				switch(item.getNodeType()) {
				case Node.ELEMENT_NODE:
//					logger.info(item.getNodeName());
					check2(item, space + "  ");
					break;
				case Node.TEXT_NODE:
					if(item.getNodeValue() == null || "".equals(item.getNodeValue().trim())) {
						continue;
					}
					System.out.print(space);
					System.out.print("data:");
					logger.info(item.getNodeValue());
					break;
				default:
					break;
				}
			}
		}
	}*/
}
