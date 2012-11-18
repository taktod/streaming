package com.ttProject.process;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.Setting;
import com.ttProject.segment.m3u8.M3u8Manager;
import com.ttProject.streaming.IMediaPacket;
import com.ttProject.streaming.IMediaPacketManager;
import com.ttProject.streaming.mpegts.MpegtsPacketManager;

/**
 * それぞれのプロセスの管理を実行します。
 * @author taktod
 */
public class ProcessHandler {
	private final Logger logger = LoggerFactory.getLogger(ProcessHandler.class);
	// アクセスキー
	private final String key;
	// 設定クオリティ
	private final Quality quality; // 1:high 2:middle 3:low
	private final ConvertProcessHandler handler;
	// 出力対象
	private final String format;
	// 接続に利用するポート番号
	private final int port;
	// クオリティー指定
	public enum Quality {
		High,Middle,Low
	}
	/**
	 * コンストラクタ
	 * @param command 動作コマンド
	 * @param key 動作キー
	 */
	public ProcessHandler(int port, Quality quality, String format, ConvertProcessHandler convertProcessHandler) {
		this.handler = convertProcessHandler;
		this.port     = port;
		this.key      = UUID.randomUUID().toString();
		this.quality  = quality;
		this.format   = format;
	}
	/**
	 * アクセスキーを取得する。
	 * @return
	 */
	public String getKey() {
		return key;
	}
	/**
	 * 指定プロセスを実行する。
	 */
	public void executeProcess() {
		try {
			Setting setting = Setting.getInstance();
			// コマンドを生成します。
			StringBuilder command = new StringBuilder();
			command.append(setting.getProcessCommand()).append(" ");
			command.append(port).append(" ");
			command.append(key).append(" | ");
			command.append("avconv -i - ");
			if(handler.getAudioFlg()) {
				command.append("-acodec libmp3lame -ac 2 -ar 44100 -ab 96k ");
			}
			else {
				command.append("-an ");
			}
			if(handler.getVideoFlg()) {
				command.append("-vcodec libx264 -profile:v main ");
				switch(quality) {
				case High:
					command.append("-s 320x240 -qmin 10 -qmax 31 ");
					break;
				case Middle:
					command.append("-s 320x240 -qmin 30 -qmax 51 ");
					break;
				default:
				case Low:
					command.append("-s 160x120 -qmin 30 -qmax 51 ");
					break;
				}
				command.append("-crf 20.0 -level 13 -coder 0 -async 4 -bf 0 -cmp +chroma ");
				command.append("-partitions -parti8x8+parti4x4+partp8x8+partp4x4-partb8x8 ");
				command.append("-me_method hex -subq 5 -g 20 -r 15 ");
			}
			command.append("-f ");
			command.append(format);
			command.append(" -");
			logger.info(command.toString());
			ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command.toString());
			// 環境変数に追加データがある場合は、ここで処理する。
			Map<String, String> env = processBuilder.environment();
			Map<String, String> envExtra = setting.getEnvExtra();
			for(String key : envExtra.keySet()) {
				String envData = env.get(key);
				if(envData == null || "".equals(envData)) {
					envData = envExtra.get(key);
				}
				else {
					envData += ":" + envExtra.get(key);
				}
				env.put(key, envData);
			}
			Process process = processBuilder.start();
			// 読み込みデータはthreadでうけとって、出力にまわす必要あり。
			final ReadableByteChannel outputChannel = Channels.newChannel(process.getInputStream());
			final IMediaPacketManager packetManager = new MpegtsPacketManager();
			final M3u8Manager m3u8Manager = new M3u8Manager(setting.getPath() + handler.getName() + "_" + quality.toString() + ".m3u8");
			final File tsFile = new File(setting.getPath() + handler.getName() + "_" + quality.toString());
			Thread t = new Thread(new Runnable() {
				private int counter = 0;
				@Override
				public void run() {
					try {
						while(true) {
							// この部分のByteBufferのサイズを大きくすると動作がよくなる。でもメモリーを食う
							ByteBuffer buffer = ByteBuffer.allocate(65536);
							outputChannel.read(buffer);
							buffer.flip();
							List<IMediaPacket> packets = packetManager.getPackets(buffer);
							for(IMediaPacket packet : packets) {
								counter ++;
								String targetFile = tsFile.getAbsolutePath() + "_" + counter + packetManager.getExt();
								String targetHttp = tsFile.getName() + "_" + counter + packetManager.getExt();
								packet.writeData(targetFile, false);
								m3u8Manager.writeData(targetFile, targetHttp, packet.getDuration(), counter, false);
							}
						}
					}
					catch (Exception e) {
						logger.error("プロセスからの出力の取得エラー", e);
					}
				}
			});
			// threadを起動しておく。
			t.start();
		}
		catch (Exception e) {
			logger.error("子プロセス起動時に例外が発生しました。", e);
		}
	}
}
