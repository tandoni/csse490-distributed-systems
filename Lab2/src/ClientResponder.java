import java.io.*;
import java.net.Socket;
import java.util.Random;

/**
 * Created by CJ on 3/24/2017.
 */
public class ClientResponder implements Runnable {

    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Socket socket;
    private boolean isLeft;

    private boolean onGoingRequest;
    private boolean onRequestCooldown;

    public ClientResponder(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override
    public void run() {
        while (true) {
            String messageRaw = null;
            try {
                messageRaw = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Message message = Message.valueOf(messageRaw);

            switch (message) {
                case REQUEST_CHOPSTICK:
                    if(onGoingRequest) {
                        sendMessage(Message.NO);
                        onRequestCooldown = true;
                        new Thread(() -> {
                            try {
                                Thread.sleep(Math.round(Math.random() * 100));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            onGoingRequest = true;
                            onRequestCooldown = false;
                            sendMessage(Message.REQUEST_CHOPSTICK);
                        }).start();
                        onGoingRequest = false;
                        break;
                    }

                    boolean available = Philosopher.INSTANCE.requestChopstick(isLeft);

                    if (available) {
                        sendMessage(Message.YES);
                    } else {
                        sendMessage(Message.NO);
                    }

                    break;

                case YOU_ARE_MY_LEFT:
                    Communicator.INSTANCE.rightSocket = this;
                    this.isLeft = false;
                    System.out.println("A client has identified as my right");
                    break;

                case YOU_ARE_MY_RIGHT:
                    Communicator.INSTANCE.leftSocket = this;
                    this.isLeft = true;
                    System.out.println("A client has identified as my left");
                    break;

                case YES:
                    Philosopher.INSTANCE.takeChopstick(isLeft);
                    onGoingRequest = false;
                    break;
                case NO:
                    Philosopher.INSTANCE.dropChopstick(isLeft);
                    onGoingRequest = false;
                    break;
                case WAKE_UP:
                    if (!Philosopher.INSTANCE.isAwake()) {
                        if (isLeft) Communicator.INSTANCE.rightSocket.sendMessage(Message.WAKE_UP);
                        else  Communicator.INSTANCE.leftSocket.sendMessage(Message.WAKE_UP);
                        Philosopher.INSTANCE.wakeUp();
                    }
                    break;
                default:
                    System.err.println("Received invalid message from a client, Message: " + messageRaw);
                    break;
            }
        }
    }

    public void registerAsLeft() {
        sendMessage(Message.YOU_ARE_MY_LEFT);
        Communicator.INSTANCE.leftSocket = this;
        this.isLeft = true;
    }

    public void registerAsRight() {
        sendMessage(Message.YOU_ARE_MY_RIGHT);
        Communicator.INSTANCE.rightSocket = this;
        this.isLeft = false;
    }

    private void sendMessage(Message s) {
        try {
            writer.write(s + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println("Unable to communicate with via " + this.toString());
            System.exit(1);
        }

    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", socket.getInetAddress(), socket.getLocalPort(), socket.getPort());
    }

    public void requestChopstick() {
        if(!(onGoingRequest || onRequestCooldown)) {
            onGoingRequest = true;
            this.sendMessage(Message.REQUEST_CHOPSTICK);
        }
    }

    public void sendWakeup() {
        this.sendMessage(Message.WAKE_UP);
    }
}
