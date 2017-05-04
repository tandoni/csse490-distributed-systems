import org.apache.zookeeper.ZooKeeper;

/**
 * Created by CJ on 3/23/2017.
 */
public class Node {
	public final int me;
    public final int iRight;
    public final int iLeft;
    public final String cLeft;
    public final String cRight;
    public final String gLeft;
    public final String gRight;
    public ZooKeeper zk;
    
    public Node(int left, int right, int me, ZooKeeper zk) {
    	this.me = me;
    	this.iLeft = left;
    	this.iRight = right;
    	this.cLeft = "/c/" + left + me;
    	this.cRight = "/c/" + me + right;
    	this.gLeft = "/g/" + left + me;
    	this.gRight = "/g/" + me + right;
    	this.zk = zk;
    }
    
    public int myNum() {
    	return me;
    }
    
    public int getILeft() {
    	return iLeft;
    }
    
    public int getIRight() {
    	return iRight;
    }
    
    public String getCLeft() {
    	return cLeft;
    }
    
    public String getCRight() {
    	return cRight;
    }
    
    public String getGLeft() {
    	return gLeft;
    }
    
    public String getGRight() {
    	return gRight;
    }
    
}
