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
	public static AtomicInteger eventSeed = new AtomicInteger();
	// mapping of agent id to its port and ip
	public static Map<Integer, List<String>> portMap = new HashMap<>();
	// mapping of topic id to list of subscriber ids
	public static Map<Integer, HashSet<Integer>> topicMap = new HashMap<>();
	// mapping of topic name to topic object
	public static Map<String, Topic> topics = new HashMap<>();
	// keep track of all events so far
	public static List<Event> events = new ArrayList<>();
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
					out.println("id&" + this.registerAgent(port, clientSocket.getLocalAddress()));
					out.close();
					outputSocket.close();	
					break;
				}

				case "publish":	{ // publish&<topicID>&<content>
					// send content to all subscribers
					int topicID = Integer.parseInt(messageChunked[1]);
					String content = messageChunked[2];
					String[] contentChunked = content.split(";"); // check if ; is the delimiter
					String name = new String();
					for (String t: topics.keySet()) {
						if (topics.get(t).getID() == topicID) {
							name = t;
							break;
						}
					}
					Event article = new Event(eventSeed.getAndIncrement(), topics.get(name),
											  contentChunked[0], contentChunked[1]);
					this.notifySubscribers(article);
					// confirm message? how? (don't know agentID)
					break;
				}
				
				case "subscribe":	{// subscribe&<id>&<topicId>
					// add agent to the list of subscribers of topic
					int agentID = Integer.parseInt(messageChunked[1]);
					int topicID = Integer.parseInt(messageChunked[2]);
					this.addSubscriber(agentID, topicID);
					this.sendMessage(agentID, "confirmed subscribe");
					break;
				}

				case "subscribedtopics":	{	//subscribedtopics&<id>
					int agentID = Integer.parseInt(messageChunked[1]);
					this.listSubscribedTopics(agentID);
					// does this need a confirmation too? agent knows it worked upon seeing list anyway
					break;
				}

				case "unsubscribe":	{	// unsubscribe&<id>&<topicId>
					int agentID = Integer.parseInt(messageChunked[1]);
					int topicID = Integer.parseInt(messageChunked[2]);
					this.removeSubscriber(agentID, topicID);
					this.sendMessage(agentID, "confirmed unsubscribe");
					break;
				}

				case "unsubscribeall":	{	// unsubscribeall&<id>
					int agentID = Integer.parseInt(messageChunked[1]);
					this.unsubscribeAll(agentID);
					this.sendMessage(agentID, "confirmed unsubscribeall");
					break;
				}

				case "advertise": {	// advertise&<topicName>&<keywordsList> 	
					System.out.println("got message okay: " + message);
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
		String name = event.getTopic().getName();
		String title = event.getTitle();
		String content = event.getContent();
		int topicID = event.getTopic().getID();
		// construct message
		String message = new String("article" + "&" + name + "&" + title + "&" + content);
		Set<Integer> subscribers = topicMap.get(topicID);
		for (Integer s: subscribers)
			this.sendMessage(s, message);
	}
	
	/*
	 * add new topic when received advertisement of new topic
	 */
	private void addTopic(String topicName, String[] keywords){
		if (!topics.containsKey(topicName)) 
		{	List<String> keywordsList = new ArrayList();
			for (String k: keywords)
				keywordsList.add(k);
			Topic newTopic = new Topic(topicSeed.getAndIncrement(), new String(topicName), keywordsList);
			topics.put(topicName, newTopic); 
			this.advertise(newTopic.getID());
		}
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
	private void removeSubscriber(int agent, int topic){
		if (topicMap.containsKey(topic)) {
			if (topicMap.get(topic).contains(agent)) 
				topicMap.get(topic).remove(agent);
		}
	}
	

	private void unsubscribeAll(int agent) {
		for (int t: topicMap.keySet())
			this.removeSubscriber(agent, t);
	}

	private void advertise(int topicID) {
		// construct message with topic name and keywords
		Topic t = topics.get(topicID);
		String message = new String("advertisement" + "&" + t.getName() + "&");	// what 
		for (String word: t.getKeywords()) 
			message = new String(message + word + " ");
		// get all agents
		Set<Integer> agents = new HashSet<>();
		for (Integer id: topicMap.keySet()) {
			agents.addAll(topicMap.get(id));
		}
		// send to all agents - message type "advertisement"
		for (Integer agent: agents) {
			this.sendMessage(agent, message);
		}
	}

	private void listSubscribedTopics(int agent) {
		String subscribedTopics = new String("subscribedtopics" + "&");
		for (String t: topics.keySet()) {
			int topicID = topics.get(t).getID();
			if (topicMap.get(topicID).contains(agent))
				subscribedTopics = new String(subscribedTopics + t + "," + topicID + ";");
		}
		this.sendMessage(agent, subscribedTopics);
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
