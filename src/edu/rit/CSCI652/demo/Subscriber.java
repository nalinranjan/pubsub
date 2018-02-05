package edu.rit.CSCI652.demo;

public interface Subscriber {
	/*
	 * subscribe to a topic
	 */
	public void subscribe();

	/*
	 * subscribe by keyword
	 */
	public void subscribeKeyword();
	
	/*
	 * unsubscribe from a topic 
	 */
	public void unsubscribe();
	
	/*
	 * unsubscribe to all subscribed topics
	 */
	public void unsubscribeAll();
	
	/*
	 * show a list of all available topics
	 */
	public void listAllTopics();

	/*
	 * show the list of topics current subscribed to 
	 */
	public void listSubscribedTopics();
	
}
