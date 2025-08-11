package org.isaac.monitoring;

import io.quarkus.test.junit.QuarkusTest;
import java.util.logging.Logger;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LoggingUtils functionality.
 * <p>
 * This test verifies that the logging utilities work correctly for:
 * - Correlation ID management
 * - MDC context handling
 * - Performance timing
 * - Structured logging patterns
 * <p>
 * Learning objectives:
 * - Understand how to test logging behavior
 * - Learn about MDC testing patterns
 * - Understand logging verification techniques
 */
@QuarkusTest
class LoggingUtilsTest {

    private static final Logger LOG = Logger.getLogger(LoggingUtilsTest.class.getName());

    @BeforeEach
    void setUp() {
        LoggingUtils.clearMDC();
    }

    @AfterEach
    void tearDown() {
        LoggingUtils.clearMDC();
    }

    @Test
    void shouldGenerateCorrelationId() {
        String correlationId = LoggingUtils.generateCorrelationId();

        assertNotNull(correlationId);
        assertEquals(8, correlationId.length());

        // Should generate different IDs
        String anotherCorrelationId = LoggingUtils.generateCorrelationId();
        assertNotEquals(correlationId, anotherCorrelationId);
    }

    @Test
    void shouldSetAndGetCorrelationId() {
        String testCorrelationId = "test-123";

        LoggingUtils.setCorrelationId(testCorrelationId);
        String retrievedId = LoggingUtils.getCorrelationId();

        assertEquals(testCorrelationId, retrievedId);
    }

    @Test
    void shouldSetOperationInMDC() {
        String operation = "test-operation";

        LoggingUtils.setOperation(operation);

        assertEquals(operation, MDC.get("operation"));
    }

    @Test
    void shouldSetUserIdInMDC() {
        String userId = "user-123";

        LoggingUtils.setUserId(userId);

        assertEquals(userId, MDC.get("userId"));
    }

    @Test
    void shouldSetClientIdInMDC() {
        String clientId = "client-456";

        LoggingUtils.setClientId(clientId);

        assertEquals(clientId, MDC.get("clientId"));
    }

    @Test
    void shouldClearMDC() {
        LoggingUtils.setCorrelationId("test-id");
        LoggingUtils.setOperation("test-op");
        LoggingUtils.setUserId("user-123");

        LoggingUtils.clearMDC();

        assertNull(LoggingUtils.getCorrelationId());
        assertNull(MDC.get("operation"));
        assertNull(MDC.get("userId"));
    }

    @Test
    void shouldLogOperationStartWithCorrelationId() {
        // This test verifies that operation start logging works
        // In a real scenario, you would capture log output to verify
        assertDoesNotThrow(() -> {
            LoggingUtils.logOperationStart(LOG, "test-operation", "test details");

            // Verify correlation ID was set
            assertNotNull(LoggingUtils.getCorrelationId());
            assertEquals("test-operation", MDC.get("operation"));
        });
    }

    @Test
    void shouldLogOperationEndWithDuration() {
        Instant startTime = Instant.now().minusMillis(100);

        assertDoesNotThrow(() -> {
            LoggingUtils.setCorrelationId("test-id");
            LoggingUtils.logOperationEnd(LOG, "test-operation", startTime, "test completion");

            // Verify operation-specific MDC values are cleared but correlation ID remains
            assertNull(MDC.get("operation"));
            assertNull(MDC.get("duration"));
            assertEquals("test-id", LoggingUtils.getCorrelationId());
        });
    }

    @Test
    void shouldLogOperationError() {
        Exception testException = new RuntimeException("Test error");

        assertDoesNotThrow(() -> {
            LoggingUtils.logOperationError(LOG, "test-operation", testException, "error context");
        });
    }

    @Test
    void shouldLogStreamingEvents() {
        assertDoesNotThrow(() -> {
            LoggingUtils.logStreamingEvent(LOG, "started", "server", "test stream started");
            LoggingUtils.logStreamingEvent(LOG, "item", "server", "processing item 1");
            LoggingUtils.logStreamingEvent(LOG, "completed", "server", "stream completed");
        });
    }

    @Test
    void shouldExecuteWithTiming() {
        AtomicReference<String> result = new AtomicReference<>();

        String returnValue = LoggingUtils.withTiming(LOG, "test-operation", "test details", () -> {
            result.set("executed");
            return "success";
        });

        assertEquals("success", returnValue);
        assertEquals("executed", result.get());
    }

    @Test
    void shouldHandleExceptionInWithTiming() {
        RuntimeException testException = new RuntimeException("Test exception");

        assertThrows(RuntimeException.class, () -> {
            LoggingUtils.withTiming(LOG, "test-operation", "test details", () -> {
                throw testException;
            });
        });
    }

    @Test
    void shouldLogMetrics() {
        assertDoesNotThrow(() -> {
            LoggingUtils.logMetric(LOG, "test.metric", 42L, "count");
            LoggingUtils.logMetric(LOG, "test.duration", 150L, "ms");
        });
    }

    @Test
    void shouldLogRequestResponse() {
        assertDoesNotThrow(() -> {
            LoggingUtils.logRequestResponse(LOG, "request", "createUser", "name=John, email=john@example.com");
            LoggingUtils.logRequestResponse(LOG, "response", "createUser", "userId=123");
        });
    }

    @Test
    void shouldMaintainCorrelationIdAcrossOperations() {
        String correlationId = LoggingUtils.generateCorrelationId();
        LoggingUtils.setCorrelationId(correlationId);

        // Simulate multiple operations
        LoggingUtils.logOperationStart(LOG, "operation1", "details1");
        assertEquals(correlationId, LoggingUtils.getCorrelationId());

        LoggingUtils.logOperationEnd(LOG, "operation1", Instant.now().minusMillis(50), "completed");
        assertEquals(correlationId, LoggingUtils.getCorrelationId());

        LoggingUtils.logOperationStart(LOG, "operation2", "details2");
        assertEquals(correlationId, LoggingUtils.getCorrelationId());
    }
}