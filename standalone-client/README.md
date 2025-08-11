# Standalone gRPC User Service Client

This is a standalone Java gRPC client that demonstrates how to connect to the gRPC Learning Service from a completely separate project. Unlike the embedded clients in the main project, this client:

- **Has no dependencies on the server project**
- **Uses standard gRPC Java libraries** (no Quarkus-specific code)
- **Can be packaged and distributed independently**
- **Demonstrates real-world client-server separation**

## Key Differences from Embedded Client

| Aspect             | Embedded Client                 | Standalone Client                 |
| ------------------ | ------------------------------- | --------------------------------- |
| **Dependencies**   | Uses server's generated classes | Generates own classes from proto  |
| **Reactive Types** | Uses Mutiny (Quarkus-specific)  | Uses standard gRPC StreamObserver |
| **Packaging**      | Part of server project          | Independent JAR                   |
| **Distribution**   | Coupled to server               | Can be distributed separately     |
| **Use Case**       | Development/testing             | Production client applications    |

## How It Works

### 1. Proto File Contract

The client gets the API contract through the `.proto` file:

```
standalone-client/src/main/proto/user.proto
```

This file defines:

- Message structures (User, CreateUserRequest, etc.)
- Service methods (CreateUser, ListUsers, etc.)
- Streaming patterns

### 2. Code Generation

When you build the project, the protobuf Maven plugin generates:

- **Message classes**: `UserProto.User`, `UserProto.CreateUserRequest`, etc.
- **Service stubs**: `UserServiceGrpc.UserServiceBlockingStub`, `UserServiceGrpc.UserServiceStub`

### 3. gRPC Communication

The client uses these generated classes to communicate with the server:

```java
// Create channel (connection to server)
ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9000)
    .usePlaintext()
    .build();

// Create stub (interface to service)
UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);

// Make calls
UserProto.User user = stub.createUser(request);
```

## Building and Running

### Prerequisites

- Java 17 or later
- Maven 3.8+
- The gRPC Learning Service running on localhost:9000

### Build the Client

```bash
cd standalone-client

# Generate gRPC classes and compile
mvn clean compile

# You can see the generated classes at:
ls target/generated-sources/protobuf/java/org/isaac/grpc/user/
```

### Run the Client

```bash
# Run with Maven
mvn exec:java

# Or compile and run directly
mvn clean package
java -cp target/classes:target/dependency/* org.isaac.client.StandaloneUserClient
```

### Expected Output

```
=== Starting Standalone gRPC Client ===

--- Demonstrating Unary Operations (Blocking) ---
Creating a new user...
Created user: User{id='uuid-123', name='Standalone Client User', email='standalone@example.com', ...}
Retrieving user by ID...
Retrieved user: User{id='uuid-123', name='Standalone Client User', email='standalone@example.com', ...}
...

--- Demonstrating Server Streaming ---
Creating test users for streaming demonstration...
Starting server streaming - listing all users...
Received user from stream: User{id='uuid-456', name='Stream User 1', ...}
...

--- Demonstrating Client Streaming ---
Sending multiple user creation requests...
Sent request for: Alice Standalone
Client streaming completed:
  Created count: 3
...

--- Demonstrating Bidirectional Streaming ---
Sent subscription request
Creating users to trigger notifications...
Received notification: UserNotification{type=CREATED, user=User{...}, ...}
...

=== Standalone gRPC Client Complete ===
```

## Code Structure

### Main Components

1. **StandaloneUserClient.java**: Main client class demonstrating all gRPC patterns
2. **pom.xml**: Maven configuration with gRPC dependencies and protobuf plugin
3. **src/main/proto/user.proto**: Service definition (copied from server)

### gRPC Patterns Demonstrated

#### 1. Unary Calls (Request-Response)

```java
// Synchronous call
UserProto.User user = blockingStub.createUser(request);
```

#### 2. Server Streaming (One Request → Multiple Responses)

```java
// Asynchronous streaming
asyncStub.listUsers(empty, new StreamObserver<UserProto.User>() {
    @Override
    public void onNext(UserProto.User user) {
        // Handle each user
    }

    @Override
    public void onCompleted() {
        // Stream finished
    }
});
```

#### 3. Client Streaming (Multiple Requests → One Response)

```java
// Get request stream
StreamObserver<UserProto.CreateUserRequest> requestObserver =
    asyncStub.createUsers(responseObserver);

// Send multiple requests
requestObserver.onNext(request1);
requestObserver.onNext(request2);
requestObserver.onCompleted();
```

#### 4. Bidirectional Streaming (Multiple ↔ Multiple)

```java
// Both client and server can send multiple messages
StreamObserver<UserProto.SubscribeRequest> subscriptionObserver =
    asyncStub.subscribeToUserUpdates(notificationObserver);

subscriptionObserver.onNext(subscribeRequest);
// Server sends notifications back through notificationObserver
```

## Real-World Usage Scenarios

### 1. Microservice Client

```java
@Component
public class UserServiceClient {
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserServiceClient() {
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress("user-service", 9000)
            .usePlaintext()
            .build();
        this.userStub = UserServiceGrpc.newBlockingStub(channel);
    }

    public User createUser(String name, String email) {
        CreateUserRequest request = CreateUserRequest.newBuilder()
            .setName(name)
            .setEmail(email)
            .build();
        return userStub.createUser(request);
    }
}
```

### 2. CLI Tool

```java
public class UserCLI {
    public static void main(String[] args) {
        if (args[0].equals("create")) {
            // Create user from command line
            StandaloneUserClient client = new StandaloneUserClient();
            // ... create user logic
        }
    }
}
```

### 3. Web Application Client

```java
@RestController
public class UserController {
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto dto) {
        CreateUserRequest grpcRequest = CreateUserRequest.newBuilder()
            .setName(dto.getName())
            .setEmail(dto.getEmail())
            .build();

        User grpcUser = userStub.createUser(grpcRequest);
        return ResponseEntity.ok(toDto(grpcUser));
    }
}
```

## Distribution and Deployment

### Creating a Standalone JAR

```bash
# Create fat JAR with all dependencies
mvn clean package assembly:single

# Run the standalone JAR
java -jar target/grpc-user-client-1.0.0-jar-with-dependencies.jar
```

### Docker Container

```dockerfile
FROM openjdk:17-jre-slim

COPY target/grpc-user-client-1.0.0-jar-with-dependencies.jar app.jar

# Configure server connection
ENV GRPC_SERVER_HOST=user-service
ENV GRPC_SERVER_PORT=9000

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Maven Dependency

Other projects can use this client as a dependency:

```xml
<dependency>
    <groupId>org.isaac.client</groupId>
    <artifactId>grpc-user-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Best Practices Demonstrated

1. **Connection Management**: Proper channel creation and shutdown
2. **Error Handling**: StatusRuntimeException handling
3. **Streaming Patterns**: Correct use of StreamObserver
4. **Resource Cleanup**: Proper channel shutdown
5. **Timeout Handling**: Appropriate timeouts for operations
6. **Logging**: Comprehensive logging for debugging

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure server is running on localhost:9000
2. **Class Not Found**: Run `mvn clean compile` to generate gRPC classes
3. **Timeout**: Increase timeout or check server responsiveness

### Debug Mode

```bash
# Enable gRPC debug logging
java -Djava.util.logging.config.file=logging.properties -jar app.jar
```

This standalone client demonstrates how gRPC clients work in real-world scenarios where the client and server are completely separate projects.
