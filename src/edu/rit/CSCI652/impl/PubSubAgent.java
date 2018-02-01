package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Publisher;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class PubSubAgent implements Publisher, Subscriber{

	private static String EM_ADDRESS = "localhost";
	private static int EM_PORT = 5000;
	private static String ID_FILE = "AgentId";

	private int listenPort;
	private int agentId;

	public PubSubAgent() {
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader(ID_FILE));
			agentId = Integer.parseInt(fileReader.readLine());
			listenPort = Integer.parseInt(fileReader.readLine());
			fileReader.close();
		} catch (FileNotFoundException e) {
			agentId = -1;
			listenPort = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (listenPort == 0) {
			try (ServerSocket freeSocket = new ServerSocket(0)) {
				listenPort = freeSocket.getLocalPort();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		new Thread(new AgentListenerThread(listenPort, ID_FILE)).start();

		if (agentId == -1) {
			register();
		}

		startCli();
	}

	private void register() {
		try (
            Socket socket = new Socket(EM_ADDRESS, EM_PORT);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		) {
			out.println("register" + listenPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startCli() {
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.println("Please select an option: ");
			System.out.println("1. View avaiable topics");
			System.out.println("2. Subscribe to a topic");
			System.out.println("3. Advertise a new topic");
			System.out.println("4. Publish an article");
			System.out.println("5. View notifications");
			System.out.println();

			int selection = 0;
			try {
				selection = Integer.parseInt(stdIn.readLine());
			} catch (NumberFormatException e) {
				System.out.print("\nInvalid input. ");
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}

			String message;

			switch (selection) {
				case 1:
					message = "topics ";
					break;
			
				case 2:
					System.out.println();
					message = "subscribe ";
					break;

				case 3:
					message = "advertise ";
					break;
				
				case 4:
					message = "publish ";
					break;

				default:
					break;
			}
		}

		// stdIn.close();
	}

	@Override
	public void subscribe(Topic topic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subscribe(String keyword) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsubscribe(Topic topic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsubscribe() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listSubscribedTopics() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publish(Event event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void advertise(Topic newTopic) {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) {
		PubSubAgent agent = new PubSubAgent();
	}
	
}
