package com.ttProject.xuggle.red5;

/**
 * 読み込みと書き込み用のインターフェイス。
 * 書き込みを実行するとwriteが
 * 読み込みが実行されるとreadが走るみたいです。
 * @author taktod
 */
public interface IRTMPEventIOHandler {
	Red5Message read() throws InterruptedException;
	void write(Red5Message msg) throws InterruptedException;
}
