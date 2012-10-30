package com.ttProject.streaming.tak.data;

public class TakMediaPacket extends TakPacket {
	@Override
	public boolean isHeader() {
		return false;
	}
	public TakMediaPacket(TakHeaderPacket headerPacket) {
		super(headerPacket);
	}
}
