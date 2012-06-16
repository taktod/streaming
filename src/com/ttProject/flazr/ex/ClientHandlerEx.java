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
	/** 動作ロガー */
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ClientHandlerEx.class);
	/** オプションデータ保持 */
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
				// この処理命令の動作はred5 wowza fmsで動作が若干違うので注意が必要。
				if("NetStream.Play.UnpublishNotify".equals(code)) {
//					logger.info("放送停止");
					// すでに動作しているsegmentCreatorは必要なくなるので停止する。
					// 変換を停止する。
					transcodeWriter.onUnpublish();
				}
				else if("NetStream.Play.Start".equals(code)) {
//					logger.info("視聴開始");
					// エンコード変換の準備をしておく。
					transcodeWriter.onPublish();
				}
				else if("NetStream.Play.PublishNotify".equals(code)) {
//					logger.info("放送開始");
					// あたらしい放送開始にあわせて、segmentCreatorを動作するように手配する
					// エンコード変換の準備をしておく。
					transcodeWriter.onPublish();
				}
			}
		}
		super.messageReceived(ctx, me);
	}
}
