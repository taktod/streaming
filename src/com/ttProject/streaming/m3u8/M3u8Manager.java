package com.ttProject.streaming.m3u8;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.Setting;

/**
 * m3u8のデータを処理するためのマネージャー
 * @author taktod
 */
public class M3u8Manager {
	private final Logger logger = LoggerFactory.getLogger(M3u8Manager.class);
	private final String header;
	private final String allowCache;
	private final String targetDuration;
	private final List<M3u8Element> elementData;
	private final String m3u8File;
	private final Integer limit = 3; // limitの設定は固定3でいいはずだが、動作検証で全データ出力させてみたいときもあるので、注意が必要。
	/**
	 * コンストラクタ
	 * @param m3u8File
	 */
	public M3u8Manager(String m3u8File) {
		header         = "#EXTM3U";
		allowCache     = "#EXT-X-ALLOW-CACHE:NO";
		targetDuration = "#EXT-X-TARGETDURATION:" + Setting.getInstance().getDuration();
		this.m3u8File  = m3u8File;
		if(limit != null) {
			elementData = new ArrayList<M3u8Element>();
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
				int startPos = index - limit < 0 ? 1 : index - limit + 1;
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(m3u8File, false)));
				pw.println(header);
				pw.println(allowCache);
				pw.println(targetDuration);
				pw.print("#EXT-X-MEDIA-SEQUENCE:");
				pw.println(startPos);
				if(startPos == 1) {
					pw.println("#EXT-X-DISCONTINUITY");
				}
				// 内容を書き込む
				for(M3u8Element data : elementData) {
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
	}
}
