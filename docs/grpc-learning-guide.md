# gRPC Learning Guide

This guide explains the gRPC concepts demonstrated in this learning service and provides educational context for understanding when and how to use different patterns.

## Table of Contents

1. [Introduction to gRPC](#introduction-to-grpc)
2. [gRPC vs REST Comparison](#grpc-vs-rest-comparison)
3. [Protocol Buffers Fundamentals](#protocol-buffers-fundamentals)
4. [gRPC Communication Patterns](#grpc-communication-patterns)
5. [Error Handling in gRPC](#error-handling-in-grpc)
6. [Reactive Programming with Mutiny](#reactive-programming-with-mutiny)
7. [Performance Considerations](#performance-considerations)
8. [Security Best Practices](#security-best-practices)
9. [Testing Strategies](#testing-strategies)
10. [Production Deployment](#production-deployment)

## Introduction to gRPC

### What is gRPC?

gRPC (gRPC Remote Procedure Calls) is a modern, open-source, high-performance RPC framework that can run in any environment. It was originally developed by Google and is now part of the Cloud Native Computing Foundation (CNCF).

### Key Features

- **High Performance**: Uses HTTP/2 and Protocol Buffers for efficient communication
- **Language Agnostic**: Supports multiple programming languages
- **Streaming**: Native support for client, server, and bidirectional streaming
- **Code Generation**: Automatic client and server code generation from service definitions
- **Pluggable**: Supports pluggable authentication, load balancing, retries, etc.

### When to Use gRPC

✅ **Good for:**

- Microservices communication
- Real-time applications
- High-performance APIs
- Polyglot environments
- Internal service communication

❌ **Not ideal for:**

- Browser-based applications (limited support)
- Simple CRUD APIs with external clients
- When human-readable formats are required
- Legacy system integration

## gRPC vs REST Comparison

| Aspect              | gRPC                          | REST                      |
| ------------------- | ----------------------------- | ------------------------- |
| **Protocol**        | HTTP/2 (binary)               | HTTP/1.1 or HTTP/2 (text) |
| **Data Format**     | Protocol Buffers              | JSON, XML                 |
| **Performance**     | Higher (binary, multiplexing) | Lower (text-based)        |
| **Streaming**       | Native support (4 types)      | Limited (SSE, WebSocket)  |
| **Browser Support** | Limited (needs grpc-web)      | Native                    |
| **Caching**         | Limited                       | Excellent (HTTP caching)  |
| **Code Generation** | Automatic from .proto         | Manual or from OpenAPI    |
| **Type Safety**     | Strong (compile-time)         | Weak (runtime)            |
| **Learning Curve**  | Steeper                       | Gentler                   |
| **Tooling**         | Growing ecosystem             | Mature ecosystem          |
| **Human Readable**  | No (binary)                   | Yes (JSON)                |

### Example Comparison

**REST API Call:**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```

**gRPC Call:**

```bash
grpcurl -plaintext -d '{"name":"John Doe","email":"john@example.com"}' \
  localhost:9000 userservice.UserService/CreateUser
```

## Protocol Buffers Fundamentals

### What are Protocol Buffers?

Protocol Buffers (protobuf) are Google's language-neutral, platform-neutral, extensible mechanism for serializing structured data.

### Benefits

- **Compact**: Binary format is smaller than JSON
- **Fast**: Faster serialization/deserialization
- **Strongly Typed**: Compile-time type checking
- **Backward Compatible**: Schema evolution support
- **Cross-Language**: Works across different programming languages

### Basic Syntax

```protobuf
syntax = "proto3";

package userservice;

// Message definition
message User {
  string id = 1;           // Field number 1
  string name = 2;         // Field number 2
  string email = 3;        // Field number 3
  int64 created_at = 4;    // Field number 4
}

// Service definition
service UserService {
  rpc CreateUser(CreateUserRequest) returns (User);
}
```

### Field Numbers

- **Unique identifiers** for each field
- **Never reuse** field numbers (for backward compatibility)
- Numbers 1-15 use 1 byte encoding (use for frequent fields)
- Numbers 16-2047 use 2 bytes

### Data Types

| Proto Type | Java Type    | Description           |
| ---------- | ------------ | --------------------- |
| `string`   | `String`     | UTF-8 encoded text    |
| `int32`    | `int`        | 32-bit signed integer |
| `int64`    | `long`       | 64-bit signed integer |
| `bool`     | `boolean`    | Boolean value         |
| `bytes`    | `ByteString` | Binary data           |
| `repeated` | `List<T>`    | Array/list of values  |

### Schema Evolution

```protobuf
// Version 1
message User {
  string id = 1;
  string name = 2;
}

// Version 2 - Adding optional field
message User {
  string id = 1;
  string name = 2;
  string email = 3;        // New optional field
  int64 created_at = 4;    // Another new field
}
```

## gRPC Communication Patterns

### 1. Unary RPC (Request-Response)

**Pattern**: Client sends single request → Server returns single response

**Use Cases:**

- Traditional CRUD operations
- Authentication
- Simple queries
- Most common pattern

**Example in our service:**

```protobuf
rpc CreateUser(CreateUserRequest) returns (User);
```

**Implementation:**

```java
@Override
public Uni<User> createUser(CreateUserRequest request) {
    return userBusinessService.createUser(request)
        .onItem().invoke(user -> log.info("Created user: {}", user.getId()));
}
```

**When to use:**

- Simple request-response operations
- CRUD operations
- Authentication/authorization
- Configuration retrieval

### 2. Server Streaming RPC

**Pattern**: Client sends single request → Server returns stream of responses

**Use Cases:**

- Large datasets
- Real-time data feeds
- File downloads
- Live metrics

**Example in our service:**

```protobuf
rpc ListUsers(Empty) returns (stream User);
```

**Implementation:**

```java
@Override
public Multi<User> listUsers(Empty request) {
    return userBusinessService.getAllUsers()
        .onItem().invoke(user -> log.debug("Streaming user: {}", user.getId()));
}
```

**Benefits:**

- Efficient for large responses
- Backpressure support
- Can start processing before all data is available
- Lower memory usage on client

**When to use:**

- Returning large datasets
- Real-time data feeds
- Exporting data
- Live dashboards

### 3. Client Streaming RPC

**Pattern**: Client sends stream of requests → Server returns single response

**Use Cases:**

- Bulk operations
- File uploads
- Batch processing
- Data aggregation

**Example in our service:**

```protobuf
rpc CreateUsers(stream CreateUserRequest) returns (CreateUsersResponse);
```

**Implementation:**

```java
@Override
public Uni<CreateUsersResponse> createUsers(Multi<CreateUserRequest> requests) {
    return requests
        .onItem().transformToUniAndConcatenate(this::createSingleUser)
        .collect().asList()
        .map(this::buildSummaryResponse);
}
```

**Benefits:**

- Efficient for large requests
- Progress tracking
- Partial failure handling
- Memory efficient

**When to use:**

- Bulk data import
- File uploads
- Batch processing
- Log aggregation

### 4. Bidirectional Streaming RPC

**Pattern**: Client and server both send streams (full-duplex)

**Use Cases:**

- Real-time communication
- Chat systems
- Live collaboration
- Gaming

**Example in our service:**

```protobuf
rpc SubscribeToUserUpdates(stream SubscribeRequest) returns (stream UserNotification);
```

**Implementation:**

```java
@Override
public Multi<UserNotification> subscribeToUserUpdates(Multi<SubscribeRequest> requests) {
    return notificationService.handleSubscriptions(requests)
        .onCancellation().invoke(() -> log.info("Client disconnected"));
}
```

**Benefits:**

- Lowest latency
- Full-duplex communication
- Real-time interaction
- Connection reuse

**When to use:**

- Real-time notifications
- Chat applications
- Live collaboration
- Interactive gaming

## Error Handling in gRPC

### gRPC Status Codes

| Code                  | Description         | HTTP Equivalent | Use Case                 |
| --------------------- | ------------------- | --------------- | ------------------------ |
| `OK`                  | Success             | 200             | Successful operation     |
| `CANCELLED`           | Cancelled by client | 499             | Client cancelled request |
| `UNKNOWN`             | Unknown error       | 500             | Unexpected error         |
| `INVALID_ARGUMENT`    | Invalid parameters  | 400             | Bad request data         |
| `DEADLINE_EXCEEDED`   | Timeout             | 504             | Request timeout          |
| `NOT_FOUND`           | Resource not found  | 404             | Resource doesn't exist   |
| `ALREADY_EXISTS`      | Resource exists     | 409             | Duplicate resource       |
| `PERMISSION_DENIED`   | Access denied       | 403             | Insufficient permissions |
| `UNAUTHENTICATED`     | Not authenticated   | 401             | Authentication required  |
| `RESOURCE_EXHAUSTED`  | Rate limited        | 429             | Too many requests        |
| `FAILED_PRECONDITION` | Precondition failed | 412             | State conflict           |
| `ABORTED`             | Operation aborted   | 409             | Concurrency conflict     |
| `OUT_OF_RANGE`        | Out of range        | 400             | Invalid range            |
| `UNIMPLEMENTED`       | Not implemented     | 501             | Method not implemented   |
| `INTERNAL`            | Internal error      | 500             | Server error             |
| `UNAVAILABLE`         | Service unavailable | 503             | Service down             |
| `DATA_LOSS`           | Data loss           | 500             | Data corruption          |

### Error Handling Best Practices

1. **Use Appropriate Status Codes**:

```java
public StatusRuntimeException mapException(Throwable throwable) {
    return switch (throwable) {
        case UserNotFoundException e -> Status.NOT_FOUND
            .withDescription(e.getMessage())
            .asRuntimeException();
        case ValidationException e -> Status.INVALID_ARGUMENT
            .withDescription(e.getMessage())
            .asRuntimeException();
        case DuplicateEmailException e -> Status.ALREADY_EXISTS
            .withDescription(e.getMessage())
            .asRuntimeException();
        default -> Status.INTERNAL
            .withDescription("Internal server error")
            .asRuntimeException();
    };
}
```

2. **Provide Clear Error Messages**:

```java
throw Status.INVALID_ARGUMENT
    .withDescription("Email format is invalid: " + email)
    .asRuntimeException();
```

3. **Handle Streaming Errors**:

```java
public Multi<User> listUsers(Empty request) {
    return userRepository.findAll()
        .onFailure().transform(error ->
            Status.INTERNAL
                .withDescription("Failed to retrieve users")
                .withCause(error)
                .asRuntimeException()
        );
}
```

## Reactive Programming with Mutiny

### What is Mutiny?

Mutiny is a reactive programming library for Java that provides two main types:

- **Uni<T>**: Represents a single asynchronous result
- **Multi<T>**: Represents a stream of items

### Uni<T> - Single Async Result

```java
// Creating a Uni
Uni<String> uni = Uni.createFrom().item("Hello");

// Transforming
Uni<String> transformed = uni
    .onItem().transform(String::toUpperCase);

// Chaining async operations
Uni<User> result = validateUser(request)
    .chain(validRequest -> repository.save(buildUser(validRequest)))
    .onItem().invoke(user -> log.info("Created: {}", user.getId()));

// Error handling
Uni<User> withErrorHandling = result
    .onFailure().transform(error -> new ServiceException("Failed to create user", error));
```

### Multi<T> - Stream of Items

```java
// Creating a Multi
Multi<String> multi = Multi.createFrom().items("a", "b", "c");

// Transforming items
Multi<String> transformed = multi
    .onItem().transform(String::toUpperCase);

// Filtering
Multi<User> activeUsers = allUsers
    .select().where(user -> user.isActive());

// Collecting to list
Uni<List<User>> userList = allUsers
    .collect().asList();
```

### Common Patterns

1. **Chain Operations**:

```java
public Uni<User> createUser(CreateUserRequest request) {
    return validator.validateCreateRequest(request)
        .chain(() -> repository.save(buildUser(request)))
        .onItem().invoke(user -> notificationService.notifyUserCreated(user));
}
```

2. **Error Transformation**:

```java
public Uni<User> getUser(String id) {
    return repository.findById(id)
        .onItem().ifNull().failWith(() -> new UserNotFoundException(id))
        .onFailure().transform(this::mapToGrpcException);
}
```

3. **Side Effects**:

```java
public Uni<User> updateUser(UpdateUserRequest request) {
    return repository.update(request)
        .onItem().invoke(user -> log.info("Updated user: {}", user.getId()))
        .onFailure().invoke(error -> log.error("Update failed", error));
}
```

## Performance Considerations

### Message Size Optimization

1. **Keep Messages Small**:

```protobuf
// Good - focused message
message CreateUserRequest {
  string name = 1;
  string email = 2;
}

// Avoid - large nested structures
message CreateUserRequest {
  User user = 1;
  UserProfile profile = 2;
  repeated UserPermission permissions = 3;
  // ... many more fields
}
```

2. **Use Appropriate Data Types**:

```protobuf
// Use int32 for small numbers
int32 age = 1;

// Use int64 for large numbers
int64 timestamp = 2;

// Use bytes for binary data
bytes avatar = 3;
```

### Connection Management

1. **Reuse Channels**:

```java
@ApplicationScoped
public class UserGrpcClientManager {
    private final ManagedChannel channel;

    @PostConstruct
    void init() {
        this.channel = ManagedChannelBuilder
            .forAddress("localhost", 9000)
            .usePlaintext()
            .build();
    }

    @PreDestroy
    void cleanup() {
        channel.shutdown();
    }
}
```

2. **Configure Keep-Alive**:

```properties
quarkus.grpc.server.keep-alive-time=30s
quarkus.grpc.server.keep-alive-timeout=5s
quarkus.grpc.server.permit-keep-alive-without-calls=false
```

### Streaming Performance

1. **Handle Backpressure**:

```java
public Multi<User> listUsers(Empty request) {
    return userRepository.findAll()
        .onOverflow().buffer(100)
        .onOverflow().drop()
        .emitOn(Infrastructure.getDefaultExecutor());
}
```

2. **Batch Processing**:

```java
public Uni<CreateUsersResponse> createUsers(Multi<CreateUserRequest> requests) {
    return requests
        .group().intoLists().of(10)  // Process in batches of 10
        .onItem().transformToUniAndConcatenate(this::processBatch)
        .collect().asList()
        .map(this::buildSummaryResponse);
}
```

## Security Best Practices

### 1. Use TLS in Production

```properties
# Enable TLS
quarkus.grpc.server.ssl.certificate=classpath:tls/server-cert.pem
quarkus.grpc.server.ssl.key=classpath:tls/server-key.pem
quarkus.grpc.server.ssl.trust-store=classpath:tls/ca-cert.pem
```

### 2. Implement Authentication

```java
@GrpcService
public class SecureUserService implements UserService {

    @Override
    public Uni<User> createUser(CreateUserRequest request) {
        return authenticateUser()
            .chain(user -> authorizeOperation(user, "CREATE_USER"))
            .chain(() -> userBusinessService.createUser(request));
    }

    private Uni<AuthenticatedUser> authenticateUser() {
        // Extract and validate JWT token
        String token = extractToken();
        return jwtValidator.validate(token);
    }
}
```

### 3. Input Validation

```java
public Uni<Void> validateCreateRequest(CreateUserRequest request) {
    List<String> errors = new ArrayList<>();

    if (request.getName().isBlank()) {
        errors.add("Name is required");
    }

    if (!isValidEmail(request.getEmail())) {
        errors.add("Valid email is required");
    }

    if (!errors.isEmpty()) {
        return Uni.createFrom().failure(
            new ValidationException(String.join(", ", errors))
        );
    }

    return Uni.createFrom().voidItem();
}
```

### 4. Rate Limiting

```java
@ApplicationScoped
public class RateLimitingInterceptor implements ServerInterceptor {

    private final RateLimiter rateLimiter = RateLimiter.create(100.0); // 100 requests per second

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        if (!rateLimiter.tryAcquire()) {
            call.close(Status.RESOURCE_EXHAUSTED.withDescription("Rate limit exceeded"), headers);
            return new ServerCall.Listener<ReqT>() {};
        }

        return next.startCall(call, headers);
    }
}
```

## Testing Strategies

### 1. Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class UserBusinessServiceTest {

    @Mock
    UserRepository repository;

    @Mock
    UserValidator validator;

    @InjectMocks
    UserBusinessService service;

    @Test
    void shouldCreateUser() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
            .setName("John Doe")
            .setEmail("john@example.com")
            .build();

        when(validator.validateCreateRequest(request))
            .thenReturn(Uni.createFrom().voidItem());
        when(repository.save(any(User.class)))
            .thenReturn(Uni.createFrom().item(buildUser()));

        // When
        Uni<User> result = service.createUser(request);

        // Then
        User user = result.await().atMost(Duration.ofSeconds(1));
        assertThat(user.getName()).isEqualTo("John Doe");
    }
}
```

### 2. Integration Testing

```java
@QuarkusTest
class UserGrpcServiceTest {

    @GrpcClient
    UserService userService;

    @Test
    void shouldCreateAndRetrieveUser() {
        // Create user
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
            .setName("John Doe")
            .setEmail("john@example.com")
            .build();

        User createdUser = userService.createUser(createRequest)
            .await().atMost(Duration.ofSeconds(5));

        // Retrieve user
        GetUserRequest getRequest = GetUserRequest.newBuilder()
            .setId(createdUser.getId())
            .build();

        User retrievedUser = userService.getUser(getRequest)
            .await().atMost(Duration.ofSeconds(5));

        assertThat(retrievedUser.getName()).isEqualTo("John Doe");
    }
}
```

### 3. Streaming Testing

```java
@Test
void shouldStreamUsers() {
    // Create test users
    createTestUsers();

    // Stream users
    Multi<User> users = userService.listUsers(Empty.newBuilder().build());

    List<User> userList = users.collect().asList()
        .await().atMost(Duration.ofSeconds(5));

    assertThat(userList).hasSize(3);
    assertThat(userList).extracting(User::getName)
        .containsExactly("User 1", "User 2", "User 3");
}
```

## Production Deployment

### 1. Configuration Management

```properties
# Use environment variables for sensitive data
quarkus.grpc.server.ssl.certificate=${TLS_CERT_PATH}
quarkus.grpc.server.ssl.key=${TLS_KEY_PATH}

# Database configuration
quarkus.datasource.username=${DB_USERNAME}
quarkus.datasource.password=${DB_PASSWORD}
quarkus.datasource.jdbc.url=${DB_URL}
```

### 2. Health Checks

```java
@ApplicationScoped
public class UserServiceHealthCheck implements HealthCheck {

    @Inject
    UserRepository repository;

    @Override
    public HealthCheckResponse call() {
        try {
            // Test database connectivity
            repository.findAll().collect().first()
                .await().atMost(Duration.ofSeconds(5));

            return HealthCheckResponse.up("UserService");
        } catch (Exception e) {
            return HealthCheckResponse.down("UserService")
                .withData("error", e.getMessage());
        }
    }
}
```

### 3. Monitoring and Metrics

```java
@ApplicationScoped
public class UserServiceMetrics {

    private final Counter userCreationCounter = Counter.builder("users_created_total")
        .description("Total number of users created")
        .register(Metrics.globalRegistry);

    private final Timer userCreationTimer = Timer.builder("user_creation_duration")
        .description("Time taken to create a user")
        .register(Metrics.globalRegistry);

    public void recordUserCreation() {
        userCreationCounter.increment();
    }

    public Timer.Sample startUserCreationTimer() {
        return Timer.start(Metrics.globalRegistry);
    }
}
```

### 4. Logging

```properties
# Structured logging for production
quarkus.log.console.json=true
quarkus.log.level=INFO

# Add correlation IDs
quarkus.log.console.json.additional-field."service.name".value=grpc-learning-service
quarkus.log.console.json.additional-field."service.version".value=${SERVICE_VERSION:1.0.0}
```

## Summary

This learning service demonstrates:

1. **All gRPC Patterns**: Unary, server streaming, client streaming, and bidirectional streaming
2. **Reactive Programming**: Using Mutiny for non-blocking operations
3. **Error Handling**: Proper gRPC status codes and error mapping
4. **Integration**: Bridging gRPC and REST APIs
5. **Testing**: Comprehensive testing strategies
6. **Production Readiness**: Configuration, monitoring, and deployment considerations

By working through this service, you'll gain practical experience with:

- Protocol Buffer design
- gRPC service implementation
- Reactive programming patterns
- Error handling strategies
- Performance optimization
- Security best practices
- Testing methodologies
- Production deployment

Continue exploring by modifying the service, adding new features, and experimenting with different patterns to deepen your understanding of gRPC and reactive programming.
