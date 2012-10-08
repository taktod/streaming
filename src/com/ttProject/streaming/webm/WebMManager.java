package com.ttProject.streaming.webm;

import org.w3c.dom.Node;

import com.ttProject.streaming.MediaManager;
import com.ttProject.xuggle.ConvertManager;

public class WebMManager extends MediaManager {
	public WebMManager(Node node) {
		super(node);
	}
	@Override
	public boolean setup() {
		WebMHandler handler = new WebMHandler("/home/xxxx/test" + getName() +".webm");
		WebMHandlerFactory factory = WebMHandlerFactory.getFactory();
		ConvertManager convertManager = ConvertManager.getInstance();
		factory.registerHandler(convertManager.getName() + "_" + getName(), handler);
		return true;
	}
	@Override
	public boolean resetupContainer() {
		ConvertManager convertManager = ConvertManager.getInstance();
		String url = WebMHandlerFactory.DEFAULT_PROTOCOL + ":" + convertManager.getName() + "_" + getName();
		return resetupContainer(url, "webm");
	}
}
