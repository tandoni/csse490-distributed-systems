import javax.swing.*;

import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;

/**
 * Created by CJ on 3/23/2017.
 */
public class Main {

	public static void main(String[] args) throws IOException {
		try {
			Philosopher.INSTANCE.setStarvationTime(Integer.parseInt(args[0]));
		} catch (NumberFormatException e) {
			System.err.println("Invalid tick rate provided");
			System.exit(1);
		}

		int myNum = Integer.parseInt(args[1]);
		int numPhilo = Integer.parseInt(args[2]);
		int zkleft, zkright;

		Watcher watch = new ClientWatcher();
		CountDownLatch connSignal = new CountDownLatch(0);

		ZooKeeper zk = new ZooKeeper("ishank.wlan.rose-hulman.edu:2181", 3000,
				new Watcher() {
					public void process(WatchedEvent event) {
						if (event.getState() == KeeperState.SyncConnected) {
							connSignal.countDown();
							System.out.println("I'm connected!");
						}
					}
				});
		try {
			connSignal.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		try {
			if (zk.exists("/c", watch) == null) {
				zk.create("/c", "start".getBytes(), Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}

			if (zk.exists("/g", watch) == null) {
				zk.create("/g", "start".getBytes(), Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}

			if (zk.exists("/cup", watch) == null) {
				zk.create("/cup", "start".getBytes(), Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}

			// determine who is my left and right

			if (myNum == 1) {
				zkleft = numPhilo;
				zkright = myNum + 1;
			} else if (myNum == numPhilo) {
				zkleft = myNum - 1;
				zkright = 1;
			} else {
				zkleft = myNum - 1;
				zkright = myNum + 1;
			}

			// make sure there are left and right nodes

			String cNameLeft = "/c/" + zkleft + myNum;
			String cNameRight = "/c/" + myNum + zkright;
			String gNameLeft = "/g/" + zkleft + myNum;
			String gNameRight = "/g/" + myNum + zkright;

			if (zk.exists(cNameLeft, watch) == null) {
				zk.create(cNameLeft, "start".getBytes(), Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}
			if (zk.exists(cNameRight, watch) == null) {
				zk.create(cNameRight, "start".getBytes(), Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}
			if (zk.exists(gNameLeft, watch) == null) {
				zk.create(gNameLeft, "start".getBytes(), Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}
			if (zk.exists(gNameRight, watch) == null) {
				zk.create(gNameRight, "start".getBytes(), Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}

		} catch (KeeperException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		/*
		 * ======================================================================
		 */
		System.out.println("Local Server Port");

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		String port = reader.readLine();

		int serverPort = Integer.parseInt(port);
		Runnable r2 = new Server(serverPort);
		Thread t2 = new Thread(r2);
		t2.start();

		System.out.println("Press enter to proceed to connection input.");
		reader.readLine();

		try {
			if (Communicator.INSTANCE.leftSocket == null) {
				System.out.println("IP:Port of left Connection");
				String input = reader.readLine();
				Node left = getNode(input);

				Socket socket = new Socket(left.host, left.port);

				ClientResponder leftClient = new ClientResponder(socket);
				leftClient.registerAsLeft();
				new Thread(leftClient).start();
			}
			if (Communicator.INSTANCE.rightSocket == null) {
				System.out.println("IP:Port of the right Connection");
				String input = reader.readLine();
				Node right = getNode(input);

				Socket socket = new Socket(right.host, right.port);

				ClientResponder rightClient = new ClientResponder(socket);
				rightClient.registerAsRight();
				new Thread(rightClient).start();
			}
		} catch (UnknownHostException e) {
			System.err.println("Invalid Arguments");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Start Philosopher Code
		// Philosopher.INSTANCE.wakeUp();

		repl(reader);

	}

	private static Node getNode(String s) {
		String[] array = s.split(":");
		if (array.length != 2) {
			throw new RuntimeException("Invalid Arguments");
		}

		return new Node(array[0], Integer.parseInt(array[1]));
	}

	public static void repl(BufferedReader reader) throws IOException {
		System.out.println("Ready for user input");
		while (true) {
			String input = reader.readLine();
			switch (input) {
			case "playing":

			case "start":
				Communicator.INSTANCE.leftSocket.sendWakeup();
				Communicator.INSTANCE.rightSocket.sendWakeup();
				Philosopher.INSTANCE.wakeUp();
				break;
			case "sleep":
				if (Philosopher.INSTANCE.getManual().equals("sleep")) {
					Philosopher.INSTANCE.nowManual("not");
				}
				Philosopher.INSTANCE.nowManual("sleep");
				Philosopher.INSTANCE.nowSleeping(System.currentTimeMillis());
				break;
			case "thirsty":
				if (Philosopher.INSTANCE.getManual().equals("thirsty")) {
					Philosopher.INSTANCE.nowManual("not");
				}
				Philosopher.INSTANCE.nowManual("thirsty");
				Philosopher.INSTANCE.nowThirsty(System.currentTimeMillis());
				break;
			case "hungry":
				if (Philosopher.INSTANCE.getManual().equals("hungry")) {
					Philosopher.INSTANCE.nowManual("not");
				}
				Philosopher.INSTANCE.nowManual("hungry");
				Philosopher.INSTANCE.nowHungry(System.currentTimeMillis());
				break;

			case "thinking":
				Philosopher.INSTANCE.nowThinking(System.currentTimeMillis(),
						false, false);
				break;
			case "gui":
				JFrame frame = new JFrame("Philosopher");
				JPanel panel = new JPanel();
				JButton button = new JButton("Philosopher is hungry: "
						+ Philosopher.INSTANCE.isHungry());
				button.addActionListener((ae) -> {
					if (Philosopher.INSTANCE.isHungry()) {
						Philosopher.INSTANCE.nowThinking(
								System.currentTimeMillis(), false, false);
					} else {
						Philosopher.INSTANCE.nowHungry(System
								.currentTimeMillis());
					}
				});
				panel.add(button);
				frame.add(panel);
				frame.pack();
				frame.addWindowListener(new WindowListener() {
					private Thread thread;
					private boolean shouldStop;

					@Override
					public void windowOpened(WindowEvent we) {
						thread = new Thread(() -> {
							while (true) {
								if (shouldStop)
									return;
								try {
									EventQueue.invokeAndWait(() -> button
											.setText("Philosopher is hungry: "
													+ Philosopher.INSTANCE
															.isHungry()));
								} catch (InterruptedException e) {
									e.printStackTrace();
								} catch (InvocationTargetException e) {
									e.printStackTrace();
								}
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						});
						thread.start();
					}

					@Override
					public void windowClosing(WindowEvent e) {
					}

					@Override
					public void windowClosed(WindowEvent e) {
						shouldStop = true;
					}

					@Override
					public void windowIconified(WindowEvent e) {
					}

					@Override
					public void windowDeiconified(WindowEvent e) {
					}

					@Override
					public void windowActivated(WindowEvent e) {
					}

					@Override
					public void windowDeactivated(WindowEvent e) {
					}
				});
				frame.setVisible(true);
			}
		}
	}
}
