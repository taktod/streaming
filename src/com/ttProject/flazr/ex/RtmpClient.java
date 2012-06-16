package com.ttProject.flazr.ex;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.rtmp.client.ClientOptions;
import com.flazr.util.Utils;
import com.ttProject.flazr.TranscodeWriter;

/**
 * flazrのエントリー動作
 * 99%flazrのソースのコピー
 * @author taktod
 */
public class RtmpClient {	
	/** ロガー */
	private static final Logger logger = LoggerFactory.getLogger(RtmpClient.class);

	/**
	 * 動作のメインエントリー
	 * @param args
	 */
	public static void main(String[] args) {
		final ClientOptions options = new ClientOptions();
		if(!options.parseCli(args)) {
			return;
		}
		Utils.printlnCopyrightNotice();
		final int count = options.getLoad();
		if(count == 1 && options.getClientOptionsList() == null) {
			// 単一のみ
			// writerに独自のを設定することで、出力動作をコントロールします。(今回はファイル出力をオーバーライドして、変換につなげます。)
			options.setWriterToSave(new TranscodeWriter(options.getStreamName()));
			connect(options);
			return;
		}
		// 複数プロセス動作は今回必要ないので、許可していません。複数プロセス動作したかったら、複数プロセスをつくればいいと思う。
		logger.error("単一プロセスのみ許可されています。");
	}
	/**
	 * 接続動作
	 * @param options
	 */
	public static void connect(final ClientOptions options) {  
		final ClientBootstrap bootstrap = getBootstrap(Executors.newCachedThreadPool(), options);
		final ChannelFuture future = bootstrap.connect(new InetSocketAddress(options.getHost(), options.getPort()));
		future.awaitUninterruptibly();
		if(!future.isSuccess()) {
			// future.getCause().printStackTrace();
			logger.error("error creating client connection: {}", future.getCause().getMessage());
		}
		future.getChannel().getCloseFuture().awaitUninterruptibly(); 
		bootstrap.getFactory().releaseExternalResources();
	}
	/**
	 * 起動作成
	 * @param executor
	 * @param options
	 * @return
	 */
	private static ClientBootstrap getBootstrap(final Executor executor, final ClientOptions options) {
		final ChannelFactory factory = new NioClientSocketChannelFactory(executor, executor);
		final ClientBootstrap bootstrap = new ClientBootstrap(factory);
		// clientPipelineFactoryをオーバーライドすることで、独自定義動作させます。
		bootstrap.setPipelineFactory(new ClientPipelineFactoryEx(options));
		bootstrap.setOption("tcpNoDelay" , true);
		bootstrap.setOption("keepAlive", true);
		return bootstrap;
	}
}
