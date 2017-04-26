/**
 * Created by CJ on 3/24/2017.
 */
public enum Message {

    REQUEST_CHOPSTICK ("I am requesting the chopstick"),
    YES ("Yes, here you go"),
    NO ("No, you may not"),
    YOU_ARE_MY_LEFT ("You are my left node"),
    YOU_ARE_MY_RIGHT ("You are my right node"),
    WAKE_UP("Yo, wake up bruh"),
    CUP("Here is the cup"), 
    GAME_QUESTION("Game?"),
    AGREE_TO_GAME("Sure"),
    DENY_GAME("Doing other things");
    
    private final String message;

    Message(String message) {
        this.message = message;
    }

    String getMessage() {return message;}
}
