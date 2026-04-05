package ca.weblite.teavmlambda.demo.messagequeue;

import ca.weblite.teavmlambda.impl.js.lambda.LambdaAdapter;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.impl.js.sqs.SqsMessageQueueProvider;
import ca.weblite.teavmlambda.impl.js.pubsub.PubSubMessageQueueProvider;
import ca.weblite.teavmlambda.generated.GeneratedRouter;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClientFactory;
import org.teavm.jso.JSBody;

public class Main {

    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getenv(String name);

    public static void main(String[] args) {
        // Register message queue providers
        SqsMessageQueueProvider.init();
        PubSubMessageQueueProvider.init();

        // Connection URI determines which backend is used:
        //   sqs://us-east-1           -> SQS
        //   sqs://localhost:9324      -> ElasticMQ (SQS-compatible)
        //   pubsub://my-project-id   -> Google Cloud Pub/Sub
        String mqUri = getenv("MESSAGEQUEUE_URI");
        if (mqUri == null || mqUri.isEmpty()) {
            mqUri = "sqs://us-east-1";
        }

        String queueUrl = getenv("QUEUE_URL");
        if (queueUrl == null || queueUrl.isEmpty()) {
            queueUrl = "http://elasticmq:9324/queue/demo-queue";
        }

        MessageQueueClient client = MessageQueueClientFactory.create(mqUri);

        MessagesResource messagesResource = new MessagesResource(client, queueUrl);
        HealthResource healthResource = new HealthResource();

        Router router = new GeneratedRouter(healthResource, messagesResource);
        LambdaAdapter.start(router);
    }
}
