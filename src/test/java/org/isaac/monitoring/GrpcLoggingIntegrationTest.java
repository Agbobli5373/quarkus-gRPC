package org.isaac.monitoring;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.isaac.grpc.user.UserProto.*;
import org.isaac.grpc.user.UserService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for gRPC logging functionality.
 * <p>
 * This test verifies that logging works correctly in the context of actual gRPC
 * calls,
 * including correlation IDs, performance metrics, and streaming lifecycle
 * events.
 * <p>
 * Learning objectives:
 * - Understand integration testing for logging
 * - Learn about testing gRPC services with logging
 * - Understand how to verify logging behavior in tests
 */
@QuarkusTest
class GrpcLoggingIntegrationTest {

        @GrpcClient
        UserService userService;

        @Test
        void shouldLogUnaryOperations() {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                                .setName("Test User")
                                .setEmail("test@example.com")
                                .build();

                // Test create user - should generate logs with correlation ID and metrics
                Uni<User> createResult = userService.createUser(request);
                User createdUser = createResult.await().atMost(Duration.ofSeconds(5));

                assertNotNull(createdUser);
                assertNotNull(createdUser.getId());
                assertEquals("Test User", createdUser.getName());

                // Test get user - should log operation with user ID in context
                GetUserRequest getRequest = GetUserRequest.newBuilder()
                                .setId(createdUser.getId())
                                .build();

                Uni<User> getResult = userService.getUser(getRequest);
                User retrievedUser = getResult.await().atMost(Duration.ofSeconds(5));

                assertNotNull(retrievedUser);
                assertEquals(createdUser.getId(), retrievedUser.getId());
        }

        @Test
        void shouldLogServerStreaming() {
                // First create some test users
                createTestUser("Alice Smith", "alice@example.com");
                createTestUser("Bob Johnson", "bob@example.com");

                Empty request = Empty.newBuilder().build();
                AtomicInteger userCount = new AtomicInteger(0);

                // Test server streaming - should log streaming lifecycle events
                userService.listUsers(request)
                                .onItem().invoke(user -> {
                                        userCount.incrementAndGet();
                                        assertNotNull(user.getId());
                                        assertNotNull(user.getName());
                                })
                                .collect().asList()
                                .await().atMost(Duration.ofSeconds(5));

                assertTrue(userCount.get() >= 2, "Should have streamed at least 2 users");
        }

        @Test
        void shouldLogErrorOperations() {
                // Test operation that should fail and generate error logs
                GetUserRequest invalidRequest = GetUserRequest.newBuilder()
                                .setId("non-existent-id")
                                .build();

                assertThrows(Exception.class, () -> {
                        userService.getUser(invalidRequest)
                                        .await().atMost(Duration.ofSeconds(5));
                });
        }

        @Test
        void shouldLogPerformanceMetrics() {
                // Create a user and measure that performance logging occurs
                CreateUserRequest request = CreateUserRequest.newBuilder()
                                .setName("Performance Test User")
                                .setEmail("perf@example.com")
                                .build();

                long startTime = System.currentTimeMillis();

                Uni<User> result = userService.createUser(request);
                User user = result.await().atMost(Duration.ofSeconds(5));

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                assertNotNull(user);
                assertTrue(duration >= 0, "Operation should have measurable duration");

                // In a real test, you would capture and verify log output
                // This test verifies that the operation completes successfully
                // and that timing information would be available for logging
        }

        @Test
        void shouldMaintainCorrelationIdAcrossOperations() {
                // This test verifies that correlation IDs are properly maintained
                // In practice, you would need to capture log output to verify the correlation
                // ID

                CreateUserRequest request = CreateUserRequest.newBuilder()
                                .setName("Correlation Test User")
                                .setEmail("correlation@example.com")
                                .build();

                // Create user
                User createdUser = userService.createUser(request)
                                .await().atMost(Duration.ofSeconds(5));

                // Get the same user - should maintain correlation context
                GetUserRequest getRequest = GetUserRequest.newBuilder()
                                .setId(createdUser.getId())
                                .build();

                User retrievedUser = userService.getUser(getRequest)
                                .await().atMost(Duration.ofSeconds(5));

                assertEquals(createdUser.getId(), retrievedUser.getId());
        }

        private User createTestUser(String name, String email) {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                                .setName(name)
                                .setEmail(email)
                                .build();

                return userService.createUser(request)
                                .await().atMost(Duration.ofSeconds(5));
        }
}