package ca.weblite.teavmlambda.docs.pages.learn;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.Div;
import ca.weblite.teavmreact.html.DomBuilder.Section;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

import ca.weblite.teavmlambda.docs.El;
import ca.weblite.teavmlambda.docs.components.CodeBlock;
import ca.weblite.teavmlambda.docs.components.CodeTabs;
import ca.weblite.teavmlambda.docs.components.Callout;

public class CloudServicesPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Cloud Services"))
            .child(p("teavm-lambda provides platform-neutral APIs for NoSQL databases, "
                + "object storage, and message queues. Each API has implementations for "
                + "AWS and Google Cloud services, discovered automatically at runtime "
                + "via ServiceLoader. Your application code stays cloud-agnostic."))
            .child(sectionFactoryOverview())
            .child(sectionNoSql())
            .child(sectionObjectStorage())
            .child(sectionMessageQueues())
            .child(sectionServiceDiscovery())
            .build();
    }

    private static ReactElement sectionFactoryOverview() {
        return Section.create().className("doc-section")
            .child(h2("Factory Methods and URI Patterns"))
            .child(p("Each cloud service is accessed through a factory class. "
                + "The factory discovers the correct implementation based on the URI "
                + "scheme or the modules on the classpath."))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Factory"),
                            El.classedText("th", "", "AWS Implementation"),
                            El.classedText("th", "", "GCP Implementation"),
                            El.classedText("th", "", "URI Pattern")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("NoSqlDatabaseFactory")),
                            El.classedText("td", "", "DynamoDB"),
                            El.classedText("td", "", "Firestore"),
                            El.classed("td", "", code("dynamodb://table"), El.classedText("span", "", " / "), code("firestore://collection"))
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("ObjectStoreFactory")),
                            El.classedText("td", "", "S3"),
                            El.classedText("td", "", "GCS"),
                            El.classed("td", "", code("s3://bucket"), El.classedText("span", "", " / "), code("gs://bucket"))
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("MessageQueueFactory")),
                            El.classedText("td", "", "SQS"),
                            El.classedText("td", "", "Pub/Sub"),
                            El.classed("td", "", code("sqs://queue-url"), El.classedText("span", "", " / "), code("pubsub://topic"))
                        )
                    )
                )
            ))
            .build();
    }

    private static ReactElement sectionNoSql() {
        String javaCode = """
// Get a NoSQL database instance
NoSqlDatabase nosql = NoSqlDatabaseFactory.create();

// Put an item
Map<String, Object> item = new HashMap<>();
item.put("id", "user-123");
item.put("name", "Alice");
item.put("email", "alice@example.com");
nosql.putItem("users", item);

// Get an item by key
Map<String, Object> user = nosql.getItem("users", "user-123");
String name = (String) user.get("name");

// Query items
List<Map<String, Object>> results = nosql.query("users",
    "email", "alice@example.com"
);

// Delete an item
nosql.deleteItem("users", "user-123");""";

        String kotlinCode = """
// Get a NoSQL database instance
val nosql = NoSqlDatabaseFactory.create()

// Put an item
val item = mapOf(
    "id" to "user-123",
    "name" to "Alice",
    "email" to "alice@example.com"
)
nosql.putItem("users", item)

// Get an item by key
val user = nosql.getItem("users", "user-123")
val name = user["name"] as String

// Query items
val results = nosql.query("users",
    "email", "alice@example.com"
)

// Delete an item
nosql.deleteItem("users", "user-123")""";

        return Section.create().className("doc-section")
            .child(h2("NoSQL Databases"))
            .child(p("The NoSqlDatabase interface provides a uniform API for key-value "
                + "and document stores. On AWS this uses DynamoDB; on Google Cloud it uses "
                + "Firestore. The same application code works with either backend."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionObjectStorage() {
        String javaCode = """
// Get an object store instance
ObjectStore store = ObjectStoreFactory.create();

// Upload an object
byte[] data = "Hello, World!".getBytes();
store.putObject("my-bucket", "hello.txt", data, "text/plain");

// Download an object
byte[] content = store.getObject("my-bucket", "hello.txt");

// List objects with a prefix
List<String> keys = store.listObjects("my-bucket", "uploads/");

// Delete an object
store.deleteObject("my-bucket", "hello.txt");

// Generate a pre-signed URL (time-limited)
String url = store.getSignedUrl("my-bucket", "hello.txt", 3600);""";

        String kotlinCode = """
// Get an object store instance
val store = ObjectStoreFactory.create()

// Upload an object
val data = "Hello, World!".toByteArray()
store.putObject("my-bucket", "hello.txt", data, "text/plain")

// Download an object
val content = store.getObject("my-bucket", "hello.txt")

// List objects with a prefix
val keys = store.listObjects("my-bucket", "uploads/")

// Delete an object
store.deleteObject("my-bucket", "hello.txt")

// Generate a pre-signed URL (time-limited)
val url = store.getSignedUrl("my-bucket", "hello.txt", 3600)""";

        return Section.create().className("doc-section")
            .child(h2("Object Storage"))
            .child(p("The ObjectStore interface abstracts file storage across AWS S3 "
                + "and Google Cloud Storage. Store, retrieve, list, and delete binary "
                + "objects using the same API regardless of provider."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionMessageQueues() {
        String javaCode = """
// Get a message queue instance
MessageQueue queue = MessageQueueFactory.create();

// Send a message
queue.sendMessage("my-queue", "{\\"event\\":\\"order.created\\",\\"orderId\\":\\"42\\"}");

// Receive messages (blocks until available or timeout)
List<QueueMessage> messages = queue.receiveMessages("my-queue", 10);
for (QueueMessage msg : messages) {
    String body = msg.getBody();
    // Process the message...

    // Acknowledge (delete) the message after processing
    queue.deleteMessage("my-queue", msg.getReceiptHandle());
}""";

        String kotlinCode = """
// Get a message queue instance
val queue = MessageQueueFactory.create()

// Send a message
queue.sendMessage("my-queue", "{'event':'order.created','orderId':'42'}")

// Receive messages (blocks until available or timeout)
val messages = queue.receiveMessages("my-queue", 10)
messages.forEach { msg ->
    val body = msg.body
    // Process the message...

    // Acknowledge (delete) the message after processing
    queue.deleteMessage("my-queue", msg.receiptHandle)
}""";

        return Section.create().className("doc-section")
            .child(h2("Message Queues"))
            .child(p("The MessageQueue interface provides a publish/consume API "
                + "for asynchronous messaging. On AWS this maps to SQS; on Google Cloud "
                + "it maps to Pub/Sub. Use it for event-driven architectures, task queues, "
                + "and decoupling services."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionServiceDiscovery() {
        return Section.create().className("doc-section")
            .child(h2("Service Discovery via SPI"))
            .child(p("All cloud service implementations are discovered at runtime using "
                + "Java's ServiceLoader mechanism. When you call a factory method like "
                + "NoSqlDatabaseFactory.create(), it looks for a registered implementation "
                + "on the classpath. You only need to include the correct Maven module for "
                + "your target cloud."))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Service"),
                            El.classedText("th", "", "AWS Module"),
                            El.classedText("th", "", "GCP Module")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classedText("td", "", "NoSQL Database"),
                            El.classed("td", "", code("teavm-lambda-dynamodb")),
                            El.classed("td", "", code("teavm-lambda-firestore"))
                        ),
                        El.classed("tr", "",
                            El.classedText("td", "", "Object Storage"),
                            El.classed("td", "", code("teavm-lambda-s3")),
                            El.classed("td", "", code("teavm-lambda-gcs"))
                        ),
                        El.classed("tr", "",
                            El.classedText("td", "", "Message Queue"),
                            El.classed("td", "", code("teavm-lambda-sqs")),
                            El.classed("td", "", code("teavm-lambda-pubsub"))
                        )
                    )
                )
            ))
            .child(Callout.note("SPI Discovery",
                p("Implementations are discovered at runtime via ServiceLoader. "
                    + "Simply add the correct Maven module for your target cloud provider "
                    + "and the factory will find it automatically. No manual configuration "
                    + "is needed.")))
            .build();
    }
}
