package org.isaac.monitoring;

import io.grpc.ServerInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Configuration class for logging and monitoring components.
 * <p>
 * This class configures the logging infrastructure including:
 * - gRPC interceptors for request/response logging
 * - Performance monitoring components
 * - Structured logging setup
 * <p>
 * Learning objectives:
 * - Understand CDI configuration for interceptors
 * - Learn about dependency injection for monitoring components
 * - Understand how to configure cross-cutting concerns
 */
@ApplicationScoped
public class LoggingConfiguration {

    /**
     * Produces the gRPC logging interceptor as a CDI bean.
     * This interceptor will be automatically registered with the gRPC server.
     * 
     * @return configured gRPC logging interceptor
     */
    @Produces
    @Singleton
    public ServerInterceptor grpcLoggingInterceptor() {
        return new GrpcLoggingInterceptor();
    }
}