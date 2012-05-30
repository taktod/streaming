import java.util.concurrent.LinkedBlockingQueue;

public class Test {
	public static LinkedBlockingQueue<String> queue = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		queue = new LinkedBlockingQueue<String>();
		final Thread ownerThread = Thread.currentThread();
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
					System.out.println("finished?");
					queue.clear();
					queue = null;
					ownerThread.interrupt();
				}
				catch (Exception e) {
				}
			}
		});
		t.start();
		queue.add("a");
		queue.add("b");
		queue.add("c");
		
		try {
			System.out.println(queue.take());
		}
		catch (Exception e) {
		}
		try {
		System.out.println(queue.take());
		}
		catch (Exception e) {
		}
		try {
		System.out.println(queue.take());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		try {
		System.out.println(queue.take());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		try {
		System.out.println(queue.take());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
