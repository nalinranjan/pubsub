package edu.rit.CSCI652.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
            System.out.println("Listening on port " + port + "...");

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
        
            case "topics":
                System.out.println("Available topics: ");
                for (int i = 1; i < messageChunks.length; i++) {
                    String[] topicElements = messageChunks[i].split(";");
                    System.out.println("\t" + topicElements[0] + "\t" + topicElements[1]);
                }
                break;

            default:
                break;
        }
        // consoleLock.unlock();
    }

}