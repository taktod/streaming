package com.ttProject.flazr.ex;

import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.client.ClientHandler;
import com.flazr.rtmp.client.ClientOptions;
import com.flazr.rtmp.message.Command;
import com.flazr.rtmp.message.MessageType;
import com.ttProject.flazr.TranscodeWriter;

/**
 * クライアントの動作を修正
 * publish unpublish playをコントロールしたい。これをコントロールすることで、放送が再開されたり、別のユーザーが放送したときにもうまくコンバートできるようにしておく。
 * @author taktod
 */
@ChannelPipelineCoverage("one")
public class ClientHandlerEx extends ClientHandler {
	private static final Logger logger = LoggerFactory.getLogger(ClientHandlerEx.class);
	private final ClientOptions options;
	/**
	 * コンストラクタ
	 * @param options
	 */
	public ClientHandlerEx(ClientOptions options) {
		super(options);
		this.options = options;
	}
	/**
	 * メッセージ取得動作をHook
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) {
		final RtmpMessage message = (RtmpMessage)me.getMessage();
		if(message.getHeader().getMessageType() == MessageType.COMMAND_AMF0
		|| message.getHeader().getMessageType() == MessageType.COMMAND_AMF3) {
			Command command = (Command)message;
			String name = command.getName();
			if("onStatus".equals(name)) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				final Map<String, String> temp = (Map)command.getArg(0);
				final String code = (String)temp.get("code");
				final TranscodeWriter transcodeWriter = (TranscodeWriter)options.getWriterToSave();
				// "NetStream.Play.Start"
				if("NetStream.Play.UnpublishNotify".equals(code)) {
					// 放送がとまったとき
					// すでに動作しているsegmentCreatorは必要なくなるので停止する。
					logger.info("放送停止");
					// 変換を停止する。
				}
				else if("NetStream.Play.Start".equals(code)) {
					logger.info("視聴開始");
					// エンコード変換の準備をしておく。
				}
				else if("NetStream.Play.PublishNotify".equals(code)) {
					// 放送が開始されたとき
					// あたらしい放送開始にあわせて、segmentCreatorを動作するように手配する
					logger.info("放送開始");
					// エンコード変換の準備をしておく。
				}
			}
		}
		super.messageReceived(ctx, me);
	}
}
