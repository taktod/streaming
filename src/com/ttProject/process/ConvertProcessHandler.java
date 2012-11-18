package com.ttProject.process;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.process.ProcessHandler.Quality;

/**
 * コンバートするプロセスを管理
 * コンバート開始するときに、使います。
 * @author taktod
 */
public class ConvertProcessHandler {
	private final Logger logger = LoggerFactory.getLogger(ConvertProcessHandler.class);
	/** メディアの存在確認フラグ */
	private final boolean audioFlg;
	public boolean getAudioFlg() {return audioFlg;}
	private final boolean videoFlg;
	public boolean getVideoFlg() {return videoFlg;}
	private final String name;
	public String getName() {return name;}
	/** 受け渡し用のデータqueue */
	private final FlvDataQueue dataQueue;
	/** データ送信用のTHread */
	private Thread dataSendingThread = null;
	private List<ProcessHandler> processList = new ArrayList<ProcessHandler>();
	private ProcessServer server;
	/**
	 * コンストラクタ
	 * @param audioFlg 音声があるかどうか
	 * @param videoFlg 映像があるかどうか
	 */
	public ConvertProcessHandler(boolean audioFlg, boolean videoFlg, String name) {
		this.audioFlg = audioFlg;
		this.videoFlg = videoFlg;
		this.dataQueue = new FlvDataQueue();
		this.name = name;
		initialize();
	}
	/**
	 * dataQueue参照
	 * @return
	 */
	public FlvDataQueue getFlvDataQueue() {
		return this.dataQueue;
	}
	/**
	 * 初期化処理
	 */
	private void initialize() {
		try {
			// プロセスを作成する。
			final Set<String> keyList = new HashSet<String>(); // このkeyListに対してwaitをあらかじめかけておけばよい。
			int portNumber = startServer(keyList);
			// flvDataQueueからデータを送りつづけるThread
			dataSendingThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						synchronized(keyList) {
							keyList.wait();
						}
						while(true) {
							// takeでとめておくので大本のthreadにinterruptかける必要がある。
							ChannelBuffer buffer = dataQueue.getData();
							// データがきたので、データを送信します。
							server.sendData(buffer);
						}
					}
					catch (InterruptedException e) {
						logger.info("データ送信動作が止まりました。");
					}
					catch (Exception e) {
						// interruptedExceptionがthreadをとめたときにくるはず。
						logger.info("データ送信用threadが例外を吐きました。");
					}
				}
			});
			dataSendingThread.start();
			
			// 必要な数プロセスをつくっておく。
			ProcessHandler process = new ProcessHandler(portNumber, Quality.High, "mpegts", this);
			processList.add(process);
			keyList.add(process.getKey());
/*			process = new ProcessHandler(portNumber, Quality.Middle, "mpegts", this);
			processList.add(process);
			keyList.add(process.getKey());*/
			process = new ProcessHandler(portNumber, Quality.Low, "mpegts", this);
			processList.add(process);
			keyList.add(process.getKey());
//			keyList.add("hoge");

			// プロセスを起動
			for(ProcessHandler handler : processList) {
				handler.executeProcess();
			}
		}
		catch (Exception e) {
			logger.error("プロセスサーバー構築中にエラーが発生しました。", e);
		}
	}
	/**
	 * サーバーを起動する。
	 * @param keyList
	 * @return
	 */
	private int startServer(Set<String> keyList) {
		// 自信のプロセス番号を取得する。
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		// プロセス番号から、ポート番号を生成する。
		int portNumber = Integer.parseInt(bean.getName().split("@")[0]);
		ProcessServer ps = null;
		// この部分でpsをループさせることが可能っぽい。
		if(portNumber < 1000) {
			portNumber += 1000;
		}
		for(; portNumber < 65535;portNumber += 1000) {
			try {
				// 通信サーバーをつくってデータがおくれるようにしておく。
				ps = new ProcessServer(portNumber); // サーバーをたてる
				break;
			}
			catch (Exception e) {
				// ここで例外がでるのは想定内
				;
			}
		}
		if(portNumber > 65535) {
			logger.error("ローカルサーバーをたてることができませんでした。");
			System.exit(0);
		}
		ps.setKeyList(keyList);
		server = ps;
		return portNumber;
	}
	/**
	 * 停止します。
	 */
	public void close() {
		// 中途停止いれてとめる。
		dataSendingThread.interrupt();
		// queueを閉じておく。
		dataQueue.close();
		// プロセスを殺す
		server.closeServer();
	}
}
