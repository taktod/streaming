package com.ttProject.segment.m3u8;

/**
 * 保持エレメント
 * @author taktod
 */
public class M3u8Element {
	private String file;
	private String http;
	private String info;
	private int index;
	private boolean isFirst;
	/**
	 * コンストラクタ
	 */
	public M3u8Element(String file, String http, float duration, int index) {
		this.file = file;
		this.http = http;
		this.info = "#EXTINF:" + duration;
		this.index = index;
		this.isFirst = index == 1;
	}
	public String getFile() {
		return file;
	}
	public String getHttp() {
		return http;
	}
	public String getInfo() {
		return info;
	}
	public int getIndex() {
		return index;
	}
	public boolean isFirst() {
		return isFirst;
	}
}
