import java.util.Random;

/**
 * Created by CJ on 3/24/2017.
 */
public class Philosopher implements Runnable {
    public static Philosopher INSTANCE;

    static {
        INSTANCE = new Philosopher();
    }

    private long starvationTime = 4000L;
    private volatile boolean hasLeftChopstick;
    private volatile boolean hasRightChopstick;
    private volatile boolean hasCup;
    private volatile boolean hungry;
    private volatile boolean thirsty;
    private long timeLastAte;
    private long startedEating = 0L;
    private long startedDrinking = 0L;
    private long startedThinking = 0L;
    private long startedChopstickAttempt = 0L;
    private boolean awake;
	private boolean isPlaying;
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
        } else if(this.thirsty && this.hasCup) {
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
        System.out.println("I know who is on my left and right..");
        System.out.println("On my left is " + Communicator.INSTANCE.leftSocket);
        System.out.println("On my right is " + Communicator.INSTANCE.rightSocket);

        while (true) {
            long currentTime = System.currentTimeMillis();
//            System.out.format("Current Time: %d\n", currentTime);
//            System.out.format("Is hungry: %b since %d\n", this.hungry, this.startedChopstickAttempt - currentTime);
//            System.out.format("Last ate: %d    startedEating: %d\n", this.timeLastAte - currentTime, startedEating - currentTime);
//            System.out.format("Table state, Left: %b  Right: %b\n", this.hasLeftChopstick, this.hasRightChopstick);

            if(this.manual.equals("sleep")) {
            	this.nowSleeping(currentTime);
            } else {

	            // If eating, reset timestamp
	            if (isEating()) this.timeLastAte = currentTime;
	
	            if (this.timeLastAte + starvationTime < currentTime) {
	                System.err.println("I starved");
	                System.exit(1);
	            }
	            if((this.startedDrinking == 0L) && isDrinking()) this.startedDrinking = currentTime;
	            	
	            if (isDrinking() && (starvationTime / 40 + this.startedDrinking < currentTime || Math.random() > 0.9)) {
	            	nowThinking(currentTime, false, true);
	            } else if(!this.thirsty && (starvationTime / 4 + this.startedThinking < currentTime || Math.random() > 0.9)) {
	            	nowThirsty(currentTime);
	            }
	
	            if (isEating() && (starvationTime / 40 + startedEating < currentTime || Math.random() > 0.9)) { // check how long we've been eating and stop if necessary
	                nowThinking(currentTime, true, false);
	            } else if (!this.hungry && (starvationTime / 4 + startedThinking < currentTime || Math.random() > 0.99)) { // Don't think for more than 1 second
	                this.nowHungry(currentTime);
	            } else if (this.hungry && !this.isEating() && startedChopstickAttempt + starvationTime / 40 < currentTime) { 
	                synchronized (this) {
	                    hasLeftChopstick = false;
	                    hasRightChopstick = false;
	                }
	                try {
	                    System.err.println("Sleeping for a bit");
	                    Thread.sleep(Math.round(Math.random() * starvationTime / 40.0));
	                    startedChopstickAttempt = System.currentTimeMillis();
	                } catch (InterruptedException e) {
	                }
	            }
	
	            // If hungry but not eating, try to take chopsticks
	            if (!isEating() && this.hungry) {
	                synchronized (this) {
	                    if (!hasLeftChopstick) Communicator.INSTANCE.leftSocket.requestChopstick();
	                    if (!hasRightChopstick) Communicator.INSTANCE.rightSocket.requestChopstick();
	                }
	            }
	            
	            try {
	                Thread.sleep(Math.round(Math.ceil(starvationTime / 4000.0)));
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	            
//	            if(!isDrinking() || !isEating() || !isHungry() || !this.thirsty) {
//	            	Communicator.INSTANCE.leftSocket.requestGame();
//	            	Communicator.INSTANCE.rightSocket.requestGame();
//	            }
	        }
        }
    }

	public synchronized boolean requestChopstick(boolean isLeft) {
        if (isLeft) return !this.hasLeftChopstick;
        else return !this.hasRightChopstick;
    }

    public synchronized void takeChopstick(boolean isLeft) {
        if (!this.hungry)
            return;

        if (isLeft) this.hasLeftChopstick = true;
        else this.hasRightChopstick = true;

        if (isEating()) {
            startedEating = System.currentTimeMillis();
        }
    }

    public synchronized void dropChopstick(boolean isLeft) {
        if (isLeft) {
            this.hasLeftChopstick = false;
        } else {
            this.hasRightChopstick = false;
        }
    }

    public synchronized void nowSleeping(long currentTime) {
    	synchronized (this) {
            hasLeftChopstick = false;
            hasRightChopstick = false;
        }
        try {
            System.err.println("Sleeping for a bit");
            Thread.sleep(Math.round(Math.random() * starvationTime / 40.0));
            //startedChopstickAttempt = System.currentTimeMillis();
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
        if(!eat && !drink) {
        	this.hasLeftChopstick = false;
	        this.hasRightChopstick = false;
	        if(this.manual.equals("not")) {
	        	this.hungry = false;
	        	this.thirsty = false;
	        } else if(this.manual.equals("hungry")) {
	        	this.thirsty = false;
	        } else if(this.manual.equals("thirsty")) {
	        	this.hungry = false;
	        }
	        if(this.hasCup) {
	        	this.hasCup = false;
	    		Communicator.INSTANCE.leftSocket.passCup();
	        }
        } else if (eat) {
	    	this.hasLeftChopstick = false;
	        this.hasRightChopstick = false;
	        if(!this.manual.equals("hungry")) {
	        	this.hungry = false;
	        }
	    } else if (drink) {
	    	if(!this.manual.equals("thirsty")) {
	    		this.thirsty = false;
	    	}
	    	this.hasCup = false;
	    	Communicator.INSTANCE.leftSocket.passCup();
	    }
        
        if(!this.thirsty && !this.hungry ) {
        	this.startedThinking = currentTime;
        	if(eat) {
        		System.out.println("Finished eating, thinking now.");
        	} else {
        		System.out.println("Finished drinking, thinking now");
        	}
        } else if(!this.thirsty && isEating()) {
        	System.out.println("finished drinking, still eating");
        } else if(!thirsty && this.hungry) {
        	System.out.println("Finished drinking, still hungry");
        } else if(!this.hungry && isDrinking()) {
        	System.out.println("Finished eating, still drinking");
        } else if(!this.hungry && this.thirsty) {
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

	public void giveCup() {
		this.hasCup = true;
	}

	public void setHasCup(boolean cup) {
		this.hasCup = cup;
	}

	public void nowManual(String man) {
		this.manual = man;
	}

	public String getManual() {
		return this.manual;
	}



}
