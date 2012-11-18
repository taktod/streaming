package com.ttProject.process;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 子プロセスにデータを送信するサーバー
 * @author taktod
 */
public class ProcessServer {
	private final Logger logger = LoggerFactory.getLogger(ProcessServer.class);
	/** つながっているクライアントのchannelデータ */
	private final Set<Channel> channels = new HashSet<Channel>();
	private final Channel serverChannel;
	private final ServerBootstrap bootstrap;
	private Set<String> keyList; // アクセスしているプロセスのキーリスト
	/**
	 * コンストラクタ
	 */
	public ProcessServer(int port) {
		bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("handler", new ProcessServerHandler());
				return pipeline;
			}
		});
		serverChannel = bootstrap.bind(new InetSocketAddress(port));
	}
	/**
	 * キーリストを登録する。
	 * @param keyList
	 */
	public void setKeyList(Set<String> keyList) {
		this.keyList = keyList;
	}
	/**
	 * データを送る。
	 */
	public void sendData(ChannelBuffer buffer) {
		// データのバッファーを準備する
		synchronized(channels) {
			for(Channel channel : channels) {
				channel.write(buffer);
			}
		}
	}
	/**
	 * サーバーを停止する。
	 */
	public void closeServer() {
		synchronized (channels) {
			// 子プロセスの切断を実行しておく。
			for(Channel channel : channels) {
				channel.close();
			}
			channels.clear();
		}
		// サーバーを閉じます。
		ChannelFuture future = serverChannel.close();
		future.awaitUninterruptibly();
		bootstrap.releaseExternalResources();
	}
	/**
	 * メッセージ処理用のクラス
	 * @author taktod
	 */
	@ChannelPipelineCoverage("one")
	private class ProcessServerHandler extends SimpleChannelUpstreamHandler {
		/**
		 * 接続したときの動作
		 */
		@Override
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			synchronized(channels) {
				channels.add(e.getChannel());
			}
			super.channelConnected(ctx, e);
		}
		/**
		 * メッセージ取得動作
		 */
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			if(keyList.size() == 0) {
				return;
			}
			Object message = e.getMessage();
			if(message instanceof ChannelBuffer) {
				// 接続時にアクセストークンをうけとる。
				ByteBuffer buffer = ((ChannelBuffer) message).toByteBuffer();
				byte[] data = new byte[buffer.remaining()];
				buffer.get(data);
				keyList.remove(new String(data).intern());
				// 全プロセスからアクセスが完了したら、プロセスを実行する。
				if(keyList.size() == 0) {
					logger.info("子プロセスから全接続をうけとったので、処理開始");
					synchronized(keyList) {
						keyList.notifyAll(); // 実行を許可する。
					}
				}
			}
			super.messageReceived(ctx, e);
		}
	}
}
