import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

public class ClientWatcher implements Watcher {

	ZooKeeper zk;

	@Override
	public void process(WatchedEvent event) {

		String path = event.getPath();
		String val = "";
		try {
			val = new String(zk.getData(path, false, null));
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Node node = Philosopher.INSTANCE.getNode();
		this.zk = node.zk;

		if (path.equals(node.getCLeft())) {
			if (Philosopher.INSTANCE.isHungry() && !Boolean.parseBoolean(val)) {
				try {
					zk.setData(path, "true".getBytes(), -1);
					Philosopher.INSTANCE.takeChopstick(true);
				} catch (KeeperException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else if (path.equals(node.getCRight())) {
			if (Philosopher.INSTANCE.isHungry() && !Boolean.parseBoolean(val)) {
				try {
					zk.setData(path, "true".getBytes(), -1);
					Philosopher.INSTANCE.takeChopstick(false);
				} catch (KeeperException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else if (path.equals(node.getGLeft())) {

		} else if (path.equals(node.getGRight())) {

		} else if (path.equals("/cup")) {
			if (Philosopher.INSTANCE.isThirsty() && !Boolean.parseBoolean(val)) {
				try {
					zk.setData(path, "true".getBytes(), -1);
					Philosopher.INSTANCE.giveCup();
				} catch (KeeperException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setZkConn(ZooKeeper zk) {
		this.zk = zk;
	}
}
