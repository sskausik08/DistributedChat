import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.net.*;
import java.util.*;

public class Chat extends UnicastRemoteObject implements ChatInterface {

	
	
	public Chat() throws RemoteException {
	}

	public void ackJoin(String peerID, String IP) throws RemoteException {  // Acknowledgement by peers to the join message.
		//sSystem.out.println("Executing Ack join");
		DistributedChat.addPeer(peerID, IP);
	}

	public int sendInitialMessage(String message, int messageId) throws RemoteException {
		//System.out.println("Executing sendInitialMessage");
		return DistributedChat.processInitial(message,messageId);
	}
	
	public void sendFinalMessage(int msgID, int seqNo) throws RemoteException{
		//System.out.println("Executing sendFinalMessage");
		DistributedChat.processFinal(msgID,seqNo);
	}

}