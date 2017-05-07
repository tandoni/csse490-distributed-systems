import javax.swing.*;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
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
		ZooKeeper zk = new ZooKeeper("localhost:2181", 3000, watch);

		try {
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
			
			
			if (zk.exists("/start", watch) == null) {
				zk.create("/start", "false".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

			List<String> chopsticks = zk.getChildren("/c", watch);
			for (String chopstick : chopsticks) {
				System.out.println("c" + chopstick);
				zk.setData("/c/" + chopstick, "false".getBytes(), -1);
			}
			if (zk.exists("/c", watch) == null) {
				zk.create("/c", "start".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

			List<String> games = zk.getChildren("/c", watch);
			for (String game : games) {
				System.out.println("g" + game);
				zk.setData("/g/" + game, "false".getBytes(), -1);
			}
			if (zk.exists("/g", watch) == null) {
				zk.create("/g", "start".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

			zk.delete("/cup", -1);
			zk.create("/cup", "false".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			

			// make sure there are left and right nodes

			Node node = new Node(zkleft, zkright, myNum, zk);
			Philosopher.INSTANCE.setNode(node);
			if (zk.exists(node.getCLeft(), watch) == null) {
				zk.create(node.getCLeft(), "false".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			if (zk.exists(node.getCRight(), watch) == null) {
				zk.create(node.getCRight(), "false".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			if (zk.exists(node.getGLeft(), watch) == null) {
				zk.create(node.getGLeft(), "false".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			if (zk.exists(node.getGRight(), watch) == null) {
				zk.create(node.getGRight(), "false".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

		} catch (KeeperException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		Main.repl(reader);

	}

	public static void repl(BufferedReader reader) throws IOException {
		System.out.println("Ready for user input");
		while (true) {
			String input = reader.readLine();
			switch (input) {
			case "playing":

			case "start":
				// Communicator.INSTANCE.leftSocket.sendWakeup();
				// Communicator.INSTANCE.rightSocket.sendWakeup();

				Philosopher.INSTANCE.wakeUp();
				break;
			case "sleep":
				if (Philosopher.INSTANCE.getManual().equals("sleep")) {
					Philosopher.INSTANCE.nowManual("not");
				}
				Philosopher.INSTANCE.nowManual("sleep");
				// Philosopher.INSTANCE.nowSleeping(System.currentTimeMillis());
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
				Philosopher.INSTANCE.nowThinking(System.currentTimeMillis(), false, false);
				break;
			case "gui":
				JFrame frame = new JFrame("Philosopher");
				JPanel panel = new JPanel();
				JButton button = new JButton("Philosopher is hungry: " + Philosopher.INSTANCE.isHungry());
				button.addActionListener((ae) -> {
					if (Philosopher.INSTANCE.isHungry()) {
						Philosopher.INSTANCE.nowThinking(System.currentTimeMillis(), false, false);
					} else {
						Philosopher.INSTANCE.nowHungry(System.currentTimeMillis());
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
											.setText("Philosopher is hungry: " + Philosopher.INSTANCE.isHungry()));
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
