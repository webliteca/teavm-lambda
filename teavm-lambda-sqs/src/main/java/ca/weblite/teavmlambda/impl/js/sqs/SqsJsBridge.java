package ca.weblite.teavmlambda.impl.js.sqs;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Low-level JavaScript interop for the AWS SDK v3 SQS Client.
 * Uses @aws-sdk/client-sqs for message queue operations.
 */
final class SqsJsBridge {

    private SqsJsBridge() {
    }

    @JSBody(params = {"region", "endpoint"}, script =
            "var sqs = require('@aws-sdk/client-sqs');"
            + "var config = { region: region };"
            + "if (endpoint) { config.endpoint = endpoint; }"
            + "return new sqs.SQSClient(config);")
    static native JSObject createClient(String region, String endpoint);

    @JSBody(params = {"client", "queueUrl", "messageBody"}, script =
            "var sqs = require('@aws-sdk/client-sqs');"
            + "return client.send(new sqs.SendMessageCommand({"
            + "  QueueUrl: queueUrl, MessageBody: messageBody"
            + "}));")
    static native JSPromise<JSObject> sendMessage(JSObject client, String queueUrl, String messageBody);

    @JSBody(params = {"client", "queueUrl", "maxMessages"}, script =
            "var sqs = require('@aws-sdk/client-sqs');"
            + "return client.send(new sqs.ReceiveMessageCommand({"
            + "  QueueUrl: queueUrl, MaxNumberOfMessages: maxMessages,"
            + "  WaitTimeSeconds: 0"
            + "}));")
    static native JSPromise<JSObject> receiveMessages(JSObject client, String queueUrl, int maxMessages);

    @JSBody(params = {"client", "queueUrl", "receiptHandle"}, script =
            "var sqs = require('@aws-sdk/client-sqs');"
            + "return client.send(new sqs.DeleteMessageCommand({"
            + "  QueueUrl: queueUrl, ReceiptHandle: receiptHandle"
            + "}));")
    static native JSPromise<JSObject> deleteMessage(JSObject client, String queueUrl, String receiptHandle);

    @JSBody(params = {"client", "queueUrl"}, script =
            "var sqs = require('@aws-sdk/client-sqs');"
            + "return client.send(new sqs.GetQueueAttributesCommand({"
            + "  QueueUrl: queueUrl,"
            + "  AttributeNames: ['ApproximateNumberOfMessages']"
            + "}));")
    static native JSPromise<JSObject> getQueueAttributes(JSObject client, String queueUrl);

    @JSBody(params = {"obj", "key"}, script = "return obj[key];")
    static native JSObject getProperty(JSObject obj, String key);

    @JSBody(params = {"obj", "key"}, script = "return String(obj[key]);")
    static native String getStringProperty(JSObject obj, String key);

    @JSBody(params = {"obj", "key"}, script = "return obj[key] != null;")
    static native boolean hasProperty(JSObject obj, String key);

    @JSBody(params = {"arr"}, script = "return arr ? arr.length : 0;")
    static native int arrayLength(JSObject arr);

    @JSBody(params = {"arr", "index"}, script = "return arr[index];")
    static native JSObject arrayGet(JSObject arr, int index);
}
