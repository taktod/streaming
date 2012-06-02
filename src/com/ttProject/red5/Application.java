package com.ttProject.red5;

import java.util.HashSet;
import java.util.Set;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.stream.IBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基本となるApplicationAdapter
 * @author taktod
 */
public class Application extends ApplicationAdapter {
	private final Logger logger = LoggerFactory.getLogger(Application.class);
	private Red5TranscodeManager manager = null;
	private Set<String> allowedStreamName = new HashSet<String>();
	/**
	 * 変換マネージャーを設置します。
	 * @param manager
	 */
	public void setTranscodeManager(Red5TranscodeManager manager) {
		this.manager = manager;
	}
	/**
	 * 有効にするストリーム名を定義
	 */
	public void setAllowedStreamName(String name) {
		allowedStreamName.add(name);
	}
	/**
	 * 有効にするストリーム名を定義
	 */
	public void setAllowedStreamNames(Set<String> names) {
		allowedStreamName.addAll(names);
	}
	/**
	 * ストリームを開始したときのイベントを拾う
	 */
	@Override
	public void streamBroadcastStart(IBroadcastStream stream) {
		super.streamBroadcastStart(stream);
		String fullName = stream.getScope().getPath() + "/" + stream.getPublishedName();
		if(!allowedStreamName.contains(fullName)) {
			logger.warn("許可されていないストリーミングが接続してきました。:" + fullName);
			stream.stop();
			return;
		}
		if(manager != null) {
			logger.info("ストリームに変換処理を追加します。");
			manager.registerTranscoder(stream);
		}
	}
	/**
	 * ストリームを停止したときのイベントを拾う
	 */
	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		super.streamBroadcastClose(stream);
		String fullName = stream.getScope().getPath() + "/" + stream.getPublishedName();
		if(allowedStreamName.contains(fullName)) {
			logger.info("ストリームが停止するので、変換処理を止めます。");
			manager.unregisterTranscoder(stream);
		}
	}
}
