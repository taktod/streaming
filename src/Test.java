import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import org.red5.io.utils.HexDump;

public class Test {
	public static LinkedBlockingQueue<String> queue = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ByteBuffer buffer1 = ByteBuffer.allocate(2);
		ByteBuffer buffer2 = ByteBuffer.allocate(2);
		ByteBuffer buffer3 = ByteBuffer.allocate(2);
		ByteBuffer buf = ByteBuffer.allocate(10);
		buffer1.put((byte)0x01);
		System.out.println(buffer1.limit());
		buffer1.flip();
		System.out.println(buffer1.limit());
		buffer2.put((byte)0x01);
		buffer2.flip();
		buffer3.put((byte)0x01);
		buffer3.flip();
		System.out.println(buffer1.position());
		System.out.println(buffer1.position());
		System.out.println(HexDump.toHexString(buf.array()));
	}
}
