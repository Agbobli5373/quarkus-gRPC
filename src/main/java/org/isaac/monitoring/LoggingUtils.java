package org.isaac.monitoring;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.jboss.logging.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Utility class for enhanced logging with performance monitoring and
 * correlation IDs.
 * <p>
 * This class provides structured logging capabilities for the gRPC learning
 * service,
 * including request correlation, performance metrics, and standardized log
 * formats.
 * <p>
 * Learning objectives:
 * - Understand structured logging in microservices
 * - Learn about request tracing and correlation
 * - Understand performance monitoring patterns
 */
public class LoggingUtils {

    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String OPERATION_KEY = "operation";
    private static final String DURATION_KEY = "duration";
    private static final String USER_ID_KEY = "userId";
    private static final String CLIENT_ID_KEY = "clientId";

    /**
     * Generates a new correlation ID for request tracking.
     * 
     * @return a unique correlation ID
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Sets the correlation ID in the MDC for the current thread.
     * 
     * @param correlationId the correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    /**
     * Gets the current correlation ID from the MDC.
     * 
     * @return the current correlation ID or null if not set
     */
    public static String getCorrelationId() {
        return (String) MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Sets the operation name in the MDC.
     * 
     * @param operation the operation name
     */
    public static void setOperation(String operation) {
        MDC.put(OPERATION_KEY, operation);
    }

    /**
     * Sets the user ID in the MDC for user-specific operations.
     * 
     * @param userId the user ID
     */
    public static void setUserId(String userId) {
        MDC.put(USER_ID_KEY, userId);
    }

    /**
     * Sets the client ID in the MDC for streaming operations.
     * 
     * @param clientId the client ID
     */
    public static void setClientId(String clientId) {
        MDC.put(CLIENT_ID_KEY, clientId);
    }

    /**
     * Clears all MDC values for the current thread.
     */
    public static void clearMDC() {
        MDC.clear();
    }

    /**
     * Logs the start of an operation with correlation ID.
     * 
     * @param logger    the logger to use
     * @param operation the operation name
     * @param details   additional operation details
     */
    public static void logOperationStart(Logger logger, String operation, String details) {
        String correlationId = getCorrelationId();
        if (correlationId == null) {
            correlationId = generateCorrelationId();
            setCorrelationId(correlationId);
        }
        setOperation(operation);

        logger.info(String.format("Operation started: %s - %s", operation, details));
    }

    /**
     * Logs the completion of an operation with duration.
     * 
     * @param logger    the logger to use
     * @param operation the operation name
     * @param startTime the operation start time
     * @param details   additional completion details
     */
    public static void logOperationEnd(Logger logger, String operation, Instant startTime, String details) {
        Duration duration = Duration.between(startTime, Instant.now());
        MDC.put(DURATION_KEY, duration.toMillis() + "ms");

        logger.info(String.format("Operation completed: %s - %s (duration: %dms)",
                operation, details, duration.toMillis()));

        // Clear operation-specific MDC values but keep correlation ID
        MDC.remove(OPERATION_KEY);
        MDC.remove(DURATION_KEY);
        MDC.remove(USER_ID_KEY);
        MDC.remove(CLIENT_ID_KEY);
    }

    /**
     * Logs an operation error with context.
     * 
     * @param logger    the logger to use
     * @param operation the operation name
     * @param error     the error that occurred
     * @param details   additional error context
     */
    public static void logOperationError(Logger logger, String operation, Throwable error, String details) {
        logger.log(Level.SEVERE, String.format("Operation failed: %s - %s", operation, details), error);
    }

    /**
     * Logs streaming lifecycle events.
     * 
     * @param logger     the logger to use
     * @param event      the streaming event (started, item, completed, cancelled,
     *                   error)
     * @param streamType the type of stream (server, client, bidirectional)
     * @param details    additional event details
     */
    public static void logStreamingEvent(Logger logger, String event, String streamType, String details) {
        logger.info(String.format("Streaming %s: %s stream - %s", event, streamType, details));
    }

    /**
     * Executes an operation with automatic timing and logging.
     * 
     * @param logger    the logger to use
     * @param operation the operation name
     * @param details   operation details
     * @param supplier  the operation to execute
     * @param <T>       the return type
     * @return the operation result
     */
    public static <T> T withTiming(Logger logger, String operation, String details, Supplier<T> supplier) {
        Instant startTime = Instant.now();
        logOperationStart(logger, operation, details);

        try {
            T result = supplier.get();
            logOperationEnd(logger, operation, startTime, "success");
            return result;
        } catch (Exception e) {
            logOperationError(logger, operation, e, details);
            throw e;
        }
    }

    /**
     * Logs performance metrics for monitoring.
     * 
     * @param logger     the logger to use
     * @param metricName the metric name
     * @param value      the metric value
     * @param unit       the metric unit (ms, count, bytes, etc.)
     */
    public static void logMetric(Logger logger, String metricName, long value, String unit) {
        logger.info(String.format("Metric: %s=%d%s", metricName, value, unit));
    }

    /**
     * Logs request/response details for debugging.
     * 
     * @param logger    the logger to use
     * @param direction "request" or "response"
     * @param operation the operation name
     * @param data      the request/response data summary
     */
    public static void logRequestResponse(Logger logger, String direction, String operation, String data) {
        logger.log(Level.FINE, String.format("%s %s: %s", direction.toUpperCase(), operation, data));
    }
}