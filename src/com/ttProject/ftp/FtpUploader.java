package com.ttProject.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ftpによるデータのアップローダー
 * Takstreamingで利用するファイルをアップロードする処理
 * @author taktod
 */
public class FtpUploader implements Runnable {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(FtpUploader.class);
	private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	
	/**
	 * queueを追加します。指定したファイルを対象にアップロードします。
	 * @param data
	 */
	public void putNewQueue(String data) {
		queue.add(data);
	}
	private FTPClient client;
	/**
	 * 動作定義 (queueを常に関ししておき、データがはいったら、処理をする。)
	 */
	@Override
	public void run() {
		String target = null;
		try {
			while(loopFlg) {
				// 可能な限りループさせる。
				target = queue.take(); // フルパスでデータがはいっている。
				while(true) {
					try {
						// ftpの確認を実施する。
						checkFtp();
						// ファイルをアップロードする。
						File f = new File(target);
						if(!f.isFile()) {
							break;
						}
						if("index.ftl".equals(f.getName())) {
							client.setFileType(FTP.ASCII_FILE_TYPE);
						}
						else {
							client.setFileType(FTP.BINARY_FILE_TYPE);
						}
						FileInputStream fis = new FileInputStream(f);
						client.storeFile(f.getName(), fis);
						// うまく処理できたら、whileから抜ける。
						break;
					}
					catch (FTPConnectionClosedException e) {
						// 失敗した場合は、もう一度ループに移る
						logger.error("失敗", e);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * ftpがきちんと使えるか確認する。
	 */
	private void checkFtp() throws IOException, SocketException, FTPConnectionClosedException {
		if(client == null || !client.isConnected() || !client.isAvailable()) {
			if(client != null) {
				if(client.isAvailable()) {
					client.logout();
				}
				if(client.isConnected()) {
					client.disconnect();
				}
			}
			client = new FTPClient();
			client.connect("server.xxxxxx.com");
			int reply = client.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply)) {
				logger.error("接続失敗");
				return;
			}
			if(!client.login("ftpUser", "ftpPass")) {
				logger.error("ログイン失敗");
				return;
			}
			if(!client.changeWorkingDirectory("/public_html/tak/test/")) {
				logger.error("ディレクトリ移動失敗");
				return;
			}
			// passiveモードにやっておく。
			client.enterLocalPassiveMode();
		}
	}
	private boolean loopFlg = true;
	/**
	 * 止める。
	 */
	public void close() {
		loopFlg = false;
	}
}
