package com.ttProject.streaming.iphone;

/**
 * Tsファイルベースのm3u8による、HttpLiveStreaming
 * @author taktod
 */
public class TsM3u8Model {
	@SuppressWarnings("unused")
	private static String path;
	public static void setPath(String path) {
		TsM3u8Model.path = path;
	}
	@SuppressWarnings("unused")
	private static String urlPath;
	public static void setUrlPath(String urlPath) {
		TsM3u8Model.urlPath = urlPath;
	}
}
