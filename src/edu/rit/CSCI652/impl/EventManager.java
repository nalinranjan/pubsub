package edu.rit.CSCI652.impl;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

public class EventManager {
	private int PORT = 5000;
	
	public static AtomicInteger idSeed = new AtomicInteger();
	// mapping of agent id to its port and ip
	public static Map<AtomicInteger,List> portMap = new HashMap<AtomicInteger,List>();
	// mapping of topic to list of subscriber ids
	public static Map<Topic,List> topicMap = new HashMap<Topic,List>();
	// list of all subscribers
	public static List<Integer> agents = new ArrayList<Integer>();

	private int PORT_INDEX = 1;
	/*
	 * Register a PubSub Agent for the first time
	 */
	private void registerAgent(String port, String ip) {
		agents.add(idSeed.get());
		
		List<String> agentInfo = new ArrayList<String>();
		agentInfo.put(ip);
		agentInfo.put(port);
		// add to portMap
		portMap.put(idSeed.get(), agentInfo);
		agents.add(idSeed.get());
		
		return idSeed.getAndIncrement();
	}


	/*
	 * Start the repo service
	 */
	private void startService() {
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
				final Socket clientSocket = serverSocket.accept();
				Thread clientHandler = new Thread(new Runnable() {
					public void run() {
						handleInput(clientSocket);
					}
				});
				clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + PORT);
        }
	}

	private void handleInput(Socket clientSocket) {
		System.out.println("Handling input from " + clientSocket.getLocalAddress());
		// get port from inputStream of socket
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String message = in.readLine();
			String[] messageChunked = message.split("\\s+");
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			
			switch(messageChunked[0]) {
				case "register":
					String port = messageChunked[1];
					out.println("id " + this.registerAgent(port, clientSocket.getLocalAddress()));	
					break;

				case "publish": 

					break;

				case "subscribe": 
					// send sp
					break;

				case "topics":

					break;
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}

		} 

		clientSocket.close();

	}

	/*
	 * notify all subscribers of new event 
	 */
	private void notifySubscribers(Event event) {
		
	}
	
	/*
	 * add new topic when received advertisement of new topic
	 */
	private void addTopic(Topic topic){
		for (i=0; i<topicMap.length; i++) {
			if topicMap[i].id == topic.id
				return;
		}
		List<String> subscribers = new ArrayList<String>();
		topicMap.put(topic, subscribers);
	}
	
	/*
	 * add subscriber to the internal list
	 */
	private void addSubscriber(){
		
	}
	
	/*
	 * remove subscriber from the list
	 */
	private void removeSubscriber(){
		
	}
	
	/*
	 * show the list of subscriber for a specified topic
	 */
	private void showSubscribers(Topic topic){
		
	}
	
	
	public static void main(String[] args) {
		new EventManager().startService();
	}


}
