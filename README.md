# gRPC Learning Service

## Overview

This project is a comprehensive gRPC learning service built with Quarkus that demonstrates all major gRPC communication patterns while integrating with REST reactive endpoints. It serves as an educational platform for understanding gRPC concepts, reactive programming with Mutiny, and service architecture best practices.

The service implements a user management system showcasing:

- **Unary RPC**: Traditional request-response patterns
- **Server Streaming**: Single request with multiple responses
- **Client Streaming**: Multiple requests with single response
- **Bidirectional Streaming**: Full-duplex communication
- **REST Integration**: Bridging gRPC services with REST APIs

## Prerequisites

- **Java 21** or later
- **Maven 3.9+**
- **Docker** (optional, for containerized deployment)
- **grpcurl** (optional, for command-line gRPC testing)

## Quick Start

### 1. Clone and Build

```bash
git clone https://github.com/Agbobli5373/quarkus-gRPC.git
cd quarkus-gRPC
./mvnw clean compile
```

### 2. Run in Development Mode

```bash
./mvnw quarkus:dev
```

This starts:

- gRPC server on `localhost:9000`
- REST API on `localhost:8080`
- Quarkus Dev UI at `http://localhost:8080/q/dev/`

### 3. Test the Service

**REST API Example:**

```bash
# Create a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'

# Get all users
curl http://localhost:8080/api/users
```

**gRPC Example (using grpcurl):**

```bash
# List available services
grpcurl -plaintext localhost:9000 list

# Create a user
grpcurl -plaintext -d '{"name":"Jane Doe","email":"jane@example.com"}' \
  localhost:9000 userservice.UserService/CreateUser
```

## Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   REST Clients  │    │  gRPC Clients   │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          ▼                      ▼
┌─────────────────────────────────────────┐
│           Quarkus Application           │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │ REST Layer  │  │   gRPC Layer    │   │
│  │             │  │                 │   │
│  └─────────────┘  └─────────────────┘   │
│           │                │            │
│           ▼                ▼            │
│  ┌─────────────────────────────────┐    │
│  │      Business Services          │    │
│  │  ┌─────────────┐ ┌─────────────┐│    │
│  │  │UserService  │ │Notification ││    │
│  │  │             │ │Service      ││    │
│  │  └─────────────┘ └─────────────┘│    │
│  └─────────────────────────────────┘    │
│           │                             │
│           ▼                             │
│  ┌─────────────────────────────────┐    │
│  │      Data Layer                 │    │
│  │  ┌─────────────┐ ┌─────────────┐│    │
│  │  │Repository   │ │In-Memory    ││    │
│  │  │             │ │Storage      ││    │
│  │  └─────────────┘ └─────────────┘│    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

### Key Components

- **UserGrpcService**: Implements all gRPC service methods
- **UserRestController**: REST endpoints that call gRPC services
- **UserBusinessService**: Core business logic and orchestration
- **NotificationService**: Manages real-time streaming notifications
- **UserRepository**: Data access layer with in-memory storage
- **UserValidator**: Business rule validation

## API Documentation

### gRPC Service Methods

#### Unary Operations

**CreateUser**

```protobuf
rpc CreateUser(CreateUserRequest) returns (User);
```

Example:

```bash
grpcurl -plaintext -d '{"name":"John Doe","email":"john@example.com"}' \
  localhost:9000 userservice.UserService/CreateUser
```

**GetUser**

```protobuf
rpc GetUser(GetUserRequest) returns (User);
```

Example:

```bash
grpcurl -plaintext -d '{"id":"user-123"}' \
  localhost:9000 userservice.UserService/GetUser
```

**UpdateUser**

```protobuf
rpc UpdateUser(UpdateUserRequest) returns (User);
```

Example:

```bash
grpcurl -plaintext -d '{"id":"user-123","name":"John Smith","email":"john.smith@example.com"}' \
  localhost:9000 userservice.UserService/UpdateUser
```

**DeleteUser**

```protobuf
rpc DeleteUser(DeleteUserRequest) returns (DeleteUserResponse);
```

Example:

```bash
grpcurl -plaintext -d '{"id":"user-123"}' \
  localhost:9000 userservice.UserService/DeleteUser
```

#### Server Streaming

**ListUsers**

```protobuf
rpc ListUsers(Empty) returns (stream User);
```

Example:

```bash
grpcurl -plaintext -d '{}' \
  localhost:9000 userservice.UserService/ListUsers
```

#### Client Streaming

**CreateUsers**

```protobuf
rpc CreateUsers(stream CreateUserRequest) returns (CreateUsersResponse);
```

Use the provided client applications in `src/main/java/org/isaac/client/` for streaming examples.

#### Bidirectional Streaming

**SubscribeToUserUpdates**

```protobuf
rpc SubscribeToUserUpdates(stream SubscribeRequest) returns (stream UserNotification);
```

Use the `NotificationSubscriberClient` for real-time notification examples.

### REST Endpoints

| Method | Endpoint          | Description    | Example                                                                                                                                                |
| ------ | ----------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| POST   | `/api/users`      | Create user    | `curl -X POST http://localhost:8080/api/users -H "Content-Type: application/json" -d '{"name":"John","email":"john@example.com"}'`                     |
| GET    | `/api/users/{id}` | Get user by ID | `curl http://localhost:8080/api/users/user-123`                                                                                                        |
| PUT    | `/api/users/{id}` | Update user    | `curl -X PUT http://localhost:8080/api/users/user-123 -H "Content-Type: application/json" -d '{"name":"John Smith","email":"john.smith@example.com"}'` |
| DELETE | `/api/users/{id}` | Delete user    | `curl -X DELETE http://localhost:8080/api/users/user-123`                                                                                              |
| GET    | `/api/users`      | List all users | `curl http://localhost:8080/api/users`                                                                                                                 |

### Error Codes

#### gRPC Status Codes

| Code             | HTTP Equivalent | Description           |
| ---------------- | --------------- | --------------------- |
| OK               | 200             | Success               |
| NOT_FOUND        | 404             | User not found        |
| INVALID_ARGUMENT | 400             | Invalid input data    |
| ALREADY_EXISTS   | 409             | Email already exists  |
| INTERNAL         | 500             | Internal server error |

#### REST HTTP Status Codes

| Code | Description           | Example Response                                                                               |
| ---- | --------------------- | ---------------------------------------------------------------------------------------------- |
| 200  | Success               | User data                                                                                      |
| 201  | Created               | Created user data                                                                              |
| 400  | Bad Request           | `{"error":"Invalid request","message":"Email is required"}`                                    |
| 404  | Not Found             | `{"error":"User not found","message":"User with ID user-123 not found"}`                       |
| 409  | Conflict              | `{"error":"Email already exists","message":"User with email john@example.com already exists"}` |
| 500  | Internal Server Error | `{"error":"Internal server error","message":"An unexpected error occurred"}`                   |

## Configuration

### Development Configuration

**application.properties**

```properties
# gRPC server configuration
quarkus.grpc.server.port=9000
quarkus.grpc.server.host=localhost
quarkus.grpc.server.reflection-service=true

# gRPC client configuration
quarkus.grpc.clients.userGrpcClient.host=localhost
quarkus.grpc.clients.userGrpcClient.port=9000
quarkus.grpc.clients.userGrpcClient.plain-text=true

# REST configuration
quarkus.http.port=8080

# Logging configuration
quarkus.log.console.json=true
quarkus.log.level=INFO
quarkus.log.category."org.isaac".level=DEBUG
```

### Test Configuration

**application-test.properties**

```properties
# Use random ports for testing
quarkus.grpc.server.port=0
quarkus.http.port=0

# Reduce logging in tests
quarkus.log.level=WARN
quarkus.log.category."org.isaac".level=INFO

# Disable JSON logging in tests
quarkus.log.console.json=false
```

### Production Configuration

**application-prod.properties**

```properties
# gRPC server configuration
quarkus.grpc.server.port=9000
quarkus.grpc.server.host=0.0.0.0
quarkus.grpc.server.reflection-service=false

# REST configuration
quarkus.http.port=8080
quarkus.http.host=0.0.0.0

# Security headers
quarkus.http.cors=true
quarkus.http.cors.origins=*
quarkus.http.cors.methods=GET,POST,PUT,DELETE
quarkus.http.cors.headers=accept,authorization,content-type,x-requested-with

# Logging configuration
quarkus.log.console.json=true
quarkus.log.level=INFO
quarkus.log.category."org.isaac".level=INFO

# Performance tuning
quarkus.grpc.server.max-inbound-message-size=4194304
quarkus.grpc.server.max-inbound-metadata-size=8192

# Health checks
quarkus.smallrye-health.root-path=/health
```

## Testing

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserGrpcServiceTest

# Run integration tests
./mvnw verify
```

### Test Categories

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **gRPC Tests**: Test gRPC service methods
4. **REST Tests**: Test REST endpoints
5. **End-to-End Tests**: Test complete workflows

### Sample Client Applications

Run the provided client examples:

```bash
# Compile the project first
./mvnw compile

# Run the main gRPC client demo
./mvnw exec:java -Dexec.mainClass="org.isaac.client.UserGrpcClient"

# Run the notification subscriber
./mvnw exec:java -Dexec.mainClass="org.isaac.client.NotificationSubscriberClient"

# Run the batch user creation client
./mvnw exec:java -Dexec.mainClass="org.isaac.client.BatchUserCreationClient"

# Run the interactive streaming demo
./mvnw exec:java -Dexec.mainClass="org.isaac.client.InteractiveStreamingDemo"
```

## Troubleshooting

### Common Setup Issues

**Issue: Port already in use**

```
Solution: Change ports in application.properties or kill the process using the port
```

**Issue: gRPC reflection not working**

```
Solution: Ensure quarkus.grpc.server.reflection-service=true in development
```

**Issue: Maven compilation fails**

```
Solution: Ensure Java 21+ is installed and JAVA_HOME is set correctly
```

### Connection Problems

**Issue: gRPC client cannot connect**

```
Check:
1. Server is running on correct port (9000)
2. Client configuration matches server
3. Firewall settings allow connections
4. Use plain-text for development (no TLS)
```

**Issue: REST endpoints return 404**

```
Check:
1. Server is running on correct port (8080)
2. Endpoint paths are correct (/api/users)
3. Content-Type header is set for POST/PUT requests
```

### Performance Issues

**Issue: Slow streaming performance**

```
Solutions:
1. Increase buffer sizes in configuration
2. Use appropriate backpressure handling
3. Monitor memory usage during streaming
4. Consider pagination for large datasets
```

**Issue: High memory usage**

```
Solutions:
1. Monitor concurrent streaming connections
2. Implement proper cleanup in bidirectional streaming
3. Use appropriate JVM heap settings
4. Profile memory usage with tools like JProfiler
```

### Debugging Tips

1. **Enable Debug Logging**:

   ```properties
   quarkus.log.category."org.isaac".level=DEBUG
   ```

2. **Use gRPC Reflection**:

   ```bash
   grpcurl -plaintext localhost:9000 list
   grpcurl -plaintext localhost:9000 describe userservice.UserService
   ```

3. **Monitor with Dev UI**:
   Visit `http://localhost:8080/q/dev/` in development mode

4. **Check Health Endpoints**:

   ```bash
   curl http://localhost:8080/q/health
   ```

5. **View Metrics**:
   ```bash
   curl http://localhost:8080/q/metrics
   ```

## Learning Resources

### gRPC vs REST Comparison

| Aspect              | gRPC                          | REST                         |
| ------------------- | ----------------------------- | ---------------------------- |
| **Protocol**        | HTTP/2                        | HTTP/1.1 or HTTP/2           |
| **Data Format**     | Protocol Buffers (binary)     | JSON (text)                  |
| **Performance**     | Higher (binary, multiplexing) | Lower (text, single request) |
| **Streaming**       | Native support                | Limited (SSE, WebSocket)     |
| **Browser Support** | Limited (needs proxy)         | Native                       |
| **Code Generation** | Automatic from .proto         | Manual or OpenAPI            |
| **Type Safety**     | Strong (compiled)             | Weak (runtime)               |

### When to Use Each Streaming Pattern

#### Unary RPC

- **Use for**: Traditional CRUD operations, simple request-response
- **Example**: Get user by ID, create user, update user
- **Benefits**: Simple, familiar pattern, easy to debug

#### Server Streaming

- **Use for**: Large datasets, real-time data feeds, file downloads
- **Example**: List all users, export data, live metrics
- **Benefits**: Efficient for large responses, backpressure support

#### Client Streaming

- **Use for**: Bulk operations, file uploads, batch processing
- **Example**: Bulk user creation, data import, log aggregation
- **Benefits**: Efficient for large requests, progress tracking

#### Bidirectional Streaming

- **Use for**: Real-time communication, chat systems, live updates
- **Example**: User notifications, collaborative editing, gaming
- **Benefits**: Full-duplex communication, lowest latency

### Error Handling Best Practices

1. **Use Appropriate Status Codes**: Map business errors to correct gRPC status codes
2. **Provide Clear Messages**: Include helpful error descriptions
3. **Handle Partial Failures**: In streaming, continue processing valid requests
4. **Log Errors Properly**: Include context and stack traces for debugging
5. **Fail Fast**: Validate inputs early to avoid unnecessary processing

### Performance Considerations

1. **Message Size**: Keep messages reasonably small for better performance
2. **Connection Pooling**: Reuse gRPC channels when possible
3. **Streaming Backpressure**: Handle slow consumers appropriately
4. **Serialization**: Protocol Buffers are more efficient than JSON
5. **Compression**: Enable gRPC compression for large messages

### Security Considerations

1. **TLS**: Always use TLS in production
2. **Authentication**: Implement proper authentication mechanisms
3. **Authorization**: Validate permissions for each operation
4. **Input Validation**: Validate all inputs on the server side
5. **Rate Limiting**: Implement rate limiting to prevent abuse

## Deployment

### Docker Deployment

1. **Build Docker Image**:

   ```bash
   ./mvnw package -Dquarkus.container-image.build=true
   ```

2. **Run Container**:
   ```bash
   docker run -p 8080:8080 -p 9000:9000 quarkus/grpc-learning-service
   ```

### Native Executable

1. **Build Native Image**:

   ```bash
   ./mvnw package -Dnative
   ```

2. **Run Native Executable**:
   ```bash
   ./target/sample-grpc-1.0.0-SNAPSHOT-runner
   ```

### Kubernetes Deployment

See `k8s/` directory for Kubernetes manifests (if available).

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Documentation

This project includes comprehensive documentation:

- **[API Reference](docs/api-reference.md)**: Complete API documentation for all gRPC and REST endpoints
- **[gRPC Learning Guide](docs/grpc-learning-guide.md)**: Educational guide explaining gRPC concepts and patterns
- **[Troubleshooting Guide](docs/troubleshooting-guide.md)**: Solutions for common issues and debugging tips
- **[Deployment Guide](docs/deployment-guide.md)**: Instructions for deploying in various environments
- **[Client Architecture Comparison](docs/client-architecture-comparison.md)**: Comparison of different client patterns
- **[Client Usage Guide](docs/client-usage-guide.md)**: How to use the provided client applications
- **[REST Client Examples](docs/rest-client-examples.md)**: REST API usage examples

## Additional Resources

- [Quarkus gRPC Guide](https://quarkus.io/guides/grpc-getting-started)
- [gRPC Documentation](https://grpc.io/docs/)
- [Protocol Buffers Guide](https://developers.google.com/protocol-buffers)
- [Mutiny Documentation](https://smallrye.io/smallrye-mutiny/)
- [Reactive Programming Guide](https://quarkus.io/guides/getting-started-reactive)
