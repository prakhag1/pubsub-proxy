	package com.google.pubsub.proxy.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Example user request json: 
 * { "topic": "test", "messages": [ { "attributes": {
 * "key1": "value1", "key2" : "value2" ... }, "data":
 * "SGVsbG8gQ2xvdWZXNzYWdlIQ==", "messageId": "123", "publishTime":
 * "...timestamp..." } ] }
 */
public class Request {

	@JsonProperty("topic")
	private String topic;
	
	@JsonProperty("messages")
	private List<Message> messages;

	@JsonProperty("topic")
	public String getTopic() {
		return topic;
	}

	@JsonProperty("topic")
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	@JsonProperty("messages")
	public List<Message> getMessages() {
		return messages;
	}

	@JsonProperty("messages")
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}
}
