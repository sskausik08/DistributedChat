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
	Thread listenThread;

	protected static Vector<Integer> peers = new Vector<Integer>();
	
	DistributedChat(int id) { 
		ID = id;
		inputSc = new Scanner(System.in);

		
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

        listenThread = new Thread(new Runnable() {           
        	public void run() { 
        		listenForPeers();
        	}});

        

		while(true) {
			System.out.print("#");
			String cmd = inputSc.nextLine();
			if(cmd.equals("Control leave")){

				// Multicast the leave message to peers.
				String leaveMessage = "LEAVE:" + ID;
				byte[] buf = new byte[DatagramSize];
           		buf = leaveMessage.getBytes();
       	    	DatagramPacket packet = new DatagramPacket(buf, buf.length, MulticastAddress, Port);
            	try {
            		mSocket.send(packet);
            	} catch (IOException e) {
            	}

          		// Exit the chat client;
				return;
			}
			else if(cmd.equals("Control join")){
				// Creating the multicast socket
				try {
					mSocket = new MulticastSocket(Port);

				} catch (Exception e) {
					System.err.println("Exception in 1");
				}
				try { 
					MulticastAddress = InetAddress.getByName("225.50.4.1");
				} catch (java.net.UnknownHostException e) {
					System.err.println("Unknown Host Reported");
				}
				try{
					SocketAddress socketAddress = new InetSocketAddress(MulticastAddress, 9999);
					NetworkInterface networkInterface = NetworkInterface.getByName("en0");
					mSocket.joinGroup(socketAddress, networkInterface);
				} catch (IOException e) {
					System.err.println("Unable to join Group");
				}


				// Joining the chat. Multicast ID to Peers.
				String joinMessage = "JOIN:" + ID;
				byte[] buf = new byte[DatagramSize];
           		buf = joinMessage.getBytes();
       	    	DatagramPacket packet = new DatagramPacket(buf, buf.length, MulticastAddress, Port);
            	try {
            		mSocket.send(packet);
            		System.out.println("Sending Join messages");

            		//Start Listening Now
            		listenThread.start();
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
	       		String[] fields = multMessageStr.split(":");
	       		if(fields[0].equals("JOIN")) {
	       			// New Peer joining. Update peer list.
	       			int id = Integer.parseInt(fields[1]);
	       			if(id==ID) {} //Ignore
	       			else {
		       				addPeer(id);
		       			try{
		       				ChatInterface c = (ChatInterface) Naming.lookup("rmi://192.168.0.101:5000/" + Integer.parseInt(fields[1]));	
		       				c.ackJoin(ID);
		       			} catch (Exception e) {
							// Peer not accessible
						}
					}
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
