public class ClientTest {
	
	public static void main(String[] args) {
		XServerSender.Start();
		XClientThread xc = new XClientThread();
		xc.start();		
	}

}

class XClientThread extends Thread {
	public void run() {
		try {
			while (true) {
				String s = String.format("hello,Xsever!\1");
				XServerSender.SendEvent2XServer(s);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
