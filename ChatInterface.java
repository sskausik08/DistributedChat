import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * This interface extends the class Remote and specifies the remote accessible 
 * methods login, logout and getMessageNo
 */
public interface ChatInterface extends Remote {
	
	public void ackJoin(int peerID) throws RemoteException; // Acknowledgement by peers to the join message.
	
	public void getMessage(String message, int msgID, int peerID) throws RemoteException;  // get Message from peer.
}
