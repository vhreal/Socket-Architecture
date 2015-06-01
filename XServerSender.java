import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

class XServerSenderThread extends Thread {
	private Socket m_socket = null;
	private OutputStream m_output = null;
	private InputStream m_input = null;
	private InputStreamReader m_isr = null;
	private BufferedReader m_br = null;
	private ConcurrentLinkedQueue<String> m_Events = new ConcurrentLinkedQueue<String>();

	private String m_ServerHost = "192.168.0.83";
	private int m_ServerPort = 5600;

	public void run() {
		long lastkeepalive = 0;
		long now = 0;

		while (true) {
			try {
				if (m_socket == null || m_socket.isClosed()) {
					Connect2XServer();
					continue;
				}

				now = System.currentTimeMillis() / 1000;
				if (now - lastkeepalive > 2)// 2s发一次心跳
				{
					RequestKeepAlive();
					lastkeepalive = now;
				}

				String event = PopEvent();
				if (event == null) {
					Thread.sleep(1000);
					continue;
				}

				SendEvent2XServer(event);
				event = null;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void PushEvent(String event) {
		m_Events.add(event);
	}

	private String PopEvent() {
		if (m_Events.size() == 0)
			return null;

		return m_Events.remove();
	}

	private boolean SendEvent2XServer(String event) {
		try {
			m_output.write(event.getBytes());

			m_output.flush();
			return true;
		} catch (Exception ex) {
			closeHandles();
			return false;
		}
	}

	private boolean RequestKeepAlive() {
		try {
			String json = String
					.format("{\"AttributeTimeinSecs\":\"%d\",\"AttributeEventName\":\"EventKeepAlive\"}",
							System.currentTimeMillis() / 1000);
			m_output.write(json.getBytes());
			m_output.flush();
			return true;
		} catch (Exception ex) {
			closeHandles();
			return false;
		}
	}

	private boolean Connect2XServer() {
		try {
			System.out.println("connecting to XServer");

			SocketAddress endpoint = new InetSocketAddress(m_ServerHost, m_ServerPort);// XServer的Ip和端口号
			m_socket = new Socket();
			m_socket.setSoTimeout(3000);// 3s网络超时
			m_socket.connect(endpoint);

			m_output = m_socket.getOutputStream();
			m_input = m_socket.getInputStream();
			m_isr = new InputStreamReader(m_input, "UTF-8");
			m_br = new BufferedReader(m_isr);

			System.out.println("connected to XServer");
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			closeHandles();
		}

		return false;
	}

	private void closeHandles() {
		try {
			if (m_isr != null) {
				m_isr.close();
				m_isr = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			if (m_br != null) {
				m_br.close();
				m_br = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			if (m_input != null) {
				m_input.close();
				m_input = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			if (m_output != null) {
				m_output.close();
				m_output = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			if (m_socket != null) {
				m_socket.close();
				m_socket = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			Thread.sleep(1000);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

/**
 * 
 * Socket连接发送XServer服务器工具类 XServerSender充当客户端
 * 
 * @author vhreal
 * 
 */
public class XServerSender {
	static private XServerSenderThread m_SenderThread = null;

	static public void Start() {// 外部类调用XServerSender.Start();
		try {
			m_SenderThread = new XServerSenderThread();
			m_SenderThread.start();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	static public void SendEvent2XServer(String event) {// 外部类调用XServerSender.SendEvent2XServer(event);
		if (m_SenderThread != null) {
			m_SenderThread.PushEvent(event);
		}
	}
}
