package com.ttProject;

import java.util.HashMap;
import java.util.Map;

/**
 * とりあえず、jsonファイルあたりでなんとかしておきたいところ。
 * 動画に必要なもの。
 * javaの起動コマンド
 * java -cp test.jar:lib/netty-3.1.5.GA.jar com.ttProject.process.ProcessEntry [port] [key]
 * processに必要な環境変数
 * PATHとLD_LIBRARY_PATHとか？(妙なインストール方法をとっていなければ変更する必要なし。)
 * 変換プログラム指定
 * avconv or ffmpeg
 * 変換コマンド
 * 音声:-acodec libmp3lame -ac 2 -ar 44100 -b:a 96k
 * 映像:-vcodec libx264 -profile:v main -s 320x240 -qmin 10 -qmax 31 -crf 20.0 -level 13...
 * 音声だけ、映像だけというデータもありうるので、片方だけの動作も見越しておいた方がいいはず。
 * 出力ファイルの出す場所。
 * ~/Sites/hls/とか？
 * 
 * 設定を保持するクラス
 * @author taktod
 */
public class Setting {
	private final static Setting instance = new Setting();
	private final int duration;
	private final String processCommand;
	private final String path; // 出力パス
	private final String userHome;
	private final Map<String, String> envExtra = new HashMap<String, String>();
	private Setting() {
		duration = 5; // 分割は2秒ごとにしておく。
		userHome = System.getProperty("user.home");
		// process用のコマンドとその出力のデータをいれておく。
		processCommand = "java -cp test.jar:lib/netty-3.1.5.GA.jar com.ttProject.process.ProcessEntry ";
		// processCommandのインスタンスをいくつか準備しておく。
		path = userHome + "/Sites/hls/";
		// 拡張環境変数
		envExtra.put("PATH", userHome + "/bin/bin");
		envExtra.put("DYLD_LIBRARY_PATH", userHome + "/bin/lib");
	}
	/**
	 * インスタンス取得
	 * @return
	 */
	public static synchronized Setting getInstance() {
		return instance;
	}
	public String getProcessCommand() {
		return processCommand;
	}
	public int getDuration() {
		return duration;
	}
	public String getPath() {
		return path;
	}
	public Map<String, String> getEnvExtra() {
		return envExtra;
	}
}
