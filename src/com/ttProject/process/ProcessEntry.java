package com.ttProject.process;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * 外部プロセスとして立ち上げ標準出力として、ffmpeg(等)にデータを流すプログラム
 * 一応できあがったつもり。
 * 余計なライブラリを読み込む必要をなくすため、ロガーはなし。
 * @author taktod
 */
public class ProcessEntry {
	/** 出力チャンネル */
	private WritableByteChannel stdout;
	/** 設定アクセスキー */
	private final String key;
	/**
	 * mainエントリー
	 * @param args
	 */
	public static void main(String[] args) {
		// args[0] : 接続ポート(ポートの指定以外は必要なし。)
		// args[1] : アクセスキー
		if(args == null || args.length != 2) {
			System.err.println("引数の数がおかしいです。");
			return;
		}
		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		}
		catch (Exception e) {
			System.err.println("入力数値が解釈できませんでした。:" + args[0]);
			return;
		}
		String key = args[1];
		new ProcessEntry(port, key);
	}
	/**
	 * コンストラクタ
	 */
	public ProcessEntry(int port, String key) {
		this.key = key;
		stdout = java.nio.channels.Channels.newChannel(System.out);
		// 動作Threadをcacheする方向でつかう。(一番すくない数で動作することを期待)
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("handler", new ProcessClientHandler());
				return pipeline;
			}
		});
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		ChannelFuture future = bootstrap.connect(new InetSocketAddress(port));
		future.awaitUninterruptibly(); // ここで終わるまでHookがかかる。
		if(future.isSuccess()) {
			future.getChannel().getCloseFuture().awaitUninterruptibly();
		}
		bootstrap.releaseExternalResources();
	}
	/**
	 * クライアント動作を定義するクラス
	 * @author taktod
	 */
	@ChannelPipelineCoverage("one")
	private class ProcessClientHandler extends SimpleChannelUpstreamHandler {
		/**
		 * メッセージをサーバーから受け取ったときの動作
		 */
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			// データをうけとったらそのデータを標準出力に吐く
			ByteBuffer buffer = ((ChannelBuffer)e.getMessage()).toByteBuffer();
			stdout.write(buffer);
		}
		/**
		 * 接続したときの処理
		 */
		@Override
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			// プロセス起動時に設定されたキーをサーバーに送る。
			ChannelBuffer keyBuffer = ChannelBuffers.buffer(key.length());
			keyBuffer.writeBytes(key.getBytes());
			e.getChannel().write(keyBuffer);
		}
	}
}
