package edu.rit.CSCI652.impl;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AgentListenerThread
 */
public class AgentListenerThread implements Runnable {
    private int port;
    private String idFile;

    ReentrantLock consoleLock;
    
    public AgentListenerThread(int port, String idFile, ReentrantLock consoleLock) {
        this.port = port;
        this.idFile = idFile;
        this.consoleLock = consoleLock;
    }

    @Override
    public void run() {
        try (ServerSocket agentlistenerSocket = new ServerSocket(port)) {
            consoleLock.lock();
            System.out.println("Listening on port " + port + "...");
            consoleLock.unlock();

            while (true) {
                consoleLock.lock();
                Socket emSocket = agentlistenerSocket.accept();
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
    
    private void handleInput(String message) {
        // consoleLock.lock();
        System.out.println("Message received: " + message);

        String[] messageChunks = message.split("&");
        switch (messageChunks[0]) {
            case "id":
                String id = messageChunks[1];
                
                try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(idFile))) {
                    fileWriter.write(id);
                    fileWriter.newLine();
                    fileWriter.write(port + "");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                break;
        
            case "topics": // topics&topicId;topicName&topicId;topicName&...
                System.out.println("Available topics: ");
                for (int i = 1; i < messageChunks.length; i++) {
                    String[] topicElements = messageChunks[i].split(";");
                    System.out.println("\t" + topicElements[0] + "\t" + topicElements[1]);
                }
                break;

            case "confirmed": // confirmed&message
                System.out.println(messageChunks[1]);
                break;

            case "subscribedtopics": // subscribedtopics&topicId;topicName&topicId;topicName&...
                System.out.println("Subscribed topics: ");
                for (int i = 1; i < messageChunks.length; i++) {
                    String[] topicElements = messageChunks[i].split(";");
                    System.out.println("\t" + topicElements[0] + "\t" + topicElements[1]);
                }
                break;

            case "article": // article&title&topic&contents
                System.out.println("New article published under " + messageChunks[2]);
                System.out.println("\n" + messageChunks[1] + "\n");
                System.out.println(messageChunks[3]);
                break;
            
            case "advertisement": // advertisement&topicId&topicName&keywords
                System.out.println("New topic created: ");
                System.out.println("\t" + messageChunks[1] + "\t" + messageChunks[2]);
                System.out.println("Keywords: " + String.join(" ", messageChunks[3].split(";")));
                break;

            default:
                break;
        }
        // consoleLock.unlock();
    }

}