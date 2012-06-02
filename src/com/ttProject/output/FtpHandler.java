package com.ttProject.output;

/**
 * ftpの接続を扱うHandler
 * @author taktod
 */
public class FtpHandler {
	/** 接続サーバー */
	@SuppressWarnings("unused")
	private String server;
	/** 接続ユーザー */
	@SuppressWarnings("unused")
	private String userName;
	/** 接続パスワード */
	@SuppressWarnings("unused")
	private String password;
	/**
	 * サーバーを設定する。
	 * @param server
	 */
	public void setServer(String server) {
		this.server = server;
	}
	/**
	 * ftpログインユーザー名を設定する。
	 * @param userName
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * ftpログインパスワードを設定する。
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * 書き込みを実行する。
	 * @param data
	 * @param size
	 */
	public void writeByteData(byte[] data, int size) {
		
	}
}
