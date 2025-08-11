package org.isaac.monitoring;

import io.grpc.*;
import java.util.logging.Logger;

import java.time.Instant;

/**
 * gRPC server interceptor for comprehensive logging and monitoring.
 * <p>
 * This interceptor provides:
 * - Request/response logging
 * - Performance metrics collection
 * - Error tracking
 * - Correlation ID management
 * <p>
 * Learning objectives:
 * - Understand gRPC interceptors
 * - Learn about cross-cutting concerns in microservices
 * - Understand monitoring and observability patterns
 */
public class GrpcLoggingInterceptor implements ServerInterceptor {

    private static final Logger LOG = Logger.getLogger(GrpcLoggingInterceptor.class.getName());

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        String correlationId = LoggingUtils.generateCorrelationId();
        LoggingUtils.setCorrelationId(correlationId);

        Instant startTime = Instant.now();

        LoggingUtils.logOperationStart(LOG, "grpc-call",
                String.format("method=%s, correlationId=%s", methodName, correlationId));

        // Wrap the server call to intercept responses
        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendMessage(RespT message) {
                LoggingUtils.logRequestResponse(LOG, "response", methodName,
                        message.getClass().getSimpleName());
                super.sendMessage(message);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                if (status.isOk()) {
                    LoggingUtils.logOperationEnd(LOG, "grpc-call", startTime,
                            String.format("method=%s, status=OK", methodName));
                } else {
                    LoggingUtils.logOperationError(LOG, "grpc-call",
                            status.asRuntimeException(),
                            String.format("method=%s, status=%s", methodName, status.getCode()));
                }
                super.close(status, trailers);
                LoggingUtils.clearMDC();
            }
        };

        // Wrap the listener to intercept requests
        ServerCall.Listener<ReqT> listener = next.startCall(wrappedCall, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            @Override
            public void onMessage(ReqT message) {
                LoggingUtils.logRequestResponse(LOG, "request", methodName,
                        message.getClass().getSimpleName());
                super.onMessage(message);
            }

            @Override
            public void onCancel() {
                LoggingUtils.logStreamingEvent(LOG, "cancelled", "grpc",
                        String.format("method=%s", methodName));
                super.onCancel();
                LoggingUtils.clearMDC();
            }

            @Override
            public void onComplete() {
                super.onComplete();
            }
        };
    }
}