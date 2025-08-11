# gRPC Learning Service - Client Usage Guide

This comprehensive guide demonstrates how to use the various client applications provided with the gRPC Learning Service. Each client showcases different aspects of gRPC communication patterns and real-world usage scenarios.

## Table of Contents

1. [Getting Started](#getting-started)
2. [gRPC Client Applications](#grpc-client-applications)
3. [REST Client Examples](#rest-client-examples)
4. [API Reference](#api-reference)
5. [Common Scenarios](#common-scenarios)
6. [Troubleshooting](#troubleshooting)

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.8+
- The gRPC Learning Service running locally
  - gRPC server on port 9000
  - REST server on port 8080

### Starting the Service

```bash
# Start the service in development mode
./mvnw quarkus:dev

# Or build and run
./mvnw clean package
java -jar target/quarkus-app/quarkus-run.jar
```

### Verify Service is Running

```bash
# Check gRPC server (requires grpcurl)
grpcurl -plaintext localhost:9000 list

# Check REST endpoints
curl http://localhost:8080/api/users
```

## gRPC Client Applications

### 1. Comprehensive gRPC Client (`UserGrpcClient`)

This is the main demonstration client that showcases all four gRPC communication patterns.

#### Running the Client

```bash
# Compile and run
mvn compile exec:java -Dexec.mainClass="org.isaac.client.UserGrpcClient"

# Or run from IDE
# Main class: org.isaac.client.UserGrpcClient
```

#### What It Demonstrates

**Unary Operations (Request-Response)**

- Create a user
- Retrieve a user by ID
- Update user information
- Delete a user

**Server Streaming (Single Request → Stream of Responses)**

- List all users with each user streamed individually
- Demonstrates handling of streaming responses

**Client Streaming (Stream of Requests → Single Response)**

- Batch creation of multiple users
- Shows how to send multiple requests and receive a summary

**Bidirectional Streaming (Stream ↔ Stream)**

- Real-time user update notifications
- Demonstrates full-duplex communication

#### Sample Output

```
=== Starting gRPC Client Demonstration ===

--- Demonstrating Unary Operations ---
Creating a new user...
Created user: User{id='uuid-123', name='John Doe', email='john.doe@example.com', createdAt=1642234567000, updatedAt=1642234567000}
Retrieving user by ID...
Retrieved user: User{id='uuid-123', name='John Doe', email='john.doe@example.com', createdAt=1642234567000, updatedAt=1642234567000}
...

--- Demonstrating Server Streaming ---
Starting server streaming - listing all users...
Received 5 users via server streaming:
  - User{id='uuid-123', name='John Doe', email='john.doe@example.com', ...}
  - User{id='uuid-456', name='Jane Smith', email='jane.smith@example.com', ...}
...
```

### 2. Real-time Notification Subscriber (`NotificationSubscriberClient`)

A dedicated client for demonstrating real-time notifications using bidirectional streaming.

#### Running the Client

```bash
mvn compile exec:java -Dexec.mainClass="org.isaac.client.NotificationSubscriberClient"
```

#### Features

- Subscribes to all user update notifications (CREATE, UPDATE, DELETE)
- Runs continuously until interrupted (Ctrl+C)
- Displays real-time notifications as they occur
- Demonstrates connection lifecycle management

#### Usage Scenario

1. Start the notification subscriber
2. In another terminal, use the main client or REST endpoints to create/update/delete users
3. Observe real-time notifications in the subscriber

#### Sample Output

```
=== Starting Real-time Notification Subscriber ===
Subscribing to user notifications... (Press Ctrl+C to stop)
Successfully subscribed to notifications. Waiting for updates...

[2024-01-15T10:30:00Z] CREATED NOTIFICATION:
  User: John Doe (ID: uuid-123, Email: john.doe@example.com)
  Message: User created successfully
  ---

[2024-01-15T10:31:00Z] UPDATED NOTIFICATION:
  User: John Smith (ID: uuid-123, Email: john.smith@example.com)
  Message: User updated successfully
  ---
```

### 3. Batch User Creation Client (`BatchUserCreationClient`)

Demonstrates efficient batch operations using client streaming.

#### Running the Client

```bash
mvn compile exec:java -Dexec.mainClass="org.isaac.client.BatchUserCreationClient"
```

#### Features

- **Predefined Users**: Creates a set of predefined users
- **Generated Users**: Creates a configurable number of generated users
- **Error Handling**: Demonstrates mixed valid/invalid data processing
- **Performance Metrics**: Shows success rates and error reporting

#### Sample Output

```
=== Starting Batch User Creation Client ===

--- Creating Predefined Users ---
Sending 5 user creation requests...
Predefined Users - Batch Creation Results:
  Successfully created: 5 users
  Created user IDs: [uuid-123, uuid-456, uuid-789, uuid-abc, uuid-def]
  Success rate: 100.0%

--- Creating 10 Generated Users ---
Sending 10 generated user creation requests...
Generated Users - Batch Creation Results:
  Successfully created: 10 users
  Created user IDs: [uuid-111, uuid-222, ...]
  Success rate: 100.0%

--- Creating Mixed Validity Users (Error Handling Demo) ---
Sending 5 mixed validity user creation requests...
Mixed Validity Users - Batch Creation Results:
  Successfully created: 3 users
  Errors encountered: 2
    - Invalid email format: not-an-email
    - Name cannot be empty
  Success rate: 60.0%
```

### 4. Interactive Streaming Demo (`InteractiveStreamingDemo`)

An interactive command-line application for exploring all gRPC patterns.

#### Running the Client

```bash
mvn compile exec:java -Dexec.mainClass="org.isaac.client.InteractiveStreamingDemo"
```

#### Features

- **Menu-driven Interface**: Easy-to-use command-line menu
- **All gRPC Patterns**: Access to unary, server streaming, client streaming, and bidirectional streaming
- **Real-time Interaction**: Create users and see immediate results
- **Educational**: Perfect for learning and experimentation

#### Menu Options

```
==================================================
Interactive gRPC Streaming Demo
==================================================
1. Unary Call (Create User)
2. Server Streaming (List Users)
3. Client Streaming (Batch Create Users)
4. Bidirectional Streaming (Subscribe to Notifications)
5. List All Users (Quick View)
6. Create Sample Data
0. Exit
==================================================
Choose an option:
```

#### Usage Examples

**Creating a User (Option 1)**

```
Choose an option: 1

--- Unary Call Demo: Create User ---
Enter user name: Alice Johnson
Enter user email: alice@example.com
Successfully created user:
  ID: uuid-123
  Name: Alice Johnson
  Email: alice@example.com
```

**Batch Creating Users (Option 3)**

```
Choose an option: 3

--- Client Streaming Demo: Batch Create Users ---
How many users would you like to create? 5
Starting client streaming with 5 users...
Client streaming completed:
  Created: 5 users
  Errors: 0
```

## REST Client Examples

For comprehensive REST client examples using curl, HTTPie, and other tools, see [REST Client Examples](rest-client-examples.md).

### Quick REST Examples

```bash
# Create user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'

# List users
curl http://localhost:8080/api/users

# Get specific user
curl http://localhost:8080/api/users/{user-id}

# Update user
curl -X PUT http://localhost:8080/api/users/{user-id} \
  -H "Content-Type: application/json" \
  -d '{"name":"John Smith","email":"john.smith@example.com"}'

# Delete user
curl -X DELETE http://localhost:8080/api/users/{user-id}
```

## API Reference

### gRPC Service Methods

#### Unary Methods

| Method       | Request             | Response             | Description           |
| ------------ | ------------------- | -------------------- | --------------------- |
| `CreateUser` | `CreateUserRequest` | `User`               | Creates a new user    |
| `GetUser`    | `GetUserRequest`    | `User`               | Retrieves user by ID  |
| `UpdateUser` | `UpdateUserRequest` | `User`               | Updates existing user |
| `DeleteUser` | `DeleteUserRequest` | `DeleteUserResponse` | Deletes user          |

#### Streaming Methods

| Method                   | Type             | Request                    | Response                  | Description             |
| ------------------------ | ---------------- | -------------------------- | ------------------------- | ----------------------- |
| `ListUsers`              | Server Streaming | `Empty`                    | `stream User`             | Lists all users         |
| `CreateUsers`            | Client Streaming | `stream CreateUserRequest` | `CreateUsersResponse`     | Batch creates users     |
| `SubscribeToUserUpdates` | Bidirectional    | `stream SubscribeRequest`  | `stream UserNotification` | Real-time notifications |

### REST Endpoints

| Method   | Endpoint          | Request Body    | Response         | Description |
| -------- | ----------------- | --------------- | ---------------- | ----------- |
| `POST`   | `/api/users`      | `CreateUserDto` | `UserDto`        | Create user |
| `GET`    | `/api/users/{id}` | -               | `UserDto`        | Get user    |
| `PUT`    | `/api/users/{id}` | `UpdateUserDto` | `UserDto`        | Update user |
| `DELETE` | `/api/users/{id}` | -               | `DeleteResponse` | Delete user |
| `GET`    | `/api/users`      | -               | `UserDto[]`      | List users  |

### Data Models

#### User

```protobuf
message User {
  string id = 1;
  string name = 2;
  string email = 3;
  int64 created_at = 4;
  int64 updated_at = 5;
}
```

#### CreateUserRequest

```protobuf
message CreateUserRequest {
  string name = 1;
  string email = 2;
}
```

#### UserNotification

```protobuf
message UserNotification {
  enum NotificationType {
    CREATED = 0;
    UPDATED = 1;
    DELETED = 2;
  }

  NotificationType type = 1;
  User user = 2;
  int64 timestamp = 3;
  string message = 4;
}
```

## Common Scenarios

### Scenario 1: Real-time User Management Dashboard

**Use Case**: Building a dashboard that shows real-time user updates

**Implementation**:

1. Use `NotificationSubscriberClient` as a base for the notification service
2. Subscribe to user notifications using bidirectional streaming
3. Use REST endpoints for CRUD operations from the web interface
4. Display real-time updates as they occur

**Code Pattern**:

```java
// Subscribe to notifications
Multi<SubscribeRequest> subscription = Multi.createFrom().items(
    SubscribeRequest.newBuilder()
        .setClientId("dashboard-" + sessionId)
        .addAllNotificationTypes(Arrays.asList(
            NotificationType.CREATED,
            NotificationType.UPDATED,
            NotificationType.DELETED
        ))
        .build()
);

userService.subscribeToUserUpdates(subscription)
    .subscribe().with(
        notification -> updateDashboard(notification),
        error -> handleError(error)
    );
```

### Scenario 2: Bulk User Import

**Use Case**: Importing users from a CSV file or external system

**Implementation**:

1. Use `BatchUserCreationClient` as a base
2. Read user data from file/external source
3. Stream user creation requests using client streaming
4. Handle partial failures gracefully

**Code Pattern**:

```java
Multi<CreateUserRequest> userStream = Multi.createFrom().iterable(userDataList)
    .map(userData -> CreateUserRequest.newBuilder()
        .setName(userData.getName())
        .setEmail(userData.getEmail())
        .build());

CreateUsersResponse response = userService.createUsers(userStream)
    .await().atMost(Duration.ofMinutes(5));

// Handle results
logger.info("Created: " + response.getCreatedCount());
logger.info("Errors: " + response.getErrorsCount());
```

### Scenario 3: User Activity Monitoring

**Use Case**: Monitoring user activities and generating reports

**Implementation**:

1. Use server streaming to efficiently process large user lists
2. Combine with notification streaming for real-time monitoring
3. Use REST endpoints for report generation

**Code Pattern**:

```java
// Process all users efficiently
userService.listUsers(Empty.newBuilder().build())
    .subscribe().with(
        user -> processUserActivity(user),
        error -> logger.error("Processing error", error),
        () -> generateReport()
    );
```

### Scenario 4: Microservice Integration

**Use Case**: Integrating with other microservices

**Implementation**:

1. Use gRPC for service-to-service communication
2. Use REST for external API integration
3. Implement proper error handling and retry logic

## Troubleshooting

### Common Issues

#### 1. Connection Issues

**Problem**: `UNAVAILABLE: io exception`

**Solutions**:

```bash
# Check if service is running
curl http://localhost:8080/health

# Check gRPC port
netstat -an | grep 9000

# Verify service logs
./mvnw quarkus:dev
```

#### 2. Streaming Issues

**Problem**: Streams not completing or hanging

**Solutions**:

- Check for proper stream completion handling
- Verify timeout configurations
- Ensure proper error handling

```java
// Proper stream handling
stream.subscribe().with(
    item -> processItem(item),
    failure -> handleFailure(failure),
    () -> handleCompletion()  // Important!
);
```

#### 3. Serialization Issues

**Problem**: `INVALID_ARGUMENT: Unable to parse request`

**Solutions**:

- Verify protobuf message structure
- Check field types and required fields
- Validate JSON format for REST calls

#### 4. Performance Issues

**Problem**: Slow response times or timeouts

**Solutions**:

```java
// Increase timeouts
.await().atMost(Duration.ofSeconds(30))

// Use appropriate streaming for large datasets
// Server streaming for large responses
// Client streaming for large requests
```

### Debugging Tips

#### Enable Detailed Logging

```properties
# application.properties
quarkus.log.level=DEBUG
quarkus.log.category."org.isaac".level=DEBUG
quarkus.log.category."io.grpc".level=DEBUG
```

#### Use gRPC Reflection

```bash
# List available services
grpcurl -plaintext localhost:9000 list

# Describe service
grpcurl -plaintext localhost:9000 describe userservice.UserService

# Make test calls
grpcurl -plaintext -d '{"name":"Test","email":"test@example.com"}' \
  localhost:9000 userservice.UserService/CreateUser
```

#### Monitor Resource Usage

```bash
# Check memory usage
jcmd <pid> VM.memory

# Check thread usage
jcmd <pid> Thread.print

# Monitor GC
java -XX:+PrintGC -XX:+PrintGCDetails ...
```

### Best Practices

1. **Connection Management**

   - Reuse channels when possible
   - Properly shut down channels
   - Handle connection failures gracefully

2. **Streaming**

   - Use appropriate streaming type for your use case
   - Handle backpressure properly
   - Implement proper error handling

3. **Error Handling**

   - Map gRPC status codes appropriately
   - Provide meaningful error messages
   - Log errors for debugging

4. **Performance**
   - Use streaming for large datasets
   - Implement proper timeouts
   - Monitor resource usage

This guide provides comprehensive information for using all the client applications and understanding the gRPC Learning Service. Use it as a reference for implementing your own gRPC clients and understanding best practices.
