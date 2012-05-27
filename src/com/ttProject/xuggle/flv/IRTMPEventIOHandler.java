package com.ttProject.xuggle.flv;

public interface IRTMPEventIOHandler {
	FlazrMessage read() throws InterruptedException;
	void write(FlazrMessage msg) throws InterruptedException;
}
