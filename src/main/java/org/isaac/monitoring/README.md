# Comprehensive Logging and Monitoring Implementation

This package implements comprehensive logging and monitoring for the gRPC learning service, providing structured logging, performance metrics, and request correlation capabilities.

## Components

### LoggingUtils

Central utility class providing:

- **Correlation ID Management**: Automatic generation and tracking of request correlation IDs
- **MDC Context Management**: Thread-local context for user IDs, client IDs, and operation details
- **Performance Timing**: Automatic operation timing with duration metrics
- **Structured Logging**: Consistent log formatting across all service layers
- **Streaming Lifecycle Logging**: Specialized logging for gRPC streaming operations

### GrpcLoggingInterceptor

gRPC server interceptor that provides:

- Request/response logging for all gRPC operations
- Performance metrics collection
- Error tracking and correlation
- Automatic correlation ID generation for each request

### RestLoggingFilter

JAX-RS filter for REST endpoint monitoring:

- Request/response lifecycle logging
- HTTP status code tracking
- Performance metrics for REST operations
- Correlation ID propagation

### LoggingConfiguration

CDI configuration class that registers the gRPC interceptor automatically.

## Features Implemented

### 1. Structured Logging Configuration

```properties
# Structured logging
quarkus.log.console.json=true
quarkus.log.level=INFO
quarkus.log.category."org.isaac.grpc".level=DEBUG
quarkus.log.category."org.isaac.rest".level=DEBUG
quarkus.log.category."org.isaac.service".level=DEBUG
quarkus.log.category."org.isaac.repository".level=DEBUG

# Performance monitoring
quarkus.log.category."org.isaac.monitoring".level=INFO

# Request correlation
quarkus.log.console.json.additional-field."service.name".value=grpc-learning-service
quarkus.log.console.json.additional-field."service.version".value=1.0.0
```

### 2. Request/Response Logging

- All gRPC operations log start/end with correlation IDs
- REST endpoints log HTTP method, path, and status codes
- Request and response data summaries for debugging

### 3. Performance Metrics

- Operation duration tracking (in milliseconds)
- Success/error counters for operations
- Streaming metrics (item counts, stream duration)
- Throughput monitoring

### 4. Streaming Lifecycle Logging

- Server streaming: start, item processing, completion
- Client streaming: batch processing, summary statistics
- Bidirectional streaming: subscription management, client lifecycle

### 5. Error Logging with Context

- Structured error logging with stack traces
- Error correlation with operation context
- gRPC status code mapping
- HTTP error response tracking

## Usage Examples

### Basic Operation Logging

```java
Instant startTime = Instant.now();
LoggingUtils.logOperationStart(logger, "grpc.createUser",
    String.format("name=%s, email=%s", request.getName(), request.getEmail()));

return userBusinessService.createUser(request)
    .onItem().invoke(user -> {
        LoggingUtils.setUserId(user.getId());
        LoggingUtils.logOperationEnd(logger, "grpc.createUser", startTime,
            String.format("userId=%s", user.getId()));
        LoggingUtils.logMetric(logger, "grpc.createUser.success", 1, "count");
    })
    .onFailure().invoke(throwable -> {
        LoggingUtils.logOperationError(logger, "grpc.createUser", throwable,
            String.format("name=%s", request.getName()));
        LoggingUtils.logMetric(logger, "grpc.createUser.error", 1, "count");
    });
```

### Streaming Lifecycle Logging

```java
LoggingUtils.logStreamingEvent(logger, "started", "server", "listUsers operation");

return userBusinessService.getAllUsers()
    .onItem().invoke(user -> {
        LoggingUtils.logStreamingEvent(logger, "item", "server",
            String.format("streaming user %s (%s)", user.getId(), user.getName()));
    })
    .onCompletion().invoke(() -> {
        LoggingUtils.logStreamingEvent(logger, "completed", "server",
            String.format("streamed %d users", userCount));
        LoggingUtils.logMetric(logger, "grpc.listUsers.streamed_count", userCount, "count");
    });
```

### Correlation ID Management

```java
// Automatic correlation ID generation
String correlationId = LoggingUtils.generateCorrelationId();
LoggingUtils.setCorrelationId(correlationId);

// Context propagation
LoggingUtils.setUserId(user.getId());
LoggingUtils.setClientId(clientId);
```

## Log Levels

- **DEBUG**: Detailed operation information, request/response data
- **INFO**: Important business events, operation start/end, metrics
- **WARN**: Recoverable errors, validation failures
- **ERROR**: Serious errors requiring attention, system failures

## Testing

The implementation includes comprehensive tests:

### LoggingUtilsTest

- Unit tests for all utility methods
- MDC context management verification
- Correlation ID generation and propagation
- Performance timing accuracy

### GrpcLoggingIntegrationTest

- Integration tests with actual gRPC service calls
- Verification of logging behavior in real scenarios
- Performance metrics validation
- Error handling verification

## Benefits

1. **Observability**: Complete visibility into system behavior
2. **Debugging**: Correlation IDs enable request tracing across services
3. **Performance Monitoring**: Automatic collection of timing and throughput metrics
4. **Educational Value**: Clear demonstration of logging best practices
5. **Production Ready**: Structured logging suitable for log aggregation systems

## Learning Objectives Achieved

- ✅ Understand structured logging in microservices
- ✅ Learn about request tracing and correlation
- ✅ Understand performance monitoring for gRPC and REST
- ✅ Implement comprehensive error logging with context
- ✅ Add streaming lifecycle logging for educational purposes
- ✅ Write tests to verify logging behavior
