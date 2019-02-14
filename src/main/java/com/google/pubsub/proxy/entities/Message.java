package com.google.pubsub.proxy.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "attributes", "data", "messageId", "publishTime" })
	public class Message {

		@JsonProperty("attributes")
		private Object attributes;

		@JsonProperty("data")
		private String data;

		@JsonProperty("messageId")
		private String messageId;

		@JsonProperty("publishTime")
		private String publishTime;

		@JsonProperty("attributes")
		public Object getAttributes() {
			return attributes;
		}

		@JsonProperty("attributes")
		public void setAttributes(Object attributes) {
			this.attributes = attributes;
		}

		@JsonProperty("data")
		public String getData() {
			return data;
		}

		@JsonProperty("data")
		public void setData(String data) {
			this.data = data;
		}

		@JsonProperty("messageId")
		public String getMessageId() {
			return messageId;
		}

		@JsonProperty("messageId")
		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}

		@JsonProperty("publishTime")
		public String getPublishTime() {
			return publishTime;
		}

		@JsonProperty("publishTime")
		public void setPublishTime(String publishTime) {
			this.publishTime = publishTime;
		}
	}