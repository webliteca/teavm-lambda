package ca.weblite.teavmlambda.impl.jvm.sqs;

import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

/**
 * JVM-based MessageQueue provider for Amazon SQS.
 * Discovered via ServiceLoader.
 */
public class AwsSqsMessageQueueProvider implements MessageQueueProvider {

    @Override
    public String getScheme() {
        return "sqs";
    }

    @Override
    public MessageQueueClient create(String uri) {
        String remainder = uri.substring("sqs://".length());

        SqsClientBuilder builder = SqsClient.builder();

        if (remainder.contains(":")) {
            builder.endpointOverride(URI.create("http://" + remainder))
                    .region(Region.US_EAST_1);
        } else {
            builder.region(Region.of(remainder));
        }

        return new AwsSqsMessageQueueClient(builder.build());
    }
}
