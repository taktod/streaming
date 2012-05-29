import java.io.FileOutputStream;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		FileOutputStream fos = new FileOutputStream("/Users/todatakahiko/test.out");
		fos.write(-5);
		fos.close();
	}

}
