package edu.rit.CSCI652.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * AgentListenerThread
 */
public class AgentListenerThread implements Runnable {
    private int port;
    
    public AgentListenerThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket agentlistenerSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port + "...");
            
            while (true) {
                Socket emSocket = agentlistenerSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(emSocket.getInputStream()));
                String message = in.readLine();
                handleInput(message);
            }
         } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void handleInput(String message) {
        System.out.println("Message received: " + message);
        // TODO: Implementation
    }

}