package org.isaac.monitoring;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Logger;

import java.time.Instant;

/**
 * JAX-RS filter for comprehensive REST endpoint logging and monitoring.
 * <p>
 * This filter provides:
 * - Request/response logging
 * - Performance metrics collection
 * - Error tracking
 * - Correlation ID management for REST endpoints
 * <p>
 * Learning objectives:
 * - Understand JAX-RS filters
 * - Learn about REST API monitoring
 * - Understand request/response lifecycle logging
 */
@Provider
public class RestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RestLoggingFilter.class.getName());
    private static final String START_TIME_PROPERTY = "startTime";
    private static final String CORRELATION_ID_PROPERTY = "correlationId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = LoggingUtils.generateCorrelationId();
        LoggingUtils.setCorrelationId(correlationId);

        Instant startTime = Instant.now();
        requestContext.setProperty(START_TIME_PROPERTY, startTime);
        requestContext.setProperty(CORRELATION_ID_PROPERTY, correlationId);

        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        LoggingUtils.logOperationStart(LOG, "rest-request",
                String.format("method=%s, path=%s, correlationId=%s", method, path, correlationId));

        // Log request details
        LoggingUtils.logRequestResponse(LOG, "request",
                String.format("%s %s", method, path),
                String.format("contentType=%s", requestContext.getMediaType()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Instant startTime = (Instant) requestContext.getProperty(START_TIME_PROPERTY);
        String correlationId = (String) requestContext.getProperty(CORRELATION_ID_PROPERTY);

        if (startTime != null && correlationId != null) {
            LoggingUtils.setCorrelationId(correlationId);

            String method = requestContext.getMethod();
            String path = requestContext.getUriInfo().getPath();
            int status = responseContext.getStatus();

            String details = String.format("method=%s, path=%s, status=%d", method, path, status);

            if (status >= 200 && status < 300) {
                LoggingUtils.logOperationEnd(LOG, "rest-request", startTime, details);
            } else if (status >= 400) {
                LoggingUtils.logOperationError(LOG, "rest-request",
                        new RuntimeException("HTTP " + status), details);
            }

            // Log response details
            LoggingUtils.logRequestResponse(LOG, "response",
                    String.format("%s %s", method, path),
                    String.format("status=%d, contentType=%s", status, responseContext.getMediaType()));

            // Log performance metrics
            long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
            LoggingUtils.logMetric(LOG, "rest.request.duration", duration, "ms");

            LoggingUtils.clearMDC();
        }
    }
}