import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.net.*;
import java.util.*;

public class Chat extends UnicastRemoteObject implements ChatInterface {

	
	
	public Chat() throws RemoteException {
	}

	public void ackJoin(int peerID) throws RemoteException {  // Acknowledgement by peers to the join message.
		System.out.println("Executing Ack join");
		DistributedChat.addPeer(peerID);
	}

	
	public void getMessage(String message, int msgID, int peerID) throws RemoteException {  // get Message from peer.
		DistributedChat.displayMessage(message, peerID);
	}
}