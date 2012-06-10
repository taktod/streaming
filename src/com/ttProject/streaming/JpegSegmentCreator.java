package com.ttProject.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * creatorはすべて適当なディレクトリにいったんデータを書き出して、それを全体で共有する形ですすめる。
 * TODO どのcreatorもとりあえずパスをきめて、パスの中身をクリアする。
 * カウンターを0に戻してそこから処理始める
 * んで、必要なメディアファイルの出力を順につづけていけば、それでOK
 * 適当なタイミングでアップロードその他の処理を実行するイベントをキックしてやって、ファイルを対象のサーバーにあげていけば、それでいいはず。
 * 
 * とりあえずパスの作成と、その中身のクリア処理はつくっていっていいはず。
 * このあたりの処理は全体で共通なので、abstructクラスをつくって、そこに記述してもいいはず。
 * @author taktod
 */
public class JpegSegmentCreator extends SegmentCreator{
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(JpegSegmentCreator.class);
	private static int duration;
	private static String tmpPath;
	@Override
	public void setDuration(int value) {
		duration = value;
	}
	@Override
	protected int getDuration() {
		return duration;
	}
	@Override
	public void setTmpPath(String path) {
		if(path.endsWith("/")) {
			tmpPath = path;
		}
		else {
			tmpPath = path + "/";
		}
	}
	@Override
	protected String getTmpPath() {
		return tmpPath;
	}
	@Override
	public void close() {
	}
}
