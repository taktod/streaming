package com.ttProject.xuggle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.xuggle.flv.FlvManager;
import com.xuggle.xuggler.IPacket;

/**
 * Xuggleでメディアデータのコンバートを実行します。
 * ただし動作は大幅に変更するかも・・・
 * 複数コンバートをいっきに実行するので、thread分けする可能性があるため。
 * @author taktod
 */
public class Transcoder implements Runnable {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(Transcoder.class);
	/** 動作定義 */
	private volatile boolean keepRunning = true;
	@Override
	public void run() {
		try {
			// 読み込み用のコンテナをオープン
			ConvertManager convertManager = ConvertManager.getInstance();
			FlvManager flvManager = convertManager.getFlvManager();
			flvManager.openInputContainer();
			// 変換を実行する。
			transcode();
		}
		catch (Exception e) {
			logger.error("変換がおわってしまいました。", e);
		}
		finally {
			logger.info("処理がおわりました。");
			// 全処理を閉じる。
			closeAll();
		}
	}
	/**
	 * 停止します。
	 */
	public void close() {
		keepRunning = false;
	}
	/**
	 * 全動作を止めます。
	 */
	private void closeAll() {
		// コンテナとか閉じる(変換処理が完了したときに動作します。)
	}
	/**
	 * 変換の中枢動作
	 */
	private void transcode() {
		logger.info("変換を開始します。");
		ConvertManager convertManager = ConvertManager.getInstance();
		FlvManager flvManager = convertManager.getFlvManager();
		IPacket packet = IPacket.make();
		while(keepRunning) {
			// コンバートを開始する。
			keepRunning = flvManager.execute(packet);
		}
	}
}
