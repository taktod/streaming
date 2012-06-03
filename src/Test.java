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
		String path = "/home/taktod/hogehoge";
		String name = "jpeg";
		if(path.endsWith("/")) {
			System.out.println("/");
		}
		else {
			System.out.println("b");
		}
	}
}
