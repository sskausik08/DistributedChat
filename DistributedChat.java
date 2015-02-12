import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.*;

public class DistributedChat {

	int ID;
	MulticastSocket mSocket = null; // Multicast Socket for sending the join and leave messages.
	Scanner inputSc ;
	InetAddress MulticastAddress;
	int Port = 9999;
	int DatagramSize = 20;

	protected static Vector<Integer> peers = new Vector<Integer>();
	
	DistributedChat(int id) { 
		ID = id;
		inputSc = new Scanner(System.in);

		try {
			mSocket = new MulticastSocket(Port);
	        MulticastAddress = InetAddress.getByName("224.0.55.55");
		    mSocket.joinGroup(MulticastAddress);
		} catch (Exception e) {

		}
	}

	public static void main(String[] args) throws IOException {	
		new DistributedChat(Integer.parseInt(args[0])).run();
	}

	public void run() throws IOException {
		Thread rmiThread = new Thread(new Runnable() {           
        	public void run() { 
        		try {         
        			ChatInterface stub = new Chat();
					Naming.rebind("rmi://192.168.0.101:5000/" + ID, stub);
        		}
        		catch (Exception e) {}
        	}});

        rmiThread.start();	

        Thread listenThread = new Thread(new Runnable() {           
        	public void run() { 
        		listenForPeers();
        	}});

        listenThread.start();

		while(true) {
			System.out.print("#");
			String cmd = inputSc.nextLine();
			if(cmd.equals("Control leave")){

				// Multicast the leave message to peers.
				String leaveMessage = "LEAVE$" + ID;
				byte[] buf = new byte[DatagramSize];
           		buf = leaveMessage.getBytes();
       	    	DatagramPacket packet = new DatagramPacket(buf, buf.length, MulticastAddress, Port);
            	try {
            		mSocket.send(packet);
            	} catch (IOException e) {}

          		// Exit the chat client;
				return;
			}
			else if(cmd.equals("Control join")){
				
				// Joining the chat. Multicast ID to Peers.
				String joinMessage = "JOIN$" + ID;
				byte[] buf = new byte[DatagramSize];
           		buf = joinMessage.getBytes();
       	    	DatagramPacket packet = new DatagramPacket(buf, buf.length, MulticastAddress, Port);
            	try {
            		mSocket.send(packet);
            		System.out.println("Sending Join messages");
            	} catch (IOException e) {}

			}
			else if(cmd.split(" ")[0].equals("Reply")){
				// Reply
				System.out.println("Reply");

			}
			else if(cmd.split(" ")[0].equals("ReplyTo")){
				// ReplyTo
				System.out.println("ReplyTo");

			}
			else {
				// Invalid cmd.
				return;
			}
		}
	}

	public void listenForPeers() {
		byte[] buf = new byte[20];
        DatagramPacket multMessage = new DatagramPacket(buf, buf.length);
      
        // try {
        // 	WCSocketSink.setSoTimeout((int) ConnTimeout); // Timeout Socket to un-block at receive()
        // } catch(SocketException e) {}

        while(true) {
	        try {
	        	System.out.println("Listening");
	       		mSocket.receive(multMessage); 
	       		String multMessageStr = new String(multMessage.getData(), 0, multMessage.getLength());
	       		System.out.println("Recvd message " + multMessageStr);
	       		String[] fields = multMessageStr.split("$");
	       		if(fields[0].equals("JOIN")) {
	       			// New Peer joining. Update peer list.
	       			addPeer(Integer.parseInt(fields[1]));
	       		}
	       		if(fields[0].equals("LEAVE")) {
	       			//  Peer Leaving. Update peer list.

	       		}
	       	}
	       	catch (IOException e) {}
	    }
	}

	public static void displayMessage(String message, int peerID) {
		System.out.println(peerID + ":" + message);
	}

	public static void addPeer(int peerID) {
		System.out.println("Adding peer" + peerID);
		peers.add(peerID);
	}

	public void reply(String message) {
		// Decide on message ID 

		int msgID = 8;

		//Send message to all peers using RMI.
		for (int i = 0; i < peers.size(); i++) {
			try {
				ChatInterface c = (ChatInterface) Naming.lookup("rmi://192.168.0.101:5000/" + peers.get(i));
				c.getMessage(message, msgID, ID);
			} catch (Exception e) {
				// Peer not accessible
			}

		}
	}

}
