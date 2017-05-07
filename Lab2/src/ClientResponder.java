import java.io.*;
import java.net.Socket;
import java.util.Random;

import org.apache.zookeeper.ZooKeeper;

/**
 * Created by CJ on 3/24/2017.
 */
public class ClientResponder implements Runnable {

//	private final BufferedReader reader;
//	private final BufferedWriter writer;
//	private final Socket socket;
//	private boolean isLeft;
//
//	private boolean onGoingRequest;
//	private boolean onRequestCooldown;

	private ZooKeeper zk;
	
	public ClientResponder(ZooKeeper zk) throws IOException {
//		this.socket = socket;
//		this.reader = new BufferedReader(new InputStreamReader(
//				socket.getInputStream()));
//		this.writer = new BufferedWriter(new OutputStreamWriter(
//				socket.getOutputStream()));
		
		this.zk = zk;
		
	}

	@Override
	public void run() {
		while (true) {
		}
	}
}
