package edu.rit.CSCI652.impl;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

public class EventManager {
	private int PORT = 5000;
	
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
