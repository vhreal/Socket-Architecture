import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

class XServerReceiverThread extends Thread {
	private Socket m_socket = null;
	private InputStream m_input = null;
	private OutputStream m_output = null;
	private BufferedReader m_br = null;
	private String m_clientHost = "";// 客户端的socket地址

	public XServerReceiverThread(Socket socket) {
		try {
			this.m_socket = socket;
			this.m_input = socket.getInputStream();
			this.m_output = socket.getOutputStream();
			this.m_br = new BufferedReader(new InputStreamReader(this.m_input, "UTF-8"));
			this.m_clientHost = m_socket.getRemoteSocketAddress().toString();
			this.m_clientHost = this.m_clientHost.substring(1);//因为m_clientHost为  /192.168.0.83:52177
			this.m_socket.setSoTimeout(3000);

			System.out.println("connection established from " + m_clientHost);
		} catch (Exception ex) {
			ex.printStackTrace();
			closeHandles();
		}
	}

	public void run() {
		try {
			while (true) {
				String event = "";

				while (true) {
					int one = this.m_br.read();
					if (one == -1) {
						closeHandles();
						break;
					}

					event += String.valueOf((char) one);
					if (one == '\1')// 一般消息尾部截止为一个不可见字符，这里假设为'\1'
					{
						XServerReceiver.PushEvent(event);
						break;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			closeHandles();
		}
	}

	private void closeHandles() {
		System.out.println("connection closed from " + m_clientHost);

		try {
			if (this.m_br != null) {
				this.m_br.close();
				this.m_br = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			if (this.m_input != null) {
				this.m_input.close();
				this.m_input = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			if (this.m_output != null) {
				this.m_output.close();
				this.m_output = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			if (this.m_socket != null) {
				this.m_socket.close();
				this.m_socket = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

class XServerListenThread extends Thread {
	private ServerSocket server_socket = null;
	private boolean m_pleaseWait = true;

	public void run() {
		while (true) {
			try {
				if (server_socket == null || server_socket.isClosed() || !server_socket.isBound()) {
					this.server_socket = new ServerSocket(5600);// XServer.TCP_PORT_FOR_OTHER_SERVER，这里假设为5600
					System.out.println("TCP Processor is listening on " + 5600);
					m_pleaseWait = false;
				}

				Socket socket = this.server_socket.accept();
				XServerReceiverThread xt = new XServerReceiverThread(socket);
				xt.start();
			} catch (BindException bex) {
				bex.printStackTrace();

				try {
					sleep(5000);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void Wait() {
		try {
			while (m_pleaseWait)
				Thread.sleep(10);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

class XServerHandlerThread extends Thread {
	public void run() {
		try {
			while (true) {
				String event = XServerReceiver.PopEvent();

				if (event == null) {
					Thread.sleep(1000);
					continue;
				}

				EventHandler(event);// 处理消息事件
				event = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void EventHandler(String event) {
		System.out.println("Received event: " + event);
	}
}

/**
 * 
 * Socket接受处理XServer返回工具类
 * XServerReceiver充当服务器
 * 
 * @author vhreal
 * 
 */
public class XServerReceiver {
	static private ConcurrentLinkedQueue<String> m_eventQueue = new ConcurrentLinkedQueue<String>();
	static private XServerListenThread m_XServerListenThread = null;// 监听线程
	static private XServerHandlerThread m_XServerHandlerThread = null;// 处理线程

	static public void PushEvent(String event) {// 外部类调用XServerReceiver.PushEvent(event);
		m_eventQueue.add(event);
	}

	static public String PopEvent() {// 外部类调用XServerReceiver.PopEvent(event);
		if (m_eventQueue.size() == 0)
			return null;

		return m_eventQueue.remove();
	}

	static public void Start()// 外部类调用XServerReceiver.Start();
	{
		m_XServerListenThread = new XServerListenThread();
		m_XServerListenThread.start();
		m_XServerListenThread.Wait();

		m_XServerHandlerThread = new XServerHandlerThread();
		m_XServerHandlerThread.start();
	}
}
