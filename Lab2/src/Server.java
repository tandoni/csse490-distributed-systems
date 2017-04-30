import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by CJ on 3/23/2017.
 */
public class Server implements Runnable{

    private final int port;

    public Server(int port) {
        this.port = port;
    }

	@Override
	public void run() {
        ServerSocket serverSocket = null;
//        Zookeeper zkClient = new ZooKeeper("");
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server Accepting Clients");
        } catch (IOException e) {
            System.err.println("Unable to start local Server-Server");
            e.printStackTrace();
            System.exit(1);
            return;
        }

        while(true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Client Connected");

                new Thread(new ClientResponder(socket)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

}
