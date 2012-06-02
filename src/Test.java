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
		for(int i = 0;i < 100;i ++) {
			buf.put((byte)i);
		}
		System.out.println("position:" + buf.position());
		System.out.println("limit:" + buf.limit());
		System.out.println("capacity:" + buf.capacity());
		System.out.println("remaining:" + buf.remaining());
		buf.flip();
		System.out.println("position:" + buf.position());
		System.out.println("limit:" + buf.limit());
		System.out.println("capacity:" + buf.capacity());
		System.out.println("remaining:" + buf.remaining());
		buf.get(new byte[30]);
		System.out.println("position:" + buf.position());
		System.out.println("limit:" + buf.limit());
		System.out.println("capacity:" + buf.capacity());
		System.out.println("remaining:" + buf.remaining());
//		System.out.println(HexDump.toHexString(buf.array()));
		/*
position:100
limit:100
capacity:100
remaining:0
position:0
limit:100
capacity:100
remaining:100
position:30
limit:100
capacity:100
remaining:70
		 */
	}
}
