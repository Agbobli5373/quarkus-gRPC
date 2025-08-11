# Troubleshooting Guide

This guide helps you diagnose and resolve common issues when working with the gRPC Learning Service.

## Table of Contents

1. [Setup and Installation Issues](#setup-and-installation-issues)
2. [Connection Problems](#connection-problems)
3. [Performance Issues](#performance-issues)
4. [Streaming Issues](#streaming-issues)
5. [Error Handling Issues](#error-handling-issues)
6. [Development Issues](#development-issues)
7. [Testing Issues](#testing-issues)
8. [Deployment Issues](#deployment-issues)
9. [Debugging Tools and Techniques](#debugging-tools-and-techniques)

## Setup and Installation Issues

### Issue: Maven compilation fails with "package does not exist"

**Symptoms:**

```
[ERROR] package org.isaac.grpc.user does not exist
[ERROR] cannot find symbol: class User
```

**Cause:** gRPC code generation hasn't run or failed.

**Solutions:**

1. Clean and regenerate code:

   ```bash
   ./mvnw clean compile
   ```

2. Verify proto file exists:

   ```bash
   ls -la src/main/proto/user.proto
   ```

3. Check generated classes:

   ```bash
   ls -la target/generated-sources/grpc/
   ```

4. Force code generation:
   ```bash
   ./mvnw quarkus:generate-code
   ```

### Issue: Java version compatibility errors

**Symptoms:**

```
[ERROR] Source option 21 is no longer supported. Use 22 or later.
```

**Cause:** Incorrect Java version.

**Solutions:**

1. Check Java version:

   ```bash
   java -version
   javac -version
   ```

2. Set JAVA_HOME correctly:

   ```bash
   export JAVA_HOME=/path/to/java21
   ```

3. Update Maven wrapper:
   ```bash
   ./mvnw wrapper:wrapper -Dmaven=3.9.6
   ```

### Issue: Port already in use

**Symptoms:**

```
Port 8080 is already in use
Port 9000 is already in use
```

**Solutions:**

1. Find and kill the process:

   ```bash
   # On Windows
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F

   # On Linux/Mac
   lsof -ti:8080 | xargs kill -9
   ```

2. Change ports in configuration:

   ```properties
   quarkus.http.port=8081
   quarkus.grpc.server.port=9001
   ```

3. Use random ports for development:
   ```properties
   quarkus.http.port=0
   quarkus.grpc.server.port=0
   ```

## Connection Problems

### Issue: gRPC client cannot connect to server

**Symptoms:**

```
io.grpc.StatusRuntimeException: UNAVAILABLE: io exception
```

**Diagnosis Steps:**

1. Verify server is running:

   ```bash
   netstat -an | grep 9000
   ```

2. Test with grpcurl:

   ```bash
   grpcurl -plaintext localhost:9000 list
   ```

3. Check server logs for errors

**Solutions:**

1. Ensure server is started:

   ```bash
   ./mvnw quarkus:dev
   ```

2. Verify client configuration:

   ```properties
   quarkus.grpc.clients.userGrpcClient.host=localhost
   quarkus.grpc.clients.userGrpcClient.port=9000
   quarkus.grpc.clients.userGrpcClient.plain-text=true
   ```

3. Check firewall settings
4. Verify network connectivity

### Issue: REST endpoints return 404

**Symptoms:**

```
HTTP 404 Not Found for /api/users
```

**Solutions:**

1. Verify endpoint paths:

   ```bash
   curl -v http://localhost:8080/api/users
   ```

2. Check controller annotations:

   ```java
   @Path("/api/users")
   @Produces(MediaType.APPLICATION_JSON)
   ```

3. Verify server is running on correct port:
   ```bash
   curl http://localhost:8080/q/health
   ```

### Issue: CORS errors in browser

**Symptoms:**

```
Access to fetch at 'http://localhost:8080/api/users' from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solutions:**

1. Enable CORS in development:

   ```properties
   quarkus.http.cors=true
   quarkus.http.cors.origins=*
   ```

2. Configure specific origins for production:
   ```properties
   quarkus.http.cors.origins=https://yourdomain.com
   ```

## Performance Issues

### Issue: Slow gRPC calls

**Symptoms:**

- High latency for simple operations
- Timeouts on streaming operations

**Diagnosis:**

1. Enable performance logging:

   ```properties
   quarkus.log.category."org.isaac.monitoring".level=DEBUG
   ```

2. Monitor with JVM tools:
   ```bash
   jcmd <pid> VM.classloader_stats
   ```

**Solutions:**

1. Increase message size limits:

   ```properties
   quarkus.grpc.server.max-inbound-message-size=8388608
   ```

2. Tune thread pools:

   ```properties
   quarkus.thread-pool.max-threads=200
   ```

3. Enable HTTP/2:
   ```properties
   quarkus.http.http2=true
   ```

### Issue: High memory usage

**Symptoms:**

- OutOfMemoryError
- Gradual memory increase
- GC pressure

**Solutions:**

1. Monitor streaming connections:

   ```java
   // Add connection tracking in NotificationService
   private final AtomicInteger activeConnections = new AtomicInteger(0);
   ```

2. Implement proper cleanup:

   ```java
   @Override
   public Multi<UserNotification> subscribeToUserUpdates(Multi<SubscribeRequest> requests) {
       return requests
           .onCancellation().invoke(() -> cleanup())
           .onFailure().invoke(error -> cleanup());
   }
   ```

3. Set JVM heap limits:
   ```bash
   java -Xmx512m -Xms256m -jar target/quarkus-app/quarkus-run.jar
   ```

## Streaming Issues

### Issue: Streaming connections drop unexpectedly

**Symptoms:**

- Clients lose connection during streaming
- CANCELLED status in logs

**Solutions:**

1. Implement keep-alive:

   ```properties
   quarkus.grpc.server.keep-alive-time=30s
   quarkus.grpc.server.keep-alive-timeout=5s
   ```

2. Handle client disconnections:

   ```java
   public Multi<UserNotification> subscribeToUserUpdates(Multi<SubscribeRequest> requests) {
       return requests
           .onCancellation().invoke(() -> log.info("Client disconnected"))
           .onFailure().invoke(error -> log.error("Stream error", error));
   }
   ```

3. Add retry logic in clients:
   ```java
   public void subscribeWithRetry() {
       Multi.createBy().repeating()
           .supplier(this::createSubscription)
           .whilst(this::shouldRetry)
           .subscribe().with(this::handleNotification);
   }
   ```

### Issue: Backpressure problems

**Symptoms:**

- Slow consumers cause memory buildup
- Stream processing delays

**Solutions:**

1. Implement proper backpressure:

   ```java
   public Multi<User> listUsers(Empty request) {
       return userRepository.findAll()
           .onOverflow().buffer(100)
           .onOverflow().drop();
   }
   ```

2. Use appropriate buffer sizes:
   ```java
   return Multi.createFrom().items(users.stream())
       .emitOn(Infrastructure.getDefaultExecutor())
       .onOverflow().buffer(50);
   ```

## Error Handling Issues

### Issue: gRPC errors not properly mapped to HTTP

**Symptoms:**

- All REST errors return 500
- Generic error messages

**Solutions:**

1. Verify exception mapper is registered:

   ```java
   @Provider
   public class RestExceptionMapper implements ExceptionMapper<StatusRuntimeException> {
       // Implementation
   }
   ```

2. Check error mapping logic:
   ```java
   public Response toResponse(StatusRuntimeException exception) {
       Status.Code code = exception.getStatus().getCode();
       return switch (code) {
           case NOT_FOUND -> Response.status(404).build();
           case INVALID_ARGUMENT -> Response.status(400).build();
           // ... other mappings
       };
   }
   ```

### Issue: Validation errors not clear

**Symptoms:**

- Generic "validation failed" messages
- No field-specific errors

**Solutions:**

1. Enhance validation messages:

   ```java
   @NotBlank(message = "Name is required and cannot be blank")
   @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
   private String name;
   ```

2. Collect all validation errors:
   ```java
   public Uni<Void> validateCreateRequest(CreateUserRequest request) {
       List<String> errors = new ArrayList<>();

       if (request.getName().isBlank()) {
           errors.add("Name is required");
       }

       if (!errors.isEmpty()) {
           return Uni.createFrom().failure(new ValidationException(String.join(", ", errors)));
       }

       return Uni.createFrom().voidItem();
   }
   ```

## Development Issues

### Issue: Hot reload not working

**Symptoms:**

- Changes not reflected without restart
- Dev mode not detecting changes

**Solutions:**

1. Verify dev mode is running:

   ```bash
   ./mvnw quarkus:dev
   ```

2. Check file permissions and paths
3. Clear target directory:

   ```bash
   ./mvnw clean
   ```

4. Restart dev mode if proto files changed

### Issue: IDE not recognizing generated classes

**Symptoms:**

- Import errors for generated gRPC classes
- IDE shows compilation errors

**Solutions:**

1. Refresh IDE project
2. Mark generated sources directory:

   - IntelliJ: Mark `target/generated-sources/grpc` as Generated Sources Root
   - Eclipse: Add to build path

3. Reimport Maven project
4. Run code generation manually:
   ```bash
   ./mvnw quarkus:generate-code
   ```

## Testing Issues

### Issue: Tests fail with port conflicts

**Symptoms:**

```
java.net.BindException: Address already in use
```

**Solutions:**

1. Use random ports in test configuration:

   ```properties
   # application-test.properties
   quarkus.grpc.server.port=0
   quarkus.http.port=0
   ```

2. Run tests sequentially:
   ```bash
   ./mvnw test -Dmaven.test.parallel=false
   ```

### Issue: gRPC client injection fails in tests

**Symptoms:**

```
No bean found for type: UserService
```

**Solutions:**

1. Use @QuarkusTest annotation:

   ```java
   @QuarkusTest
   class UserGrpcServiceTest {
       @GrpcClient
       UserService userService;
   }
   ```

2. Configure test client properly:
   ```properties
   quarkus.grpc.clients.userGrpcClient.host=localhost
   quarkus.grpc.clients.userGrpcClient.port=0
   ```

## Deployment Issues

### Issue: Docker container fails to start

**Symptoms:**

```
Error: Could not find or load main class io.quarkus.runner.GeneratedMain
```

**Solutions:**

1. Build with correct profile:

   ```bash
   ./mvnw package -Dquarkus.container-image.build=true
   ```

2. Check Dockerfile configuration
3. Verify all dependencies are included

### Issue: Native compilation fails

**Symptoms:**

```
Error: Classes that should be initialized at run time got initialized during image building
```

**Solutions:**

1. Add reflection configuration:

   ```json
   // src/main/resources/META-INF/native-image/reflect-config.json
   [
     {
       "name": "org.isaac.grpc.user.User",
       "allDeclaredConstructors": true,
       "allPublicConstructors": true,
       "allDeclaredMethods": true,
       "allPublicMethods": true
     }
   ]
   ```

2. Use JVM mode for development:
   ```bash
   ./mvnw package -Dquarkus.package.jar.type=uber-jar
   ```

## Debugging Tools and Techniques

### Enable Debug Logging

```properties
# Enable debug logging for specific packages
quarkus.log.category."org.isaac".level=DEBUG
quarkus.log.category."io.grpc".level=DEBUG

# Enable gRPC wire logging (very verbose)
quarkus.log.category."io.grpc.netty".level=DEBUG
```

### Use gRPC Reflection

```bash
# List available services
grpcurl -plaintext localhost:9000 list

# Describe a service
grpcurl -plaintext localhost:9000 describe userservice.UserService

# Describe a message type
grpcurl -plaintext localhost:9000 describe userservice.User
```

### Monitor with Health Checks

```bash
# Check application health
curl http://localhost:8080/q/health

# Check liveness
curl http://localhost:8080/q/health/live

# Check readiness
curl http://localhost:8080/q/health/ready
```

### View Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/q/metrics

# Application metrics
curl http://localhost:8080/q/metrics/application
```

### Use Dev UI

In development mode, visit `http://localhost:8080/q/dev/` for:

- gRPC service information
- Configuration values
- Health checks
- Metrics
- Log levels

### JVM Debugging

```bash
# Enable JVM debugging
./mvnw quarkus:dev -Ddebug=5005

# Connect with IDE debugger on port 5005
```

### Network Debugging

```bash
# Monitor network connections
netstat -an | grep -E "(8080|9000)"

# Test connectivity
telnet localhost 9000

# Monitor traffic (Linux/Mac)
tcpdump -i lo -A -s 0 port 9000
```

### Memory Analysis

```bash
# Generate heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.classloader_stats

# Monitor GC
java -XX:+PrintGC -XX:+PrintGCDetails -jar target/quarkus-app/quarkus-run.jar
```

## Getting Help

If you're still experiencing issues:

1. **Check the logs** - Enable debug logging and look for error messages
2. **Search existing issues** - Check the project's issue tracker
3. **Create a minimal reproduction** - Isolate the problem
4. **Provide context** - Include configuration, logs, and environment details
5. **Ask for help** - Create an issue with detailed information

## Common Log Messages and Their Meanings

| Log Message                                          | Meaning                  | Action                                   |
| ---------------------------------------------------- | ------------------------ | ---------------------------------------- |
| `UNAVAILABLE: io exception`                          | gRPC connection failed   | Check server status and network          |
| `INVALID_ARGUMENT: ...`                              | Bad request data         | Validate input parameters                |
| `NOT_FOUND: User with ID ... not found`              | Resource doesn't exist   | Check if resource was created            |
| `ALREADY_EXISTS: User with email ... already exists` | Duplicate resource       | Use different email or update existing   |
| `CANCELLED: call was cancelled`                      | Client cancelled request | Normal for streaming disconnections      |
| `DEADLINE_EXCEEDED: deadline exceeded`               | Request timeout          | Increase timeout or optimize performance |
