package edu.rit.CSCI652.impl;

/**
 * AgentListenerThread.java
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread to listen for messages from the Event Manager.
 * 
 * @author  Nalin Ranjan
 * @author  Sanchitha Seshadri
 */
public class AgentListenerThread implements Runnable {
    private int port;
    private String idFile;

    ReentrantLock consoleLock;
    Object registerLock;
    
    /**
     * Constructor. Initializes object members.
     * 
     * @param   port            The port number to listen on
     * @param   idFile          The name of the agent ID file
     * @param   consoleLock     A lock for command-line output
     * @param   registerLock    A lock to wait for new agent registration
     */
    public AgentListenerThread(int port, String idFile, ReentrantLock consoleLock, Object registerLock) {
        this.port = port;
        this.idFile = idFile;
        this.consoleLock = consoleLock;
        this.registerLock = registerLock;
    }

    /**
     * Listens on a port and handles incoming messages from the Event Manager.
     */
    public void run() {
        try (ServerSocket agentlistenerSocket = new ServerSocket(port)) {
            consoleLock.lock();
            System.out.println("Listening on port " + port + "...");
            consoleLock.unlock();

            while (true) {
                Socket emSocket = agentlistenerSocket.accept();
                consoleLock.lock();
                BufferedReader in = new BufferedReader(new InputStreamReader(emSocket.getInputStream()));
                String message = in.readLine();
                handleInput(message);
                emSocket.close();
                consoleLock.unlock();
            }
         } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Displays messages from the Event Manager.
     * 
     * @param   message     The received message
     */
    private void handleInput(String message) {
        String[] messageChunks = message.split("\\^");
        if (messageChunks[0].equals("multi")) {
            for (int i = 1; i < messageChunks.length; i++) {
                parseMessage(messageChunks[i]);
            }
        }
        else {
            parseMessage(message);
        }
    }

    /**
     * Parses and displays a message from the Event Manager.
     * 
     * @param   message     The message to be parsed
     */
    private void parseMessage(String message) {
        String[] messageChunks = message.split("&");
        switch (messageChunks[0]) {
            case "id": // Message format: id&agentId
                String id = messageChunks[1];
                
                try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(idFile))) {
                    fileWriter.write(id);
                    fileWriter.newLine();
                    fileWriter.write(port + "");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                synchronized (registerLock) {
                    registerLock.notify();
                }
                
                break;
        
            case "topics": // Message format: topics&topicId;topicName&topicId;topicName&...
                System.out.println("\nAvailable topics: ");
                for (int i = 1; i < messageChunks.length; i++) {
                    String[] topicElements = messageChunks[i].split(";");
                    System.out.println("\t" + topicElements[0] + "\t" + topicElements[1]);
                }
                break;

            case "confirmed": // Message format: confirmed&message
                System.out.println("\n" + messageChunks[1]);
                break;

            case "subscribedtopics": // Message format: subscribedtopics&topicId;topicName&topicId;topicName&...
                System.out.println("\nSubscribed topics: ");
                for (int i = 1; i < messageChunks.length; i++) {
                    String[] topicElements = messageChunks[i].split(";");
                    System.out.println("\t" + topicElements[0] + "\t" + topicElements[1]);
                }
                break;

            case "article": // Message format: article&title&topic&contents
                System.out.println("\nNew article published under " + messageChunks[2]);
                System.out.println("\n" + messageChunks[1] + "\n");
                System.out.println(messageChunks[3]);
                break;
            
            case "advertisement": // Message format: advertisement&topicId&topicName&keywords
                System.out.println("\nNew topic created: ");
                System.out.println("\t" + messageChunks[1] + "\t" + messageChunks[2]);
                System.out.println("Keywords: " + String.join(" ", messageChunks[3].split(";")));
                break;

            default:
                break;
        }
    }
}