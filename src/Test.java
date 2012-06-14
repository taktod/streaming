
//@SuppressWarnings("unused")
public class Test {
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int a = 0x12345678;
		System.out.println(Integer.toHexString((byte)(0xFF & (a >> 32))));
	}
}
