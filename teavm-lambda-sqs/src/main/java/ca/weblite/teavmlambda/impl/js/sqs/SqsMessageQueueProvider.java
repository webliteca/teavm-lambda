package ca.weblite.teavmlambda.impl.js.sqs;

import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClientFactory;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueProvider;
import org.teavm.jso.JSObject;

/**
 * Message queue provider for Amazon SQS.
 *
 * <p>Connection URI formats:</p>
 * <ul>
 *   <li>{@code sqs://us-east-1} - SQS in us-east-1</li>
 *   <li>{@code sqs://localhost:9324} - ElasticMQ / SQS-compatible local endpoint</li>
 * </ul>
 */
public class SqsMessageQueueProvider implements MessageQueueProvider {

    static {
        MessageQueueClientFactory.register(new SqsMessageQueueProvider());
    }

    /**
     * Forces class initialization, which triggers provider registration.
     */
    public static void init() {
        // static initializer does the work
    }

    @Override
    public String getScheme() {
        return "sqs";
    }

    @Override
    public MessageQueueClient create(String uri) {
        String remainder = uri.substring("sqs://".length());

        String region;
        String endpoint = null;

        if (remainder.contains(":")) {
            endpoint = "http://" + remainder;
            region = "us-east-1";
        } else {
            region = remainder;
        }

        JSObject client = SqsJsBridge.createClient(region, endpoint);
        return new SqsMessageQueueClient(client);
    }
}
