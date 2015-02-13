import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * This interface extends the class Remote and specifies the remote accessible 
 * methods login, logout and getMessageNo
 */
public interface ChatInterface extends Remote {
	
	public void ackJoin(String peerID, String IP) throws RemoteException; // Acknowledgement by peers to the join message.

	public int sendInitialMessage(String message, int messageId) throws RemoteException;

	public void sendFinalMessage(int messageId, int seq_no) throws RemoteException;
}
