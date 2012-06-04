import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import org.red5.io.utils.HexDump;

@SuppressWarnings("unused")
public class Test {
	public static LinkedBlockingQueue<String> queue = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ByteBuffer buf = ByteBuffer.allocate(100);
		for(int i = 0;i < 50;i ++) {
			buf.put((byte)i);
		}
		buf.flip();
		byte[] data = new byte[buf.limit()];
		buf.get(data);
		System.out.println(HexDump.toHexString(data));
		buf.flip();
		buf.position(5);
		System.out.println(buf.get());
		buf.position(5);
		buf.put((byte)0xFF);
		buf.position(0);
		data = new byte[buf.limit()];
		buf.get(data);
		System.out.println(HexDump.toHexString(data));
	}
}
