# gRPC Client Architecture Comparison

This document explains the different approaches to implementing gRPC clients and how they work in various deployment scenarios.

## Overview

The gRPC Learning Service includes two types of client implementations:

1. **Embedded Clients** - Part of the same project as the server
2. **Standalone Client** - Completely separate project

Each approach has different use cases, benefits, and trade-offs.

## Architecture Comparison

### Embedded Clients (Current Implementation)

**Location**: `src/main/java/org/isaac/client/`

**Characteristics**:

- Part of the same Maven project as the server
- Uses Quarkus-specific reactive types (Mutiny)
- Shares generated gRPC classes with the server
- Tightly coupled to server implementation

**Dependencies**:

```java
// Uses server's generated classes directly
import org.isaac.grpc.user.*;

// Uses Quarkus reactive types
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

// Example usage
MutinyUserServiceGrpc.MutinyUserServiceStub stub =
    MutinyUserServiceGrpc.newMutinyStub(channel);

Uni<User> userUni = stub.createUser(request);
User user = userUni.await().atMost(Duration.ofSeconds(5));
```

### Standalone Client

**Location**: `standalone-client/` (separate project)

**Characteristics**:

- Completely independent Maven project
- Uses standard gRPC Java libraries
- Generates its own gRPC classes from proto files
- Loosely coupled - only depends on the API contract (proto file)

**Dependencies**:

```java
// Uses its own generated classes
import org.isaac.grpc.user.UserProto;
import org.isaac.grpc.user.UserServiceGrpc;

// Uses standard gRPC types
import io.grpc.stub.StreamObserver;

// Example usage
UserServiceGrpc.UserServiceBlockingStub stub =
    UserServiceGrpc.newBlockingStub(channel);

UserProto.User user = stub.createUser(request);
```

## Detailed Comparison

### 1. Project Structure

#### Embedded Clients

```
grpc-learning-service/
├── src/main/java/org/isaac/
│   ├── client/                    # Client code here
│   │   ├── UserGrpcClient.java
│   │   ├── BatchUserCreationClient.java
│   │   └── ...
│   ├── grpc/                      # Server implementation
│   └── service/                   # Business logic
├── src/main/proto/user.proto      # Shared proto file
└── pom.xml                        # Single project
```

#### Standalone Client

```
grpc-learning-service/             # Server project
├── src/main/java/org/isaac/grpc/  # Server code
├── src/main/proto/user.proto      # Server proto
└── pom.xml                        # Server dependencies

standalone-client/                 # Separate client project
├── src/main/java/org/isaac/client/ # Client code
├── src/main/proto/user.proto      # Copy of proto file
└── pom.xml                        # Client-specific dependencies
```

### 2. Dependency Management

#### Embedded Clients

```xml
<!-- Single pom.xml with both server and client dependencies -->
<dependencies>
    <!-- Quarkus gRPC (includes server and client) -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-grpc</artifactId>
    </dependency>

    <!-- Mutiny reactive streams -->
    <dependency>
        <groupId>io.smallrye.reactive</groupId>
        <artifactId>mutiny</artifactId>
    </dependency>

    <!-- Server dependencies also included -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-hibernate-orm-panache</artifactId>
    </dependency>
</dependencies>
```

#### Standalone Client

```xml
<!-- Separate pom.xml with only client dependencies -->
<dependencies>
    <!-- Pure gRPC client libraries -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
    </dependency>

    <!-- No server dependencies -->
</dependencies>
```

### 3. Code Generation

#### Embedded Clients

```bash
# Generated classes are shared between server and client
target/generated-sources/grpc/org/isaac/grpc/user/
├── UserProto.java                 # Message classes
├── UserServiceGrpc.java           # Standard gRPC stubs
└── MutinyUserServiceGrpc.java     # Quarkus Mutiny stubs (used by embedded clients)
```

#### Standalone Client

```bash
# Client generates its own classes
standalone-client/target/generated-sources/protobuf/java/org/isaac/grpc/user/
├── UserProto.java                 # Message classes (same structure)
└── UserServiceGrpc.java           # Standard gRPC stubs only
```

### 4. Programming Models

#### Embedded Clients (Reactive)

```java
public class UserGrpcClient {
    private final MutinyUserServiceGrpc.MutinyUserServiceStub asyncStub;

    public void demonstrateUnaryOperations() {
        // Reactive programming with Mutiny
        CreateUserRequest request = CreateUserRequest.newBuilder()
            .setName("John Doe")
            .setEmail("john@example.com")
            .build();

        // Returns Uni<User> (reactive type)
        User user = asyncStub.createUser(request)
            .await().atMost(Duration.ofSeconds(5));

        // Streaming with Multi<T>
        Multi<User> userStream = asyncStub.listUsers(Empty.newBuilder().build());
        List<User> users = userStream.collect().asList()
            .await().atMost(Duration.ofSeconds(10));
    }
}
```

#### Standalone Client (Traditional)

```java
public class StandaloneUserClient {
    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;
    private final UserServiceGrpc.UserServiceStub asyncStub;

    public void demonstrateUnaryOperations() {
        // Traditional blocking calls
        UserProto.CreateUserRequest request = UserProto.CreateUserRequest.newBuilder()
            .setName("John Doe")
            .setEmail("john@example.com")
            .build();

        // Direct blocking call
        UserProto.User user = blockingStub.createUser(request);

        // Streaming with StreamObserver
        asyncStub.listUsers(UserProto.Empty.newBuilder().build(),
            new StreamObserver<UserProto.User>() {
                @Override
                public void onNext(UserProto.User user) {
                    // Handle each user
                }

                @Override
                public void onCompleted() {
                    // Stream finished
                }

                @Override
                public void onError(Throwable throwable) {
                    // Handle error
                }
            });
    }
}
```

## Use Cases and When to Choose Each

### Embedded Clients - Best For:

1. **Development and Testing**

   - Quick prototyping
   - Integration testing
   - Development-time exploration

2. **Monolithic Applications**

   - When client and server are part of the same application
   - Internal service communication within the same JVM

3. **Quarkus Ecosystem**
   - When you want to leverage Quarkus reactive features
   - When using other Quarkus extensions

**Example Scenarios**:

```java
// Integration test
@QuarkusTest
class UserServiceIntegrationTest {
    @GrpcClient("user-service")
    UserService userGrpcClient;  // Injected by Quarkus

    @Test
    void testCreateUser() {
        // Test using embedded client
    }
}

// Internal service call within same application
@ApplicationScoped
public class OrderService {
    @GrpcClient("user-service")
    UserService userService;

    public void processOrder(String userId) {
        User user = userService.getUser(GetUserRequest.newBuilder()
            .setId(userId).build())
            .await().atMost(Duration.ofSeconds(5));
        // Process order with user info
    }
}
```

### Standalone Client - Best For:

1. **Production Client Applications**

   - Web applications connecting to gRPC services
   - Mobile backends
   - CLI tools

2. **Microservices Architecture**

   - Service-to-service communication
   - Cross-team service consumption
   - Third-party integrations

3. **Distribution and Packaging**
   - When clients need to be distributed separately
   - When different teams own client and server
   - When clients have different release cycles

**Example Scenarios**:

```java
// Web application client
@RestController
public class UserController {
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    @PostMapping("/api/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserProto.User grpcUser = userStub.createUser(
            UserProto.CreateUserRequest.newBuilder()
                .setName(request.getName())
                .setEmail(request.getEmail())
                .build());

        return ResponseEntity.ok(toResponse(grpcUser));
    }
}

// CLI tool
public class UserCLI {
    public static void main(String[] args) {
        StandaloneUserClient client = new StandaloneUserClient();

        if ("create".equals(args[0])) {
            client.createUser(args[1], args[2]);
        }

        client.shutdown();
    }
}

// Another microservice
@Service
public class NotificationService {
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    public void sendWelcomeEmail(String userId) {
        UserProto.User user = userStub.getUser(
            UserProto.GetUserRequest.newBuilder()
                .setId(userId)
                .build());

        emailService.sendWelcome(user.getEmail(), user.getName());
    }
}
```

## Migration Path

### From Embedded to Standalone

1. **Copy Proto File**

   ```bash
   cp src/main/proto/user.proto standalone-client/src/main/proto/
   ```

2. **Update Dependencies**

   ```xml
   <!-- Remove Quarkus dependencies -->
   <!-- Add standard gRPC dependencies -->
   ```

3. **Convert Reactive Code**

   ```java
   // From:
   User user = mutinyStub.createUser(request)
       .await().atMost(Duration.ofSeconds(5));

   // To:
   UserProto.User user = blockingStub.createUser(request);
   ```

4. **Handle Streaming**

   ```java
   // From:
   Multi<User> stream = mutinyStub.listUsers(empty);
   stream.subscribe().with(user -> process(user));

   // To:
   asyncStub.listUsers(empty, new StreamObserver<UserProto.User>() {
       @Override
       public void onNext(UserProto.User user) { process(user); }
       // ... other methods
   });
   ```

## Performance Considerations

### Embedded Clients

- **Pros**:

  - No network overhead for same-JVM calls
  - Shared connection pools
  - Integrated with Quarkus optimizations

- **Cons**:
  - Larger application footprint
  - Coupled deployment lifecycle

### Standalone Clients

- **Pros**:

  - Smaller, focused applications
  - Independent scaling and deployment
  - Better resource isolation

- **Cons**:
  - Network latency for all calls
  - Connection management overhead
  - Separate monitoring and debugging

## Security Considerations

### Embedded Clients

```java
// Security handled at application level
// Shared security context
@GrpcClient("user-service")
UserService userService;  // Inherits app security
```

### Standalone Clients

```java
// Explicit security configuration
ManagedChannel channel = ManagedChannelBuilder.forAddress("user-service", 9000)
    .useTransportSecurity()  // Enable TLS
    .build();

// Add authentication
UserServiceGrpc.UserServiceBlockingStub authenticatedStub =
    UserServiceGrpc.newBlockingStub(channel)
        .withCallCredentials(new JwtCallCredentials(token));
```

## Conclusion

Both approaches have their place in a complete gRPC ecosystem:

- **Use embedded clients** for development, testing, and when client and server are tightly coupled
- **Use standalone clients** for production applications, microservices, and when you need loose coupling

The key is understanding that gRPC's strength lies in its ability to support both patterns through the same protocol buffer contract, allowing you to choose the right approach for each specific use case.
