# NoSQL, Object Storage, and Message Queues

> Read this when the user needs DynamoDB, Firestore, S3, GCS, SQS, or Pub/Sub integration.

All cloud services follow the same pattern: platform-neutral API module + implementation modules discovered via ServiceLoader. Register the client on the Container before constructing GeneratedRouter.

---

## NoSQL (DynamoDB / Firestore)

### Setup

```java
NoSqlClient nosql = NoSqlClientFactory.create("dynamodb://us-east-1");
// or: NoSqlClientFactory.create("dynamodb://localhost:8000")   // local DynamoDB
// or: NoSqlClientFactory.create("firestore://my-project-id")

container.register(NoSqlClient.class, nosql);
```

**Maven modules**:
- Always: `teavm-lambda-nosqldb`
- One of: `teavm-lambda-dynamodb` or `teavm-lambda-firestore`

### API

```java
// Store a document
nosql.put("users", "user-123", "{\"name\":\"Alice\",\"email\":\"a@b.com\"}");

// Retrieve by ID (returns JSON string or null)
String json = nosql.get("users", "user-123");

// Delete
nosql.delete("users", "user-123");

// Query with filter (operators: =, <, >, <=, >=, begins_with)
List<String> results = nosql.query("users", "email", "=", "a@b.com");

// List all documents
List<String> all = nosql.list("users");
```

---

## Object Storage (S3 / GCS)

### Setup

```java
ObjectStoreClient store = ObjectStoreClientFactory.create("s3://us-east-1");
// or: ObjectStoreClientFactory.create("s3://localhost:9000")   // MinIO
// or: ObjectStoreClientFactory.create("gcs://my-project-id")

container.register(ObjectStoreClient.class, store);
```

**Maven modules**:
- Always: `teavm-lambda-objectstore`
- For S3: `teavm-lambda-s3` (Node.js) or `teavm-lambda-s3-jvm` (JVM)
- For GCS: `teavm-lambda-gcs` (Node.js) or `teavm-lambda-gcs-jvm` (JVM)

### API

```java
// Upload text
store.putObject("my-bucket", "data/file.json", jsonString, "application/json");

// Upload binary
store.putObjectBytes("my-bucket", "images/photo.jpg", imageBytes, "image/jpeg");

// Download text (returns UTF-8 string or null)
String content = store.getObject("my-bucket", "data/file.json");

// Download binary (returns bytes or null)
byte[] bytes = store.getObjectBytes("my-bucket", "images/photo.jpg");

// Check existence
boolean exists = store.objectExists("my-bucket", "data/file.json");

// List objects by prefix
List<String> keys = store.listObjects("my-bucket", "data/");

// Delete
store.deleteObject("my-bucket", "data/file.json");
```

---

## Message Queues (SQS / Pub/Sub)

### Setup

```java
MessageQueueClient mq = MessageQueueClientFactory.create("sqs://us-east-1");
// or: MessageQueueClientFactory.create("sqs://localhost:9324")    // ElasticMQ
// or: MessageQueueClientFactory.create("pubsub://my-project-id")

container.register(MessageQueueClient.class, mq);
```

**Maven modules**:
- Always: `teavm-lambda-messagequeue`
- For SQS: `teavm-lambda-sqs` (Node.js) or `teavm-lambda-sqs-jvm` (JVM)
- For Pub/Sub: `teavm-lambda-pubsub` (Node.js) or `teavm-lambda-pubsub-jvm` (JVM)

### API

```java
// Send a message (returns message ID)
String messageId = mq.sendMessage("my-queue-url", "{\"action\":\"process\",\"id\":\"123\"}");

// Receive messages (max 1-10)
List<Message> messages = mq.receiveMessages("my-queue-url", 5);
for (Message msg : messages) {
    String body = msg.getBody();
    String id = msg.getMessageId();

    // Process message ...

    // Acknowledge/delete so it's not redelivered
    mq.deleteMessage("my-queue-url", msg.getReceiptHandle());
}

// Check queue depth
int count = mq.getMessageCount("my-queue-url");  // -1 if unsupported
```

---

## Environment Variables

| Service | Variable | Example |
|---------|----------|---------|
| NoSQL | `NOSQL_URI` | `dynamodb://us-east-1` |
| Object Storage | `OBJECTSTORE_URI` | `s3://us-east-1` |
| Message Queue | `MQ_URI` | `sqs://us-east-1` |

These are conventions used in examples — the library itself reads whatever URI you pass to the factory.
