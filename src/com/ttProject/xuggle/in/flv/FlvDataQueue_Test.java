package com.ttProject.xuggle.in.flv;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class FlvDataQueue_Test extends FlvDataQueue {
	private FileOutputStream fos;
	public FlvDataQueue_Test(String path) {
		try {
			fos = new FileOutputStream(path);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void putHeaderData(ByteBuffer header) {
		System.out.println("headerデータを書き込みます。");
		byte[] data = new byte[header.limit()];
		header.get(data);
		try {
			fos.write(data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		header.flip();
	}
	@Override
	public void putTagData(ByteBuffer tag) {
		System.out.println("タグデータを書き込みます。");
		byte[] data = new byte[tag.limit()];
		tag.get(data);
		try {
			fos.write(data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		tag.flip();
	}
	@Override
	public void close() {
		System.out.println("閉じます。");
		try {
			if(fos != null) {
				fos.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public ByteBuffer read() {
		return null;
	}
}
