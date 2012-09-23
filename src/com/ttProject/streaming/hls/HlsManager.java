package com.ttProject.streaming.hls;

import org.w3c.dom.Node;

import com.ttProject.streaming.MediaManager;
import com.ttProject.xuggle.ConvertManager;

public class HlsManager extends MediaManager {
	public HlsManager(Node node) {
		super(node);
	}
	@Override
	public boolean setup() {
		return true;
	}
	@Override
	public boolean resetupContainer() {
		ConvertManager convertManager = ConvertManager.getInstance();
		String url = HlsHandlerFactory.DEFAULT_PROTOCOL + ":" + convertManager.getName() + "_" + getName();
		return resetupContainer(url, "mpegts");
	}
}
