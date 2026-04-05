package ca.weblite.teavmlambda.impl.js.pubsub;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Low-level JavaScript interop for the Google Cloud Pub/Sub Node.js SDK
 * ({@code @google-cloud/pubsub}).
 *
 * <p>Pub/Sub uses topics for publishing and subscriptions for receiving.
 * The queueUrl parameter in the MessageQueueClient API maps to:
 * - For sendMessage: the topic name (e.g. "my-topic")
 * - For receiveMessages/deleteMessage: the subscription name (e.g. "my-subscription")
 */
final class PubSubJsBridge {

    private PubSubJsBridge() {
    }

    @JSBody(params = {"projectId"}, script =
            "var PubSub = require('@google-cloud/pubsub').PubSub;"
            + "var config = {};"
            + "if (projectId) { config.projectId = projectId; }"
            + "return new PubSub(config);")
    static native JSObject createClient(String projectId);

    @JSBody(params = {"pubsub", "topicName", "messageData"}, script =
            "var buffer = Buffer.from(messageData);"
            + "return pubsub.topic(topicName).publishMessage({ data: buffer })"
            + ".then(function(messageId) { return { messageId: messageId }; });")
    static native JSPromise<JSObject> publishMessage(JSObject pubsub, String topicName, String messageData);

    @JSBody(params = {"pubsub", "subscriptionName", "maxMessages"}, script =
            "return pubsub.subscription(subscriptionName)"
            + ".pull({ maxMessages: maxMessages, returnImmediately: true })"
            + ".then(function(data) {"
            + "  var messages = data[0] || [];"
            + "  return messages.map(function(msg) {"
            + "    return {"
            + "      messageId: msg.id,"
            + "      body: msg.data.toString(),"
            + "      receiptHandle: msg.ackId"
            + "    };"
            + "  });"
            + "});")
    static native JSPromise<JSObject> pullMessages(JSObject pubsub, String subscriptionName, int maxMessages);

    @JSBody(params = {"pubsub", "subscriptionName", "ackId"}, script =
            "return pubsub.subscription(subscriptionName)"
            + ".ack([ackId]);")
    static native JSPromise<JSObject> acknowledge(JSObject pubsub, String subscriptionName, String ackId);

    @JSBody(params = {"obj", "key"}, script = "return String(obj[key]);")
    static native String getStringProperty(JSObject obj, String key);

    @JSBody(params = {"arr"}, script = "return arr ? arr.length : 0;")
    static native int arrayLength(JSObject arr);

    @JSBody(params = {"arr", "index"}, script = "return arr[index];")
    static native JSObject arrayGet(JSObject arr, int index);
}
