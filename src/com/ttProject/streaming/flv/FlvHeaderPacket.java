package com.ttProject.streaming.flv;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class FlvHeaderPacket extends FlvPacket {
	private ByteBuffer buffer;
	private ByteBuffer flvHeader = null;
	private ByteBuffer videoSequenceHeader = null;
	private ByteBuffer audioSequenceHeader = null;
	private boolean isSaved = false;
	public FlvHeaderPacket(FlvPacketManager manager) {
		super(manager);
	}
	@Override
	public boolean isHeader() {
		return true;
	}
	public boolean isSaved() {
		return isSaved;
	}
	/**
	 * 解析を実施します。
	 * ここにくるデータは、mediaPacket側でみつけた、単一パケットのコピーとしますので、終端等は調べる必要なし。
	 */
	@Override
	public boolean analize(ByteBuffer buffer) {
		byte type = buffer.get();
		buffer.rewind();
		switch(type) {
		case FlvPacketManager.AUDIO_TAG:
			audioSequenceHeader = buffer;
			isSaved = false;
			break;
		case FlvPacketManager.VIDEO_TAG:
			videoSequenceHeader = buffer;
			isSaved = false;
			break;
		case FlvPacketManager.FLV_TAG:
			flvHeader = buffer;
			break;
		default:
			return false;
		}
		ByteBuffer data = ByteBuffer.allocate(
				flvHeader.limit() + 
				(videoSequenceHeader == null ? 0 : videoSequenceHeader.limit()) +
				(audioSequenceHeader == null ? 0 : audioSequenceHeader.limit())
		);
		data.put(flvHeader);
		if(videoSequenceHeader != null) {
			data.put(videoSequenceHeader);
			videoSequenceHeader.rewind();
		}
		if(audioSequenceHeader != null) {
			data.put(audioSequenceHeader);
			audioSequenceHeader.rewind();
		}
		this.buffer = data;
		return true;
	}
	@Override
	public void writeData(String targetFile, boolean append) {
		try {
			WritableByteChannel channel = Channels.newChannel(new FileOutputStream(targetFile, append));
			buffer.flip();
			channel.write(buffer);
		}
		catch (Exception e) {
		}
		finally {
			isSaved = true;
		}
	}
}
