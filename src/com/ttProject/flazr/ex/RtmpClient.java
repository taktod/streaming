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
	private static final Logger logger = LoggerFactory.getLogger(RtmpClient.class);

	public static void main(String[] args) {
		final ClientOptions options = new ClientOptions();
		if(!options.parseCli(args)) {
			return;
		}
		Utils.printlnCopyrightNotice();
		final int count = options.getLoad();
		if(count == 1 && options.getClientOptionsList() == null) {
			// 単一のみ
			options.setWriterToSave(new TranscodeWriter(options.getStreamName()));
			connect(options);
			return;
		}
		logger.error("単一プロセスのみ許可されています。");
	}
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
	private static ClientBootstrap getBootstrap(final Executor executor, final ClientOptions options) {
		final ChannelFactory factory = new NioClientSocketChannelFactory(executor, executor);
		final ClientBootstrap bootstrap = new ClientBootstrap(factory);
		bootstrap.setPipelineFactory(new ClientPipelineFactoryEx(options));
		bootstrap.setOption("tcpNoDelay" , true);
		bootstrap.setOption("keepAlive", true);
		return bootstrap;
	}
}
