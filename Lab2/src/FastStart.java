import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by CJ on 3/29/2017.
 */
public class FastStart {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = Integer.parseInt(args[0]);
        Node left = getNode(args[1]);
        Node right = getNode(args[2]);
        long sleepTime = Long.parseLong(args[3]);
        int tickRAte = Integer.parseInt(args[4]);
        boolean cup = Boolean.parseBoolean(args[5]);
        
        Philosopher.INSTANCE.setStarvationTime(tickRAte);
        Philosopher.INSTANCE.setHasCup(cup);
        
        Runnable r2 = new Server(port);
        Thread t2 = new Thread(r2);
        t2.start();

        Thread.sleep(sleepTime);

        if(Communicator.INSTANCE.leftSocket == null) {
            try {
                Socket socket = new Socket(left.host, left.port);

                ClientResponder leftClient = new ClientResponder(socket);
                leftClient.registerAsLeft();
                new Thread(leftClient).start();
            } catch (IOException ignored) {
                System.err.println("Unable to connect to left client");
            }
        }
        if(Communicator.INSTANCE.rightSocket == null) {
            try {
                Socket socket = new Socket(right.host, right.port);

                ClientResponder rightClient = new ClientResponder(socket);
                rightClient.registerAsRight();
                new Thread(rightClient).start();
            } catch (IOException ignored) {
                System.err.println("Unable to connect to right client");
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Main.repl(reader);
    }

    private static Node getNode(String s) {
        String[] array = s.split(":");
        if(array.length != 2) {
            throw new RuntimeException("Invalid Arguments");
        }

        return new Node(array[0], Integer.parseInt(array[1]));
    }
}
