package edu.rit.CSCI652.demo;

public interface Publisher {
	/*
	 * publish an event of a specific topic with title and content
	 */
	public void publish();
	
	/*
	 * advertise new topic
	 */
	public void advertise();
}
