package edu.rit.CSCI652.demo;

import java.util.List;

public class Event {
	private int id;
	private Topic topic;
	private String title;
	private String content;

	public Event(int id, Topic topic, String title, String content) {
		this.id = id;
		this.topic = topic;
		this.title = title;
		this.content = content;
	}

	public int getID() {
		return this.id;
	}

	public Topic getTopic() {
		return this.topic;
	}

	public String getTitle() {
		return this.title;
	}

	public String getContent() {
		return this.content;
	}
}
