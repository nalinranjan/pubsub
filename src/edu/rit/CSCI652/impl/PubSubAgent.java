package edu.rit.CSCI652.impl;

/**
 * PubSubAgent.java
 */

import edu.rit.CSCI652.demo.Publisher;
import edu.rit.CSCI652.demo.Subscriber;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class representing a Publish/Subscribe Agent. Acts as both publisher and
 * subscriber.
 * 
 * @author	Nalin Ranjan
 * @author	Sanchitha Seshadri
 * 
 */
public class PubSubAgent implements Publisher, Subscriber{

    private static String EM_ADDRESS = "localhost";
    private static int EM_PORT = 5000;
    private static String ID_FILE = "AgentId";

    private int listenPort;
    private int agentId;
    private BufferedReader stdIn;

    ReentrantLock consoleLock = new ReentrantLock(true);
    Object registerLock = new Object();

    /**
     * Contructor. Registers with the Event Manager and prompts the user for
     * input.
     */
    public PubSubAgent() {
        readIdFile();

        if (listenPort == 0) {
            try (ServerSocket freeSocket = new ServerSocket(0)) {
                listenPort = freeSocket.getLocalPort();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        new Thread(new AgentListenerThread(listenPort, ID_FILE, consoleLock, registerLock)).start();

        if (agentId == -1) {
            register();
            System.out.println("Registering with Event Manager...");
            synchronized (registerLock) {
                try {
                    registerLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        readIdFile();
        checkin();
        startCli();
    }

    /**
     * Reads agent ID and port number from the ID file.
     */
    private void readIdFile() {
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
    }

    /**
     * Sends a message to the Event Manager.
     * 
     * @param	message		The message to be sent
     */
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

    /**
     * Registers a new agent with the Event Manager.
     */
    private void register() {
        sendMessage("register&" + listenPort);
    }

    /**
     * Prompts the user for input on the command line.
     */
    private void startCli() {
        stdIn = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            consoleLock.lock();
            System.out.println("\nPlease select an option: ");
            System.out.println("1. View avaiable topics");
            System.out.println("2. Subscribe by topic");
            System.out.println("3. Subscribe by keyword");
            System.out.println("4. List subscribed topics");
            System.out.println("5. Unsubscribe from a topic");
            System.out.println("6. Unsubscribe from all topics");
            System.out.println("7. Advertise a new topic");
            System.out.println("8. Publish an article");
            System.out.println("9. View notifications");
            System.out.println("10. Quit");
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

            switch (selection) {
                case 1:
                    listAllTopics();
                    break;

                case 2:
                    subscribe();
                    break;
                
                case 3:
                    subscribeKeyword();
                    break;

                case 4:
                    listSubscribedTopics();
                    break;
                
                case 5:
                    unsubscribe();
                    break;

                case 6:
                    unsubscribeAll();
                    break;

                case 7:
                    advertise();
                    break;

                case 8:
                    publish();
                    break;

                case 9:
                    checkin();
                    break;
                
                case 10:
                    System.exit(0);

                default:
                    break;
            }

            consoleLock.unlock();
        }
    }

    /**
     * Sends a request to list all available topics.
     */
    public void listAllTopics() {
        sendMessage("topics&" + agentId);
    }

    /**
     * Sends a request to subscribe to a topic by ID.
     */
    public void subscribe() {
        String message = "subscribe&" + agentId + "&";
        System.out.print("\nEnter topic ID: ");
        try {
            message += Integer.parseInt(stdIn.readLine());
            sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.print("\nInvalid input. ");
        }
    }

    /**
     * Sends a request to subscribe to topics by keyword.
     */
    public void subscribeKeyword() {
        String message = "subscribekeyword&" + agentId + "&";
        System.out.print("\nEnter keyword: ");
        try {
            message += stdIn.readLine();
            sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.print("\nInvalid input. ");
        }
    }

    /**
     * Sends a request to unsubscribe from a topic.
     */
    public void unsubscribe() {
        String message = "unsubscribe&" + agentId + "&";
        System.out.print("\nEnter topic ID: ");
        try {
            message += Integer.parseInt(stdIn.readLine());
            sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.print("\nInvalid input. ");
        }
    }

    /**
     * Sends a request to unsubscribe from all topics.
     */
    public void unsubscribeAll() {
        sendMessage("unsubscribeall&" + agentId);
    }

    /**
     * Sends a request to list all subscribed topics.
     */
    public void listSubscribedTopics() {
        sendMessage("subscribedtopics&" + agentId);
    }

    /**
     * Publishes a new article.
     */
    public void publish() {
        String message = "publish&";
        try {
            System.out.print("\nEnter topic ID: ");
            message += stdIn.readLine() + "&";
            System.out.print("\nEnter title: ");
            message += stdIn.readLine() + "&";
            System.out.print("\nEnter article contents: ");
            message += stdIn.readLine(); 
            sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Advertises a new topic.
     */
    public void advertise() {
        String message = "advertise&";
        try {
            System.out.print("\nEnter topic name: ");
            message += stdIn.readLine() + "&";
            System.out.print("\nEnter keywords: ");
            message += stdIn.readLine(); 
            sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a request to check for pending notifications.
     */
    public void checkin() {
        sendMessage("checkin&" + agentId);
    }

    /**
     * Entry point of the program. Creates a new agent.
     * 
     * @param	args	Command-line arguments
     */
    public static void main(String[] args) {
        new PubSubAgent();
    }
}
