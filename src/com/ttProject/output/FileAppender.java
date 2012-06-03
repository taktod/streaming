package com.ttProject.output;

import java.io.FileOutputStream;

/**
 * 特定の場所にファイルを書き出していく追加動作
 * このクラスオブジェクトを各creatorに持たせて、creatorから渡されたbyte配列データを指示にしたがって、書き込んでいく。
 * とする。
 * とりあえずまとまった量が完成したら書き込みを実施する。appendモードは利用しない
 * とする。
 * まぁoutputの部分は正直cronでもいいと思ってる。
 * @author taktod
 */
public class FileAppender implements IDataAppender{
	// beanベースの設定
	/** 出力パス定義 */
	private static String path; // 実際の出力はpath/名前/ファイルという形になります。
	/** 出力prefix定義 */
	private static String prefix; // 出力prefix設定
	/** 連番にするかどうか */
	private static boolean isSequence;
	/**
	 * 出力パス設定
	 * @param path
	 */
	public void setPath(String path) {
		FileAppender.path = path;
	}
	/**
	 * 出力prefix設定
	 * @param prefix
	 */
	public void setPrefix(String prefix) {
		FileAppender.prefix = prefix;
	}
	/**
	 * 連番にするかどうか
	 * @param sequence
	 */
	public void setSequence(Boolean sequence) {
		isSequence = sequence;
	}
	// 内部設定
	/** 動作名 */
	private String name;
	/** 拡張子 */	
	private String ext;
	/** カウンター */
	private int count = 0;
	/**
	 * コンストラクタ
	 */
	public FileAppender(String name, String ext) {
		count = 0;
		this.name = name;
		this.ext  = ext;
	}
	/**
	 * ファイルを開く
	 */
	private String getFilePath() {
		StringBuilder targetFile = new StringBuilder();
		targetFile.append(path).append("/");
		targetFile.append(name).append("/");
		targetFile.append(prefix);
		if(isSequence) {
			targetFile.append("-").append(count);
		}
		targetFile.append(".").append(ext);
		return targetFile.toString();
	}
	/**
	 * byteデータを書き込む
	 * @param data
	 * @param size
	 */
	@Override
	public void writeBytes(byte[] data, int size) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(getFilePath());
			fos.write(data);
		}
		catch (Exception e) {
		}
		finally {
			if(fos != null) {
				try {
					fos.close();
				}
				catch (Exception e) {
				}
				fos = null;
			}
		}
	}
	/**
	 * 文字列を書き込む
	 * @param data
	 */
	@Override
	public void writeText(String data) {
		
	}
}
