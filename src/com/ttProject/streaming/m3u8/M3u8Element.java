package com.ttProject.streaming.m3u8;

/**
 * 保持エレメント
 * @author taktod
 */
public class M3u8Element {
	private String file;
	private String http;
	private String info;
	private int index;
	/**
	 * コンストラクタ
	 */
	public M3u8Element(String file, String http, int duration, int index) {
		this.file = file;
		this.http = http;
		this.info = "#EXTINF:" + duration;
		this.index = index;
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
}
