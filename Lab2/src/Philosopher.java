import java.io.IOException;
import java.util.Random;

import org.apache.zookeeper.KeeperException;

import sun.util.resources.cldr.es.CurrencyNames_es_PR;

/**
 * Created by CJ on 3/24/2017.
 */
public class Philosopher implements Runnable {
	public static Philosopher INSTANCE;

	static {
		INSTANCE = new Philosopher();
	}

	private long starvationTime = 4000L;
	public Node node;
	private volatile boolean hasLeftChopstick;
	private volatile boolean hasRightChopstick;
	private volatile boolean hasCup;
	private volatile boolean hungry;
	private volatile boolean thirsty;
	private long timeLastAte;
	private long timeStartedWaiting = System.currentTimeMillis();
	private long timeStartedGaming = System.currentTimeMillis();
	private long startedEating = 0L;
	private long startedDrinking = 0L;
	private long startedThinking = 0L;
	private long startedChopstickAttempt = 0L;
	private boolean awake;
	private boolean isPlaying;
	private boolean isWaiting;
	private String manual;

	private Philosopher() {
		this.hasLeftChopstick = false;
		this.hasRightChopstick = false;
		this.hasCup = false;
		this.hungry = new Random().nextBoolean();
		this.thirsty = new Random().nextBoolean();
		this.awake = false;
		this.isPlaying = false;
		this.manual = "not";
	}

	public synchronized boolean isAwake() {
		return this.awake;
	}

	public void setStarvationTime(long starvationTime) {
		this.starvationTime = starvationTime;
	}

	public synchronized boolean isEating() {
		return hasLeftChopstick && hasRightChopstick && hungry;
	}

	public synchronized boolean isDrinking() {
		return hasCup && thirsty;
	}

	public synchronized void wakeUp() {
		this.timeLastAte = System.currentTimeMillis();
		if (this.hungry) {
			this.startedChopstickAttempt = System.currentTimeMillis();
		} else if (this.thirsty && this.hasCup) {
			this.startedDrinking = System.currentTimeMillis();
		} else {
			this.startedThinking = System.currentTimeMillis();
		}

		new Thread(this).start();
		this.awake = true;
		System.out.println("Started philosopher");
	}

	@Override
	public void run() {
		// System.out.println("I know who is on my left and right..");
		// System.out.println("On my left is " +
		// Communicator.INSTANCE.leftSocket);
		// System.out.println("On my right is " +
		// Communicator.INSTANCE.rightSocket);

		try {
			ClientResponder res = new ClientResponder(this.node.zk);
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		while (true) {
			long currentTime = System.currentTimeMillis();
			// System.out.format("Current Time: %d\n", currentTime);
			// System.out.format("Is hungry: %b since %d\n", this.hungry,
			// this.startedChopstickAttempt - currentTime);
			// System.out.format("Last ate: %d startedEating: %d\n",
			// this.timeLastAte - currentTime, startedEating - currentTime);
			// System.out.format("Table state, Left: %b Right: %b\n",
			// this.hasLeftChopstick, this.hasRightChopstick);

			if (this.manual.equals("sleep")) {
				this.nowSleeping(currentTime);
			} else if (this.isPlaying || this.isWaiting) {
				String left = "";
				String right = "";

				try {
					left = new String(this.node.zk.getData(this.node.gLeft, true, null));
					right = new String(this.node.zk.getData(this.node.gRight, true, null));
				} catch (KeeperException | InterruptedException e1) {
					e1.printStackTrace();
				}
				
				if (left.equals("true") && right.equals("game")) {
						try {
							this.node.zk.setData(this.node.gLeft, "false".getBytes(), -1);
						} catch (KeeperException | InterruptedException e) {
							e.printStackTrace();
						}
				
				}
				if (left.equals("game") && right.equals("true")) {
					try {
						this.node.zk.setData(this.node.gRight, "false".getBytes(), -1);
					} catch (KeeperException | InterruptedException e) {
						e.printStackTrace();
					}
				}

				if ((left.equals("game") || right.equals("game")) && (this.timeStartedGaming + 30000 < currentTime
						&& this.timeStartedWaiting + 30000 < currentTime)) {
					doneGaming();
				}
				if (left.equals("false") && right.equals("false")) {
					doneGaming();
				}

				// if (this.timeStartedWaiting + 15000 < currentTime) {
				// System.err.println("quit looking for game");
				// doneGaming();
				// }
				// if (this.timeStartedGaming + 10000 < currentTime) {
				// System.err.println("quit gaming");
				// doneGaming();
				// }
			} else {

				// If eating, reset timestamp
				if (isEating()) {
					this.timeLastAte = currentTime;
				}
				if (this.timeLastAte + starvationTime < currentTime) {
					System.err.println("I starved");
					System.exit(1);
				}
				if ((this.startedDrinking == 0L) && isDrinking())
					this.startedDrinking = currentTime;

				if (isDrinking() && (starvationTime / 40 + this.startedDrinking < currentTime || Math.random() > 0.9)) {
					nowThinking(currentTime, false, true);
				} else if (!this.thirsty
						&& (starvationTime / 1 + this.startedThinking < currentTime || Math.random() > 0.9)) {
					nowThirsty(currentTime);
				} else if (this.thirsty && !this.hasCup) {
					String cup = "";

					try {
						cup = new String(this.node.zk.getData("/cup", true, null));
					} catch (KeeperException | InterruptedException e) {
						e.printStackTrace();
					}

					if (cup.equals("false")) {
						setHasCup(true);
					}

				}

				if (isEating() && (starvationTime / 40 + startedEating < currentTime || Math.random() > 0.9)) { // check
																												// how
																												// long
																												// we've
																												// been
																												// eating
																												// and
																												// stop
																												// if
																												// necessary
					nowThinking(currentTime, true, false);
				} else if (!this.hungry
						&& (starvationTime / 4 + startedThinking < currentTime || Math.random() > 0.99)) { // Don't
																											// think
																											// for
																											// more
																											// than
																											// 1
																											// second
					this.nowHungry(currentTime);
				} else if (this.hungry && !this.isEating()
						&& startedChopstickAttempt + starvationTime / 40 < currentTime) {
					synchronized (this) {
						dropChopstick(true);
						dropChopstick(false);
					}
					try {
						// System.err.println("Sleeping for a bit");
						// Thread.sleep(Math.round(Math.random() *
						// starvationTime
						// / 40.0));
						startedChopstickAttempt = System.currentTimeMillis();
					} catch (Exception e) {
					}
				}

				// If hungry but not eating, try to take chopsticks
				if (!isEating() && this.hungry) {
					synchronized (this) {
						if (!hasLeftChopstick) {
							String left = "";
							try {
								left = new String(this.node.zk.getData(this.node.cLeft, true, null));
							} catch (KeeperException | InterruptedException e1) {
								e1.printStackTrace();
							}
							if (left.equals("false")) {
								this.takeChopstick(true);
							}
						}
						if (!hasRightChopstick) {
							String right = "";
							try {
								right = new String(this.node.zk.getData(this.node.cRight, true, null));
							} catch (KeeperException | InterruptedException e1) {
								e1.printStackTrace();
							}
							if (right.equals("false")) {
								this.takeChopstick(false);
							}
						}
					}
				}

				try {
					Thread.sleep(Math.round(Math.ceil(starvationTime / 40.0)));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// if(!isDrinking() || !isEating() || !isHungry() ||
				// !this.thirsty) {
				// Communicator.INSTANCE.leftSocket.requestGame();
				// Communicator.INSTANCE.rightSocket.requestGame();
				// }
			}
		}
	}

	public void letsGame() {
		this.dropCup();
		this.dropChopstick(true);
		this.dropChopstick(false);
		String left = "";
		try {
			left = new String(this.node.zk.getData(this.node.gLeft, true, null));
		} catch (KeeperException | InterruptedException e1) {
			e1.printStackTrace();
		}

		String right = "";
		try {
			right = new String(this.node.zk.getData(this.node.gRight, true, null));
		} catch (KeeperException | InterruptedException e1) {
			e1.printStackTrace();
		}

		if (left.equals("true")) {
			this.timeStartedGaming = System.currentTimeMillis();
			this.isPlaying = true;
			try {
				System.out.println("started game with left");
				this.node.zk.setData(this.node.gLeft, "game".getBytes(), -1);
				this.node.zk.setData(this.node.gRight, "false".getBytes(), -1);

			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}

		} else if (right.equals("true")) {
			this.timeStartedGaming = System.currentTimeMillis();
			this.isPlaying = true;

			try {
				this.node.zk.setData(this.node.gLeft, "false".getBytes(), -1);
				System.out.println("started game with right");
				this.node.zk.setData(this.node.gRight, "game".getBytes(), -1);
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}

		} else {
			this.timeStartedWaiting = System.currentTimeMillis();
			this.isWaiting = true;
			System.out.println("started waiting for game");
			try {
				this.node.zk.setData(this.node.gLeft, "true".getBytes(), -1);
				this.node.zk.setData(this.node.gRight, "true".getBytes(), -1);

			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private void doneGaming() {
		this.isPlaying = false;
		this.isWaiting = false;
		try {
			this.node.zk.setData(this.node.gLeft, "false".getBytes(), -1);
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
		try {
			this.node.zk.setData(this.node.gRight, "false".getBytes(), -1);
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Done gaming");
	}

	// public synchronized boolean requestChopstick(boolean isLeft) {
	// if (isLeft)
	// return !this.hasLeftChopstick;
	// else
	// return !this.hasRightChopstick;
	// }

	public synchronized void takeChopstick(boolean isLeft) {
		if (!this.hungry)
			return;

		if (isLeft) {
			// System.out.println("I picked up my left chopstick");
			try {
				this.node.zk.setData(this.node.cLeft, "true".getBytes(), -1);
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}
			this.hasLeftChopstick = true;
		} else {
			// System.out.println("I picked up my right chopstick");
			try {
				this.node.zk.setData(this.node.cLeft, "true".getBytes(), -1);
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}
			this.hasRightChopstick = true;
		}

		if (isEating()) {
			System.out.println("I am eating");
			startedEating = System.currentTimeMillis();
		}
	}

	public synchronized void dropChopstick(boolean isLeft) {
		if (isLeft) {
			// System.out.println("I put down my left chopstick");
			try {
				this.node.zk.setData(this.node.cLeft, "false".getBytes(), -1);
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}
			this.hasLeftChopstick = false;

		} else {
			// System.out.println("I put down my right chopstick");
			try {
				this.node.zk.setData(this.node.cLeft, "false".getBytes(), -1);
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}
			this.hasRightChopstick = false;
		}
	}

	public synchronized void nowSleeping(long currentTime) {
		synchronized (this) {
			dropChopstick(true);
			dropChopstick(false);
		}
		try {
			System.err.println("Sleeping for a bit");
			Thread.sleep(Math.round(Math.random() * starvationTime / 40.0));
			startedChopstickAttempt = System.currentTimeMillis();
		} catch (InterruptedException e) {
		}
	}

	public void nowPlaying() {
		this.isPlaying = true;
	}

	public synchronized void nowHungry(long currentTime) {
		this.hungry = true;
		this.startedChopstickAttempt = currentTime;

		System.out.println("Now hungry");
	}

	public synchronized void nowThinking(long currentTime, boolean eat, boolean drink) {
		if (!eat && !drink) {
			dropChopstick(true);
			dropChopstick(false);
			if (this.manual.equals("not")) {
				this.hungry = false;
				this.thirsty = false;
			} else if (this.manual.equals("hungry")) {
				this.thirsty = false;
			} else if (this.manual.equals("thirsty")) {
				this.hungry = false;
			}
			dropCup();
		} else if (eat) {
			System.out.println("I am done eating");
			dropChopstick(true);
			dropChopstick(false);
			if (!this.manual.equals("hungry")) {
				this.hungry = false;
			}
		} else if (drink) {
			if (!this.manual.equals("thirsty")) {
				this.thirsty = false;
			}
			dropCup();
		}

		if (!this.thirsty && !this.hungry) {
			this.startedThinking = currentTime;
			if (eat) {
				System.out.println("Finished eating, thinking now.");
			} else {
				System.out.println("Finished drinking, thinking now");
			}
		} else if (!this.thirsty && isEating()) {
			System.out.println("finished drinking, still eating");
		} else if (!thirsty && this.hungry) {
			System.out.println("Finished drinking, still hungry");
		} else if (!this.hungry && isDrinking()) {
			System.out.println("Finished eating, still drinking");
		} else if (!this.hungry && this.thirsty) {
			System.out.println("Finishd eating, still thirsty");
		}
	}

	public synchronized void nowThirsty(long currentTime) {
		this.thirsty = true;
		System.out.println("Now thirsty");
	}

	public boolean isHungry() {
		return hungry;
	}

	public boolean isThirsty() {
		return thirsty;
	}

	public void dropCup() {
		System.out.println("I Put Down the cup");
		this.hasCup = false;
		try {
			this.node.zk.setData("/cup", "false".getBytes(), -1);
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setHasCup(boolean cup) {
		System.out.println("I have the cup");
		this.hasCup = true;
		try {
			this.node.zk.setData("/cup", "true".getBytes(), -1);
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void nowManual(String man) {
		this.manual = man;
	}

	public String getManual() {
		return this.manual;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public Node getNode() {
		return this.node;
	}

}
