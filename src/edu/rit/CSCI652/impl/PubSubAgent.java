package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Publisher;
import edu.rit.CSCI652.demo.Subscriber;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class PubSubAgent implements Publisher, Subscriber{

	private static String EM_ADDRESS = "localhost";
	private static int EM_PORT = 5000;
	private static String ID_FILE = "AgentId";

	private int listenPort;
	private int agentId;
	private BufferedReader stdIn;

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

	private void sendMessage(String message) {
		try (
            Socket socket = new Socket(EM_ADDRESS, EM_PORT);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		) {
			out.println(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void register() {
		sendMessage("register&" + listenPort);
	}

	private void startCli() {
		stdIn = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.println("Please select an option: ");
			System.out.println("1. View avaiable topics");
			System.out.println("2. Subscribe to a topic");
			System.out.println("3. List subsribed topics");
			System.out.println("4. Unsubscribe from a topic");
			System.out.println("5. Unsubscribe from all topics");
			System.out.println("6. Advertise a new topic");
			System.out.println("7. Publish an article");
			System.out.println("8. View notifications");
			System.out.println("9. Quit");
			System.out.print("\n> ");

			int selection = 0;
			try {
				selection = Integer.parseInt(stdIn.readLine());
			} catch (NumberFormatException e) {
				System.out.print("\nInvalid input. ");
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}

			String message = "";

			switch (selection) {
				case 1:
					listAllTopics();
					break;

				case 2:
					subscribe();
					break;

				case 3:
					listSubscribedTopics();
					break;
				
				case 4:
					unsubscribe();
					break;

				case 5:
					unsubscribeAll();
					break;

				case 6:
					advertise();
					break;

				case 7:
					publish();
					break;
				
				case 9:
					System.exit(0);

				default:
					break;
			}
		}
	}

	public void listAllTopics() {
		sendMessage("topics");
	}

	@Override
	public void subscribe() {
		String message = "subscribe&" + agentId + "&";
		System.out.println("\nEnter topic ID: ");
		try {
			message += Integer.parseInt(stdIn.readLine());
			sendMessage(message);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.out.print("\nInvalid input. ");
		}
	}

	@Override
	public void unsubscribe() {
		String message = "unsubscribe&" + agentId + "&";
		System.out.println("\nEnter topic ID: ");
		try {
			message += Integer.parseInt(stdIn.readLine());
			sendMessage(message);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.out.print("\nInvalid input. ");
		}
	}

	@Override
	public void unsubscribeAll() {
		sendMessage("unsubscribeall&" + agentId);
	}

	@Override
	public void listSubscribedTopics() {
		sendMessage("subscribedtopics&" + agentId);
	}

	@Override
	public void publish() {
		String message = "publish&";
		try {
			System.out.println("\nEnter title: ");
			message += stdIn.readLine() + "&";
			System.out.println("\nEnter topic ID: ");
			message += stdIn.readLine() + "&";
			System.out.println("\nEnter article contents: ");
			message += stdIn.readLine(); 
			sendMessage(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void advertise() {
		String message = "advertise&";
		try {
			System.out.println("\nEnter topic name: ");
			message += stdIn.readLine() + "&";
			System.out.println("\nEnter keywords: ");
			message += stdIn.readLine(); 
			sendMessage(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		PubSubAgent agent = new PubSubAgent();
	}
}
