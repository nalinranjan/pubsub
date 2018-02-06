package edu.rit.CSCI652.impl;


import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Topic;

/**
 * Event Manager that handles all pub/sub operations. 
 */
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
    // mapping of agentID to messages received while offline to keep track of undelivered messages 
    private static Map<Integer, List<String>> pendingMessages = new HashMap<>();

    /**
     * Register a PubSub Agent for the first time
     * @param   port    port of the agent
     * @param   ip      ip address of the agent
     */
    private int registerAgent(String port, InetAddress ip) {
        List<String> agentInfo = new ArrayList<String>();
        agentInfo.add(ip.toString());
        agentInfo.add(port);
        portMap.put(agentSeed.get(), agentInfo);
        
        return agentSeed.getAndIncrement();
    }

    /**
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

    /**
     * Get the socket to connect with and talk to client on
     * @param   clientId    unique ID assigned to the client upon registering  
     * @return  clientSocket    Socket to connect with client 
     */
    private Socket getOutputSocket(int clientId) {
        List<String> clientInfo = portMap.get(clientId);
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(InetAddress.getByName(clientInfo.get(0).split("/")[1]), 
                                    Integer.parseInt(clientInfo.get(1)));
        } catch (ConnectException e) {
            System.out.println("Agent " + clientId + " not online. Saving message.");
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return clientSocket;
    }

    /**
     * Send message to the client specified
     * @param   clientId    unique ID assigned to the client upon registering
     * @param   message     message to send to client
     */
    private void sendMessage(int clientId, String message) {
        Socket clientSocket = getOutputSocket(clientId);
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            if (pendingMessages.containsKey(clientId)) {
                String newMessage = "multi^";
                for (String m : pendingMessages.get(clientId)) {
                    newMessage += m + "^";
                }
                pendingMessages.remove(clientId);
                message = newMessage + message;
            }

            out.println(message);
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            saveMessage(clientId, message);
        }

        System.out.println("Message sent to agent " + clientId + ": " + message);
    }


    /**
     * Save message if agent is offline
     * @param   agentID     unique ID assigned to agent upon registering
     * @param   message     message to be saved to send agent upon coming online
     */
    private void saveMessage(int agentID, String message) {
        if (!pendingMessages.containsKey(agentID)) {
            pendingMessages.put(agentID, new ArrayList<String>());
        }
        pendingMessages.get(agentID).add(message);
    }


    /**
     * Handle requests from the pub/sub agents
     * @param   clientSocket    socket to connect with agent
     */
    private void handleInput(Socket clientSocket) {
        System.out.println("Handling input from " + clientSocket.getInetAddress());

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            String message = in.readLine();
            String[] messageChunked = message.split("\\&");
            
            switch(messageChunked[0]) {
                case "register":	{
                    String port = messageChunked[PORT_INDEX];
                    int clientId = registerAgent(port, clientSocket.getInetAddress());
                    sendMessage(clientId, "id&" + clientId);
                    break;
                }

                case "publish":	{ // publish&<title>&<topicID>&<content>
                    // send content to all subscribers
                    int topicID = Integer.parseInt(messageChunked[2]);
                    String content = messageChunked[3];
                    String name = null;
                    for (String t: topics.keySet()) {
                        if (topics.get(t).getID() == topicID) {
                            name = t;
                            break;
                        }
                    }
                    if (name != null) {
                        Event article = new Event(eventSeed.getAndIncrement(), topics.get(name),
                                                  messageChunked[1], content);
                        notifySubscribers(article);
                    }
                    break;
                }
                
                case "subscribe":	{	// subscribe&<id>&<topicId>
                    int agentID = Integer.parseInt(messageChunked[1]);
                    int topicID = Integer.parseInt(messageChunked[2]);
                    if (topicID < topicSeed.get()) {
                        addSubscriber(agentID, topicID);
                        sendMessage(agentID, "confirmed&Subscribed successfully.");
                    }
                    else {
                        sendMessage(agentID, "confirmed&Invalid topic ID.");                        
                    }
                    break;
                }

                case "topics":  { // topics&<id>
                    int agentID = Integer.parseInt(messageChunked[1]);
                    listAllTopics(agentID);
                    break;
                }

                case "subscribedtopics":	{	// subscribedtopics&<id>
                    int agentID = Integer.parseInt(messageChunked[1]);
                    listSubscribedTopics(agentID);
                    break;
                }

                case "unsubscribe":	{	// unsubscribe&<id>&<topicId>
                    int agentID = Integer.parseInt(messageChunked[1]);
                    int topicID = Integer.parseInt(messageChunked[2]);
                    removeSubscriber(agentID, topicID);
                    sendMessage(agentID, "confirmed&Unsubscribed successfully.");
                    break;
                }

                case "unsubscribeall":	{	// unsubscribeall&<id>
                    int agentID = Integer.parseInt(messageChunked[1]);
                    unsubscribeAll(agentID);
                    sendMessage(agentID, "confirmed&Unsubscribed successfully.");
                    break;
                }

                case "subscribekeyword": { // subscribekeyword&<agentid>&<keyword>
                    int agentID = Integer.parseInt(messageChunked[1]);
                    String keyword = messageChunked[2].toLowerCase();
                    subscribeKeyword(agentID, keyword);
                    sendMessage(agentID, "confirmed&Subscribed successfully.");
                    break;
                }

                case "advertise": {	// advertise&<topicName>&<keywordsList> 	
                    String topicName = messageChunked[1].toLowerCase();
                    String[] keywordsList = messageChunked[2].split("\\s+");
                    addTopic(topicName, keywordsList);
                    break;
                }

                default: {
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

    /**
     * Method to notify all subscribers of new event 
     * @param   event   Event to notify all subscribers of event topic about
     */
    private void notifySubscribers(Event event) {
        String name = event.getTopic().getName();
        String title = event.getTitle();
        String content = event.getContent();
        int topicID = event.getTopic().getID();
        // construct message
        String message = new String("article" + "&" + title + "&" + name + "&" + content);
        Set<Integer> subscribers = topicMap.get(topicID);
        for (Integer s: subscribers) {
            sendMessage(s, message);
        }
        events.add(event);
    }
    
    /**
     * Method to add new topic when received advertisement of new topic
     * @param   topicName   name of the topic to add
     * @param   keywords    string array of keywords associated with that topic 
     */
    private synchronized void addTopic(String topicName, String[] keywords){
        if (!topics.containsKey(topicName)) {
            List<String> keywordsList = new ArrayList<>(Arrays.asList(keywords));
            Topic newTopic = new Topic(topicSeed.getAndIncrement(), new String(topicName), keywordsList);
            topics.put(topicName, newTopic);
            topicMap.put(newTopic.getID(), new HashSet<Integer>());
            advertiseTopic(newTopic);
        }
    }
    
    /**
     * add subscriber to the internal list
     * @param   agentID     unique ID assigned to agent upon registering
     * @param   topicID     unique ID assigned to topic upon creation
     */
    private synchronized void addSubscriber(int agentID, int topicID) {
        if (topicMap.containsKey(topicID))
            topicMap.get(topicID).add(agentID);
    }

    private synchronized void subscribeKeyword(int agentID, String word) {
        for (String topicName: topics.keySet())
            for (String keyword: topics.get(topicName).getKeywords())
                if (keyword.equals(word))
                    addSubscriber(agentID, topics.get(topicName).getID());
    }
    
    /**
     * Method to remove subscriber from the list
     * @param   agentID     unique ID assigned to agent upon registering
     * @param   topic       topic that the agent wants to be unsubscribed from     
     */
    private synchronized void removeSubscriber(int agentID, int topic){
        if (topicMap.containsKey(topic)) {
            if (topicMap.get(topic).contains(agentID)) 
                topicMap.get(topic).remove(agentID);
        }
    }
    
    /**
     * Method to remove subscriber from all the topics currently subscribed to
     * @param   agentID     unique ID assigned to the agent upon registering
     */
    private synchronized void unsubscribeAll(int agentID) {
        for (int topic: topicMap.keySet())
            removeSubscriber(agentID, topic);
    }

    /**
     * Method to advertise a new topic
     * @param   topic   topic to advertise
     */
    private void advertiseTopic(Topic topic) {
        String message = "advertisement&" + topic.getID() + "&" + topic.getName() +
                         "&" + String.join(" ", topic.getKeywords());
        
        for (int i = 0; i < agentSeed.get(); i++) {
            sendMessage(i, message);
        }
    }

    /**
     * List all topics in the pub/sub system
     * @param   agentID     unique ID assigned to agent upon registering
     */
    private void listAllTopics(int agentID) {
        String topicsList = new String("topics");
        for (String t: topics.keySet()) {
            int topicID = topics.get(t).getID();
            topicsList += "&" + topicID + ";" + t;
        }
        sendMessage(agentID, topicsList);
    }

    /**
     * List of all topics that the pub/sub 
     * @param   agentID     unique ID assigned to agent upon registering
     */
    private void listSubscribedTopics(int agentID) {
        String subscribedTopics = new String("subscribedtopics");
        for (String t: topics.keySet()) {
            int topicID = topics.get(t).getID();
            if (topicMap.get(topicID).contains(agent)) {
                subscribedTopics += "&" + topicID + ";" + t;
            }
        }
        sendMessage(agentID, subscribedTopics);
    }


    /**
     * Main method - starts the Event Manager
     * @param   args    command-line arguments
     */
    public static void main(String[] args) {
        new EventManager().startService();
    }


}
