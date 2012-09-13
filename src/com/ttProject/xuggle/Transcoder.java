package com.ttProject.xuggle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xuggleでメディアデータのコンバートを実行します。
 * ただし動作は大幅に変更するかも・・・
 * 複数コンバートをいっきに実行するので、thread分けする可能性があるため。
 * @author taktod
 */
public class Transcoder implements Runnable {
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(Transcoder.class);
	
	@Override
	public void run() {
	}
}
