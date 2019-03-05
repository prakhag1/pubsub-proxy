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
 * Post validation, this class is used to forward request downstream to Google
 * Cloud PubSub
 */
@Path("/publish")
public class PublishMessage {

	// Carries all variables initialized during server init.
	// See com.google.pubsub.proxy.server.ServletInit for more details
	@Context
	ServletContext ctx;
	// Locally store topic-to-publisher handler
	private HashMap<String, Publisher> publishers = new HashMap<String, Publisher>();
	private static final Logger LOGGER = Logger.getLogger(PublishMessage.class.getName());
	private static final String projectId = ServiceOptions.getDefaultProjectId();

	/**
	 * Post authentication, pass through the request to Google Cloud PubSub Request
	 * data is captured in Request POJO. For the passed message format, please refer
	 * com.google.pubsub.proxy.entities.Request
	 * 
	 * @param message
	 * @return
	 * @throws Exception
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateAccessToken
	public Response doPost(Request req) throws Exception {

		// Check for missing fields which are mandatory
		if (null == req.getTopic() || null == req.getMessages() || req.getMessages().isEmpty())
			throw new MissingRequiredFieldsException();

		try {
			// Get publisher
			Publisher publisher = getPublisher(req.getTopic());

			// Publish message
			for (final Message msg : req.getMessages()) {
				publishMessage(publisher, msg);
			}

		} catch (Exception ex) {
			throw new GenericAPIException(ex);
		}
		// If no exception is caught, 200OK is returned to the client without
		// waiting for what happens downstream. Failed messages are captured in a BQ
		// sink for a postmortem
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

		// Async call to PubSub
		ApiFuture<String> future = publisher.publish(builder.build());
		ApiFutures.addCallback(future, new ApiFutureCallback<String>() {

			// Failed to publish messages downstream
			public void onFailure(Throwable throwable) {
				// Write failed msgs to a sink: BQ in this case
				if (throwable instanceof ApiException) {
					ApiException apiException = ((ApiException) throwable);
					LOGGER.severe("Failed to publish message: " + apiException.getMessage());
					PublishMessageUtils.insertFailedMessagesInBQ(msg, apiException, ctx);
				}
			}

			// Successfully published messages downstream
			public void onSuccess(String msgId) {
				LOGGER.info("Successfully published: " + msgId);
			}

		}, MoreExecutors.directExecutor());
	}

	/**
	 * A long living publisher object needed. Creating new publisher on each request
	 * would be extremely expensive resulting in performance degradation and
	 * resource overhead
	 * 
	 * @param topic
	 * @return
	 * @throws Exception
	 */
	private Publisher getPublisher(String topic) throws IOException {

		// Check if publisher against a topic already exists
		if (!publishers.containsKey(topic)) {
			// Double checked locking to prevent any race conditions on publisher creation
			synchronized (PublishMessage.class) {
				if (!publishers.containsKey(topic)) {
					LOGGER.info("Creating new publisher for: " + topic);
					// Create new publisher
					try {
						Publisher publisher = Publisher.newBuilder(ProjectTopicName.of(projectId, topic)).build();
						// Save publisher for later use
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