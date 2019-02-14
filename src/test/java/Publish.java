import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

public class Publish {

	private static Publisher publisher;
	private static String PROJECT_ID = ServiceOptions.getDefaultProjectId();

	public static void main(String[] args) throws Exception {
		try {
			// Create a publisher instance with default settings bound to the topic
			publisher = Publisher.newBuilder(ProjectTopicName.of(PROJECT_ID, "test")).build();

			List<String> messages = Arrays.asList("123", "pqr");
			final long startTime = System.currentTimeMillis();

			for (final String message : messages) {
				ByteString data = ByteString.copyFromUtf8(message);
				PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

				// Once published, returns a server-assigned message id (unique within the
				// topic)
				ApiFuture<String> future = publisher.publish(pubsubMessage);

				// Add an asynchronous callback to handle success / failure
				ApiFutures.addCallback(future, new ApiFutureCallback<String>() {

					public void onFailure(Throwable throwable) {
						if (throwable instanceof ApiException) {
							ApiException apiException = ((ApiException) throwable);
							// details on the API exception
							System.out.println(apiException.getStatusCode().getCode());
							System.out.println(apiException.isRetryable());
						}
						System.out.println("Error publishing message : " + message);
					}

					public void onSuccess(String messageId) {
						// Once published, returns server-assigned message ids (unique within the topic)
						System.out.println(messageId);
						long stopTime = System.currentTimeMillis();
						System.out.println("Time taken: " + (stopTime - startTime));

					}
				}, MoreExecutors.directExecutor());
			}
		} finally {
			if (publisher != null) {
				// When finished with the publisher, shutdown to free up resources.
				publisher.shutdown();
				publisher.awaitTermination(1, TimeUnit.MINUTES);
			}
		}
		

		/*ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(PROJECT_ID, "test");
		// Instantiate an asynchronous message receiver
		MessageReceiver receiver =
		    new MessageReceiver() {
		      public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
		        // handle incoming message, then ack/nack the received message
		        System.out.println("Id : " + message.getMessageId());
		        System.out.println("Data : " + message.getData().toStringUtf8());
		        consumer.ack();
		      }
		    };

		Subscriber subscriber = null;
		try {
		  // Create a subscriber for "my-subscription-id" bound to the message receiver
		  subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
		  subscriber.startAsync();
		  // ...
		} finally {
		  // stop receiving messages
		  if (subscriber != null) {
		    subscriber.stopAsync();
		  }
		}*/
		
	}
}
