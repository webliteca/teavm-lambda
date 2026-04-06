package ca.weblite.teavmlambda.demo.messagequeue;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.messagequeue.Message;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;

import java.util.List;

@Path("/messages")
public class MessagesResource {

    private final MessageQueueClient client;
    private final String queueUrl;

    public MessagesResource(MessageQueueClient client, String queueUrl) {
        this.client = client;
        this.queueUrl = queueUrl;
    }

    @POST
    public Response sendMessage(@Body String body) {
        String messageId = client.sendMessage(queueUrl, body);
        return Response.status(201)
                .header("Content-Type", "application/json")
                .body("{\"messageId\":\"" + escapeJson(messageId) + "\"}");
    }

    @GET
    public Response receiveMessages() {
        List<Message> messages = client.receiveMessages(queueUrl, 10);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) json.append(",");
            Message msg = messages.get(i);
            json.append("{\"messageId\":\"").append(escapeJson(msg.getMessageId()))
                .append("\",\"body\":\"").append(escapeJson(msg.getBody()))
                .append("\",\"receiptHandle\":\"").append(escapeJson(msg.getReceiptHandle()))
                .append("\"}");
        }
        json.append("]");
        return Response.ok(json.toString())
                .header("Content-Type", "application/json");
    }

    @DELETE
    @Path("/{receiptHandle}")
    public Response deleteMessage(@PathParam("receiptHandle") String receiptHandle) {
        client.deleteMessage(queueUrl, receiptHandle);
        return Response.status(204);
    }

    @GET
    @Path("/count")
    public Response getMessageCount() {
        int count = client.getMessageCount(queueUrl);
        return Response.ok("{\"count\":" + count + "}")
                .header("Content-Type", "application/json");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
