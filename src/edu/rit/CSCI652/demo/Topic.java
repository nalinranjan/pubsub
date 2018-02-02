package edu.rit.CSCI652.demo;

import java.util.List;

public class Topic {
	private int id;
	private List<String> keywords;
	private String name;

	public Topic(int topicID, String topicName, List<String> topicKeywords) {
		this.id = topicID;
		this.keywords = topicKeywords;
		this.name = topicName;
	}

	public int getID()	{
		return this.id;
	}

	public List<String> getKeywords() {
		return this.keywords;
	}

	public String getName() {
		return this.name;
	}
}
