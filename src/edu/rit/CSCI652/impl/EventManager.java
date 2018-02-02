package edu.rit.CSCI652.impl;


import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

public class EventManager {
	private int PORT = 5000;
	private int PORT_INDEX = 1;

	public static AtomicInteger agentSeed = new AtomicInteger();
	public static AtomicInteger topicSeed = new AtomicInteger();
	// mapping of agent id to its port and ip
	public static Map<Integer, List<String>> portMap = new HashMap<>();
	// mapping of topic id to list of subscriber ids
	public static Map<Integer, HashSet<Integer>> topicMap = new HashMap<>();
	// when creating new topic, add to topicMap like topicMap.put("newtopicid", new HashSet<Integer>())
	public static Map<String, Topic> topics = new HashMap<>();
	/*
	 * Register a PubSub Agent for the first time
	 */
	private int registerAgent(String port, InetAddress ip) {
		List<String> agentInfo = new ArrayList<String>();
		agentInfo.add(ip.toString());
		agentInfo.add(port);
		portMap.put(agentSeed.get(), agentInfo);
		
		return agentSeed.getAndIncrement();
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

	private Socket getOutputSocket(int clientId) {
		List<String> clientInfo = portMap.get(clientId);
		Socket clientSocket = null;
		try {
			clientSocket = new Socket(InetAddress.getByName(clientInfo.get(0).split("/")[1]), 
									Integer.parseInt(clientInfo.get(1)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return clientSocket;
	}

	private void sendMessage(int clientId, String message) {
		Socket clientSocket = getOutputSocket(clientId);
		try {
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);					
			out.println(message);
			out.close();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleInput(Socket clientSocket) {
		System.out.println("Handling input from " + clientSocket.getLocalAddress());
		// get port from inputStream of socket
		try (
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		) {
			String message = in.readLine();
			String[] messageChunked = message.split("\\&");
			
			
			switch(messageChunked[0]) {
				case "register":	{
					String port = messageChunked[PORT_INDEX];
					int clientId = this.registerAgent(port, clientSocket.getLocalAddress());
					Socket outputSocket = getOutputSocket(clientId);
					PrintWriter out = new PrintWriter(outputSocket.getOutputStream(), true);					
					out.println("id " + this.registerAgent(port, clientSocket.getLocalAddress()));
					out.close();
					outputSocket.close();	
					break;
				}

				case "publish":	{ // publish&<topicID>&<content>
					// send content to all subscribers
					int topic = Integer.parseInt(messageChunked[1]);
					String content = messageChunked[2];
					// iterate to send to every subscriber
					//this.notifySubscribers();
					break;
				}
				
				case "subscribe":	{// subscribe&<id>&<topicId>
					// add agent to the list of subscribers of topic
					int agentID = Integer.parseInt(messageChunked[1]);
					int topicID = Integer.parseInt(messageChunked[2]);
					this.addSubscriber(agentID, topicID);
					break;
				}

				case "subscribedtopics":	{	//subscribedtopics&<id>
					// send a list of all topics available
					// does the event manager have to maintain a list of topics by name ??!
					int agentID = Integer.parseInt(messageChunked[1]);

					break;
				}

				case "unsubscribe":	{	// unsubscribe&<id>&<topicId>
					int agentID = Integer.parseInt(messageChunked[1]);
					int topicID = Integer.parseInt(messageChunked[2]);
					this.removeSubscriber(agentID, topicID);
					// acknowledge unsubscribed "confirm unsubscribe"
					break;
				}

				case "unsubscribeall":	{	// unsubscribeall&<id>

					break;
				}

				case "advertise": {	// advertise <topicName> <keywordsList> 	
					// create topic 
					// verify topic doesn't already exist? 
					String topicName = messageChunked[1].toLowerCase();
					String[] keywordsList = messageChunked[2].split("\\s+");
					this.addTopic(topicName, keywordsList);

					break;
				}
			}
		} catch(IOException e){
			e.printStackTrace();
		}

		try {
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * notify all subscribers of new event 
	 */
	private void notifySubscribers(Event event) {
		
	}
	
	/*
	 * add new topic when received advertisement of new topic
	 */
	private void addTopic(String topicName, String[] keywords){
		
	}
	
	/*
	 * add subscriber to the internal list
	 */
	private void addSubscriber(int agentID, int topicID){
		List<String> subscriberInfo = portMap.get(agentID);
		if (topicMap.containsKey(topicID)) {
			topicMap.get(topicID).add(agentID);
		}
	}
	
	/*
	 * remove subscriber from the list
	 */
	private void removeSubscriber(int agentID, int topicID){
		if (topicMap.containsKey(topicID)) {
			if (topicMap.get(topicID).contains(agentID)) 
				topicMap.get(topicID).remove(agentID);
		}
	}
	
	/*
	 * show the list of subscriber for a specified topic
	 *
	private void showSubscribers(int topicID){
	}*/
	
	
	public static void main(String[] args) {
		new EventManager().startService();
	}


}
