import java.net.Socket;

/**
 * Created by CJ on 3/24/2017.
 */
public class Communicator {
    public static Communicator INSTANCE;

    static {
        INSTANCE = new Communicator();
    }

    public ClientResponder leftSocket;
    public ClientResponder rightSocket;
}
