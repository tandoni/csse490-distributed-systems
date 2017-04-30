import javax.swing.*;
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

        System.out.println("Local Server Port");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String port = reader.readLine();

        int serverPort = Integer.parseInt(port);
        Runnable r2 = new Server(serverPort);
        Thread t2 = new Thread(r2);
        t2.start();

        System.out.println("Press enter to proceed to connection input.");
        reader.readLine();

        try {
            if(Communicator.INSTANCE.leftSocket == null) {
                System.out.println("IP:Port of left Connection");
                String input = reader.readLine();
                Node left = getNode(input);


                Socket socket = new Socket(left.host, left.port);

                ClientResponder leftClient = new ClientResponder(socket);
                leftClient.registerAsLeft();
                new Thread(leftClient).start();
            }
            if(Communicator.INSTANCE.rightSocket == null) {
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
        //Philosopher.INSTANCE.wakeUp();

        repl(reader);

    }

    private static Node getNode(String s) {
        String[] array = s.split(":");
        if(array.length != 2) {
            throw new RuntimeException("Invalid Arguments");
        }

        return new Node(array[0], Integer.parseInt(array[1]));
    }

    public static void repl(BufferedReader reader) throws IOException {
        System.out.println("Ready for user input");
        while(true) {
            String input = reader.readLine();
            switch (input) {
            	case "playing": 
            	
            
                case "start":
                    Communicator.INSTANCE.leftSocket.sendWakeup();
                    Communicator.INSTANCE.rightSocket.sendWakeup();
                    Philosopher.INSTANCE.wakeUp();
                    break;
                case "sleep":
                	if(Philosopher.INSTANCE.getManual().equals("sleep")) {
                		Philosopher.INSTANCE.nowManual("not");
                	}
                	Philosopher.INSTANCE.nowManual("sleep");
                	Philosopher.INSTANCE.nowSleeping(System.currentTimeMillis());
                	break;
                case "thirsty":
                	if(Philosopher.INSTANCE.getManual().equals("thirsty")) {
                		Philosopher.INSTANCE.nowManual("not");
                	}
                	Philosopher.INSTANCE.nowManual("thirsty");
                	Philosopher.INSTANCE.nowThirsty(System.currentTimeMillis());
                	break;
                case "hungry":
                	if(Philosopher.INSTANCE.getManual().equals("hungry")) {
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
                        if(Philosopher.INSTANCE.isHungry()) {
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
                                while(true) {
                                    if(shouldStop)
                                        return;
                                    try {
                                        EventQueue.invokeAndWait(() -> button.setText("Philosopher is hungry: " + Philosopher.INSTANCE.isHungry()));
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
                        public void windowClosing(WindowEvent e) { }

                        @Override
                        public void windowClosed(WindowEvent e) {
                            shouldStop = true;
                        }

                        @Override
                        public void windowIconified(WindowEvent e) { }

                        @Override
                        public void windowDeiconified(WindowEvent e) { }

                        @Override
                        public void windowActivated(WindowEvent e) { }

                        @Override
                        public void windowDeactivated(WindowEvent e) { }
                    });
                    frame.setVisible(true);
            }
        }
    }
}
