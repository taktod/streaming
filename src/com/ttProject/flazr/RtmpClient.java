package com.ttProject.flazr;

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
import com.flazr.rtmp.client.ClientPipelineFactory;
import com.flazr.util.Utils;

/**
 * flazrのエントリー動作
 * 99%flazrのソースのコピー
 * @author taktod
 */
public class RtmpClient {	
	private static final Logger logger = LoggerFactory.getLogger(RtmpClient.class);

	public static void main(String[] args) {
		System.out.println();
		final ClientOptions options = new ClientOptions();
		if(!options.parseCli(args)) {
			return;
		}
		Utils.printlnCopyrightNotice();
		final int count = options.getLoad();
		if(count == 1 && options.getClientOptionsList() == null) {
			// TODO 単一接続だけ面倒をみる場合はここで、接続に対するflvWriterを書いてやればよい。
			options.setWriterToSave(new FlvWriter(options.getStreamName()));
			connect(options);
			return;
		}
		//======================================================================
		final Executor executor = Executors.newFixedThreadPool(options.getThreads());
		if(options.getClientOptionsList() != null) {
			logger.info("file driven load testing mode, lines: {}", options.getClientOptionsList().size());
			int line = 0;
			for(final ClientOptions tempOptions : options.getClientOptionsList()) {
				line++;
				logger.info("running line #{}", line);
				for(int i = 0; i < tempOptions.getLoad(); i++) {
					final int index = i + 1;
					final int tempLine = line;
					executor.execute(new Runnable() {
						@Override public void run() {
							// TODO 複数接続で面倒をみる場合はここで、接続に対するflvWriterを個々に書いてやればよい。
							tempOptions.setWriterToSave(new FlvWriter(tempOptions.getStreamName()));
							final ClientBootstrap bootstrap = getBootstrap(executor, tempOptions);
							bootstrap.connect(new InetSocketAddress(tempOptions.getHost(), tempOptions.getPort()));
							logger.info("line #{}, spawned connection #{}", tempLine + 1, index);
						}
					});
				}
			}
			return;
		}
		//======================================================================
		final ClientBootstrap bootstrap = getBootstrap(executor, options);
		logger.info("load testing mode, no. of connections to create: {}", count);
		options.setSaveAs(null);
		for(int i = 0; i < count; i++) {
			final int index = i + 1;
			executor.execute(new Runnable() {
				@Override public void run() {                    
					bootstrap.connect(new InetSocketAddress(options.getHost(), options.getPort()));
					logger.info("spawned connection #{}", index);
				}
			});
		}
		// TODO graceful shutdown
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
		bootstrap.setPipelineFactory(new ClientPipelineFactory(options));
		bootstrap.setOption("tcpNoDelay" , true);
		bootstrap.setOption("keepAlive", true);
		return bootstrap;
	}
}
