package edu.rit.CSCI652.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * AgentListenerThread
 */
public class AgentListenerThread implements Runnable {
    private int port;
    private String idFile;
    
    public AgentListenerThread(int port, String idFile) {
        this.port = port;
        this.idFile = idFile;
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
                emSocket.close();
            }
         } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void handleInput(String message) {
        System.out.println("Message received: " + message);

        switch (message.split(" ")[0]) {
            case "id":
                int id = Integer.parseInt(message.split(" ")[1]);
                
                try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(idFile))) {
                    fileWriter.write(id);
                    fileWriter.newLine();
                    fileWriter.write(port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                break;
        
            default:
                break;
        }
    }

}