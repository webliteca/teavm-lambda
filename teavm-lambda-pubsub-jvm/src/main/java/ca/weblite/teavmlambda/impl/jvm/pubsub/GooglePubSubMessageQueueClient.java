package ca.weblite.teavmlambda.impl.jvm.pubsub;

import ca.weblite.teavmlambda.api.messagequeue.Message;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * JVM implementation of MessageQueueClient using the Google Cloud Pub/Sub Java SDK.
 *
 * <p>Queue URL mapping:</p>
 * <ul>
 *   <li>For {@code sendMessage}: queueUrl is the topic name (e.g. "my-topic")</li>
 *   <li>For {@code receiveMessages}/{@code deleteMessage}: queueUrl is the subscription name</li>
 * </ul>
 */
public class GooglePubSubMessageQueueClient implements MessageQueueClient {

    private final String projectId;

    GooglePubSubMessageQueueClient(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public String sendMessage(String queueUrl, String messageBody) {
        TopicName topicName = TopicName.of(projectId, queueUrl);
        try {
            Publisher publisher = Publisher.newBuilder(topicName).build();
            try {
                PubsubMessage message = PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8(messageBody))
                        .build();
                ApiFuture<String> future = publisher.publish(message);
                return future.get();
            } finally {
                publisher.shutdown();
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<Message> receiveMessages(String queueUrl, int maxMessages) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, queueUrl);
        try {
            SubscriberStubSettings settings = SubscriberStubSettings.newBuilder().build();
            try (SubscriberStub subscriber = GrpcSubscriberStub.create(settings)) {
                PullRequest pullRequest = PullRequest.newBuilder()
                        .setSubscription(subscriptionName.toString())
                        .setMaxMessages(maxMessages)
                        .build();
                PullResponse response = subscriber.pullCallable().call(pullRequest);

                List<Message> messages = new ArrayList<>();
                for (ReceivedMessage rm : response.getReceivedMessagesList()) {
                    messages.add(new Message(
                            rm.getMessage().getMessageId(),
                            rm.getMessage().getData().toStringUtf8(),
                            rm.getAckId()));
                }
                return messages;
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessage(String queueUrl, String receiptHandle) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, queueUrl);
        try {
            SubscriberStubSettings settings = SubscriberStubSettings.newBuilder().build();
            try (SubscriberStub subscriber = GrpcSubscriberStub.create(settings)) {
                AcknowledgeRequest ackRequest = AcknowledgeRequest.newBuilder()
                        .setSubscription(subscriptionName.toString())
                        .addAckIds(receiptHandle)
                        .build();
                subscriber.acknowledgeCallable().call(ackRequest);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public int getMessageCount(String queueUrl) {
        return -1;
    }
}
