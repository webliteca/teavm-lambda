package ca.weblite.teavmlambda.api.messagequeue;

/**
 * A message received from a message queue.
 */
public final class Message {

    private final String messageId;
    private final String body;
    private final String receiptHandle;

    public Message(String messageId, String body, String receiptHandle) {
        this.messageId = messageId;
        this.body = body;
        this.receiptHandle = receiptHandle;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getBody() {
        return body;
    }

    /**
     * Returns the receipt handle used to delete/acknowledge this message.
     */
    public String getReceiptHandle() {
        return receiptHandle;
    }
}
