package com.ttProject.segment.m3u8;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.Setting;

/**
 * m3u8のデータを処理するためのマネージャー
 * @author taktod
 */
public class M3u8Manager {
	private static final Map<String, M3u8Manager> managerMap = new HashMap<String, M3u8Manager>();
	private final Logger logger = LoggerFactory.getLogger(M3u8Manager.class);
	private final String header;
	private final String allowCache;
	private final String targetDuration;
	private List<M3u8Element> elementData;
	private Integer num;
	// このあたりのstaticデータの管理をするやつがm3u8Managerということにしておきたい。
//	private static final Map<String, List<M3u8Element>> elementDataMap = new HashMap<String, List<M3u8Element>>();
//	private static final Map<String, Integer> numMap = new HashMap<String, Integer>();
	private final String m3u8File;
	private final Integer limit = 3; // limitの設定は固定3でいいはずだが、動作検証で全データ出力させてみたいときもあるので、注意が必要。
	private long lastUpdate;
	public static M3u8Manager getInstance(String m3u8File) {
		M3u8Manager instance = managerMap.get(m3u8File);
		if(instance == null) {
			instance = new M3u8Manager(m3u8File);
			managerMap.put(m3u8File, instance);
		}
		return instance;
	}
	/**
	 * コンストラクタ
	 * @param m3u8File
	 */
	private M3u8Manager(String m3u8File) {
		header         = "#EXTM3U";
		allowCache     = "#EXT-X-ALLOW-CACHE:NO";
		targetDuration = "#EXT-X-TARGETDURATION:" + Setting.getInstance().getDuration();
		this.m3u8File  = m3u8File;
		if(limit != null) {
//			elementData = elementDataMap.get(m3u8File);
//			if(elementData == null) {
				elementData = new ArrayList<M3u8Element>();
//			}
//			elementDataMap.put(m3u8File, elementData);
//			num = numMap.get(m3u8File);
//			if(num == null) {
				num = 0;
//			}
//			numMap.put(m3u8File, num);
		}
		else {
			// ファイルに先頭の情報を書き込む
			elementData = null;
			try {
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(m3u8File, false)));
				pw.println(header);
				pw.println(allowCache);
				pw.println(targetDuration);
				pw.close();
				pw = null;
			}
			catch (Exception e) {
				logger.error("m3u8ファイル書き込み中にエラー", e);
			}
		}
		lastUpdate = System.currentTimeMillis();
	}
	/**
	 * データの書き込み処理
	 * @param target
	 * @param http
	 * @param duration
	 * @param index
	 * @param endFlg
	 */
	public void writeData(String target, String http, int duration, int index, boolean endFlg) {
		M3u8Element element = new M3u8Element(target, http, duration, index);
		if(limit != null) {
			// limitが設定されている場合は、m3u8上のデータ量がきまっている。
			elementData.add(element); // エレメントを追加する。
			if(elementData.size() > limit) {
				// elementデータよりサイズが大きい場合は必要のないデータがあるので、先頭のデータを落とす
				M3u8Element removedData = elementData.remove(0);
				// いらなくなったファイルは削除する必要があるので、消す
				File deleteFile = new File(removedData.getFile());
				if(deleteFile.exists()) {
					// 削除しておく。
					deleteFile.delete();
				}
			}
			try {
//				int startPos = index - limit < 0 ? 1 : index - limit + 1;
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(m3u8File, false)));
				pw.println(header);
				pw.println(allowCache);
				pw.println(targetDuration);
				pw.print("#EXT-X-MEDIA-SEQUENCE:");
				num ++;
//				numMap.put(m3u8File, num);
				pw.println(num);
				// 内容を書き込む
				for(M3u8Element data : elementData) {
					if(data.isFirst()) {
//						pw.println("#EXT-X-ENDLIST");// x endlistをいれてしまうと、動画がおわってしまう。
						pw.println("#EXT-X-DISCONTINUITY");
					}
					pw.println(data.getInfo());
					pw.println(data.getHttp());
				}
				if(endFlg) {
					pw.println("#EXT-X-ENDLIST");
				}
				pw.close();
				pw = null;
			}
			catch (Exception e) {
				logger.error("m3u8ファイル書き込み中にエラー", e);
			}
		}
		else {
			try {
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(m3u8File, true)));
				pw.println(element.getInfo());
				pw.println(element.getHttp());
				if(endFlg) {
					pw.println("#EXT-X-ENDLIST");
				}
				pw.close();
				pw = null;
			}
			catch (Exception e) {
				logger.error("m3u8ファイル書き込み中にエラー", e);
			}
		}
		lastUpdate = System.currentTimeMillis();
	}
	public static void fillEmptySpace() {
		
	}
}
