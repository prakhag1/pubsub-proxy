/* Copyright 2019 Google Inc. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License. */

package com.google.pubsub.proxy.actions.publish;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.proxy.entities.Message;
import com.google.pubsub.proxy.entities.Request;
import com.google.pubsub.proxy.exceptions.GenericAPIException;
import com.google.pubsub.proxy.exceptions.MissingRequiredFieldsException;
import com.google.pubsub.proxy.util.PublishMessageUtils;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PubsubMessage.Builder;

/**
 * Post validation, this class is used to forward 
 * request downstream to Google Cloud PubSub
 */
@Path("/publish")
public class PublishMessage {
	@Context
	ServletContext ctx;
	
	private HashMap<String, Publisher> publishers = new HashMap<String, Publisher>();
	private static final Logger LOGGER = Logger.getLogger(PublishMessage.class.getName());
	private static final String projectId = ServiceOptions.getDefaultProjectId();
	/**
	 * Post authentication, pass through the request to Google Cloud PubSub Request
	 * data is captured in Request POJO. For the passed message format look at
	 * com.google.pubsub.proxy.entities.Request
	 * @param message
	 * @return
	 * @throws Exception
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateAccessToken
	public Response doPost(Request req) throws Exception {
		if (null == req.getTopic() || null == req.getMessages() || req.getMessages().isEmpty()) {
			throw new MissingRequiredFieldsException();
		}
		try {
			Publisher publisher = getPublisher(req.getTopic());
			for (final Message msg : req.getMessages()) {
				publishMessage(publisher, msg);
			}
		} catch (Exception ex) {
			throw new GenericAPIException(ex);
		}
		return Response.ok().build();
	}
	/**
	 * 
	 * @param publisher
	 * @param msg
	 * @throws GenericAPIException
	 * @throws InterruptedException
	 */
	private void publishMessage(Publisher publisher, Message msg) throws GenericAPIException {
		Builder builder = PubsubMessage.newBuilder();
		if (null != msg.getData()) {
			builder.setData(ByteString.copyFromUtf8(msg.getData()));
		}
		if (null != msg.getMessageId()) {
			builder.setMessageId(msg.getMessageId());
		}
		if (null != msg.getPublishTime()) {
			builder.setPublishTime(PublishMessageUtils.getTimeStamp(msg.getPublishTime()));
		}
		if (null != msg.getAttributes()) {
			builder.putAllAttributes(PublishMessageUtils.getAllAttributes(msg.getAttributes()));
		}
		ApiFuture<String> future = publisher.publish(builder.build());
		ApiFutures.addCallback(future, new ApiFutureCallback<String>() {
			public void onFailure(Throwable throwable) {
				if (throwable instanceof ApiException) {
					ApiException apiException = ((ApiException) throwable);
					LOGGER.severe("Failed to publish message: " + apiException.getMessage());
				}
			}
			public void onSuccess(String msgId) {
				LOGGER.info("Successfully published: " + msgId);
			}

		}, MoreExecutors.directExecutor());
	}
	/**
	 * A long living publisher object created since 
	 * creating a new publisher on each request
	 * would be extremely expensive resulting in 
	 * performance degradation and resource overhead
	 * @param topic
	 * @return
	 * @throws Exception
	 */
	private Publisher getPublisher(String topic) throws IOException {
		// Check if publisher against a topic already exists
		if (!publishers.containsKey(topic)) {
			// Double checked locking to prevent any race condition
			synchronized (PublishMessage.class) {
				if (!publishers.containsKey(topic)) {
					LOGGER.info("Creating new publisher for: " + topic);
					try {
						Publisher publisher = Publisher.newBuilder(
								ProjectTopicName.of(projectId, topic)).build();
						publishers.put(topic, publisher);
						return publisher;
					} catch (IOException e) {
						LOGGER.severe("Cannot create publisher: " + e.getMessage());
						throw e;
					}
				}
			}
		}
		return publishers.get(topic);
	}
}