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

		if (path.equals(node.getCLeft())) {
			if (Philosopher.INSTANCE.isHungry() && !Boolean.parseBoolean(val)) {
					Philosopher.INSTANCE.takeChopstick(true);
			} else  {
				Philosopher.INSTANCE.dropChopstick(true);
			}
		} else if (path.equals(node.getCRight())) {
			if (Philosopher.INSTANCE.isHungry() && !Boolean.parseBoolean(val)) {
				Philosopher.INSTANCE.takeChopstick(true);
			} else {
				Philosopher.INSTANCE.dropChopstick(false);
			}
		} else if (path.equals(node.getGLeft())) {

		} else if (path.equals(node.getGRight())) {

		} else if (path.equals("/cup")) {
			if (Philosopher.INSTANCE.isThirsty() && !Boolean.parseBoolean(val)) {
				Philosopher.INSTANCE.setHasCup(true);
			} else {
				Philosopher.INSTANCE.dropCup();
			}
		}
	}

	public void setZkConn(ZooKeeper zk) {
		this.zk = zk;
	}
}
