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
	Thread rmiThread;
	String IP;
	

	protected static Vector<Integer> peers = new Vector<Integer>();
	public static Map<Integer,String> peersIP;
	
	static int largest_agreed_seq;
	static int largest_proposed_seq;

	
	public static Map<Integer,String> msg_id_map;
	public static SortedMap<Integer, Integer> seq_id_map;
	public static SortedMap<Integer, String> delivery_map;

	DistributedChat(int id) { 

		msg_id_map = new HashMap<Integer, String>();
		seq_id_map = new TreeMap<Integer, Integer>(); 
		delivery_map = new TreeMap<Integer, String>();

		peersIP = new HashMap<Integer, String>();

		ID = id;
		inputSc = new Scanner(System.in);

		largest_agreed_seq = 0;
		largest_proposed_seq = 0;

		try{

			NetworkInterface ni = NetworkInterface.getByName("en0");
			Enumeration<InetAddress> inetAddresses =  ni.getInetAddresses();

			while(inetAddresses.hasMoreElements()) {
				InetAddress ia = inetAddresses.nextElement();
				if(!ia.isLinkLocalAddress()) {
		                //System.out.println("IP: " + ia.getHostAddress());
					IP = ia.getHostAddress().toString();
					System.out.println(IP);
				}
			}

		} catch(Exception e){	
		}
	}

	public static void main(String[] args) throws IOException {	
		new DistributedChat(Integer.parseInt(args[0])).run();
	}

	public void run() throws IOException {
		rmiThread = new Thread(new Runnable() {           
        	public void run() { 
        		try {         
        			ChatInterface stub = new Chat();
					Naming.rebind("rmi://" + IP + ":5000/" + ID, stub);
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
				exit();
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
				String joinMessage = "JOIN:" + ID + ":" + IP;
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
				String message = cmd.split(" ")[1];

				String tohash = ID+": "+message;
				int mid = tohash.hashCode();

				int seq_num = 0;
				for (int i =0 ; i < peers.size() ; i++){
					try {
						int val;
						ChatInterface c = (ChatInterface) Naming.lookup("rmi://" + peersIP.get(peers.get(i)) + ":5000/" + peers.get(i));
						val = c.sendInitialMessage(tohash,mid);
						seq_num = Math.max(seq_num, val);
					}
					catch(Exception e){ // Peer not accessible 
						System.err.println("Some Exception");
					}
				}
				for (int i=0; i < peers.size() ; i++){
					try{
						ChatInterface c = (ChatInterface) Naming.lookup("rmi://" + peersIP.get(peers.get(i)) + ":5000/" + peers.get(i));
						c.sendFinalMessage(mid,seq_num);
					}catch(Exception e){
						System.err.println("Some Exception");
					}
				}
				

			}
			else if(cmd.split(" ")[0].equals("ReplyTo")){
				// ReplyTo
				System.out.println("ReplyTo");

			}
			else {
				// Invalid cmd. Ignore.
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
		       				addPeer(id, fields[2]);
		       			try{
		       				ChatInterface c = (ChatInterface) Naming.lookup("rmi://" + peersIP.get(Integer.parseInt(fields[1])) + ":5000/" + Integer.parseInt(fields[1]));	
		       				c.ackJoin(ID, IP);
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

	public static void addPeer(int peerID, String ip) {
		System.out.println("Adding peer" + peerID);
		peers.add(peerID);
		peersIP.put(peerID, ip);
	}

	public void reply(String message) {
		// Decide on message ID 

		int msgID = 8;

		//Send message to all peers using RMI.
		for (int i = 0; i < peers.size(); i++) {
			try {
				ChatInterface c = (ChatInterface) Naming.lookup("rmi://" + peersIP.get(peers.get(i)) + ":5000/" + peers.get(i));
				c.getMessage(message, msgID, ID);
			} catch (Exception e) {
				// Peer not accessible
			}

		}
	}

	public static int processInitial(String message,int messageId){
		msg_id_map.put(messageId,message);
		largest_proposed_seq = Math.max(largest_agreed_seq,largest_proposed_seq)+1;
		seq_id_map.put(messageId,largest_proposed_seq);
		return largest_proposed_seq;
	}
	
	public static int processFinal(int messageId, int seqNo){
		largest_agreed_seq = Math.max(seqNo, largest_agreed_seq);
		seq_id_map.remove(messageId);
		seq_id_map.put(messageId,seqNo);
		delivery_map.put(seqNo, msg_id_map.get(messageId));
		System.out.println("#" + msg_id_map.get(messageId));
		return 0;
	}

	public void exit() {
		// Stop threads.
	}
}
