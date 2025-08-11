# Deployment Guide

This guide covers various deployment options for the gRPC Learning Service, from local development to production environments.

## Table of Contents

1. [Local Development](#local-development)
2. [Docker Deployment](#docker-deployment)
3. [Kubernetes Deployment](#kubernetes-deployment)
4. [Native Executable](#native-executable)
5. [Cloud Deployment](#cloud-deployment)
6. [Configuration Management](#configuration-management)
7. [Monitoring and Observability](#monitoring-and-observability)

## Local Development

### Prerequisites

- Java 21 or later
- Maven 3.9+
- Docker (optional)

### Development Mode

```bash
# Clone the repository
git clone <repository-url>
cd grpc-learning-service

# Run in development mode with hot reload
./mvnw quarkus:dev
```

This starts:

- gRPC server on `localhost:9000`
- REST API on `localhost:8080`
- Dev UI at `http://localhost:8080/q/dev/`

### Development Configuration

The service uses `application.properties` for development with:

- gRPC reflection enabled
- Debug logging enabled
- Plain-text communication (no TLS)

## Docker Deployment

### Building the Docker Image

#### JVM Mode (Recommended for most cases)

```bash
# Build the application
./mvnw package

# Build Docker image
docker build -f Dockerfile -t grpc-learning-service:latest .

# Run the container
docker run -d \
  --name grpc-learning-service \
  -p 8080:8080 \
  -p 9000:9000 \
  grpc-learning-service:latest
```

#### Native Mode (Smaller image, faster startup)

```bash
# Build native executable (requires GraalVM or container build)
./mvnw package -Dnative -Dquarkus.native.container-build=true

# Build Docker image
docker build -f Dockerfile.native -t grpc-learning-service:native .

# Run the container
docker run -d \
  --name grpc-learning-service \
  -p 8080:8080 \
  -p 9000:9000 \
  grpc-learning-service:native
```

### Docker Compose

For easy multi-service deployment:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f grpc-learning-service

# Stop all services
docker-compose down
```

### Docker Configuration

#### Environment Variables

```bash
docker run -d \
  --name grpc-learning-service \
  -p 8080:8080 \
  -p 9000:9000 \
  -e QUARKUS_PROFILE=prod \
  -e JAVA_OPTS_APPEND="-Xmx512m -Xms256m" \
  grpc-learning-service:latest
```

#### Health Checks

The Docker image includes health checks:

```bash
# Check container health
docker ps

# View health check logs
docker inspect grpc-learning-service | grep -A 10 Health
```

## Kubernetes Deployment

### Basic Deployment

Create `k8s/deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: grpc-learning-service
  labels:
    app: grpc-learning-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: grpc-learning-service
  template:
    metadata:
      labels:
        app: grpc-learning-service
    spec:
      containers:
        - name: grpc-learning-service
          image: grpc-learning-service:latest
          ports:
            - containerPort: 8080
              name: http
            - containerPort: 9000
              name: grpc
          env:
            - name: QUARKUS_PROFILE
              value: "prod"
            - name: JAVA_OPTS_APPEND
              value: "-Xmx512m -Xms256m"
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 8080
            initialDelaySeconds: 5
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: grpc-learning-service
  labels:
    app: grpc-learning-service
spec:
  selector:
    app: grpc-learning-service
  ports:
    - name: http
      port: 8080
      targetPort: 8080
    - name: grpc
      port: 9000
      targetPort: 9000
  type: ClusterIP
```

### Ingress Configuration

Create `k8s/ingress.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: grpc-learning-service-ingress
  annotations:
    nginx.ingress.kubernetes.io/backend-protocol: "GRPC"
    nginx.ingress.kubernetes.io/grpc-backend: "true"
spec:
  rules:
    - host: grpc-learning.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: grpc-learning-service
                port:
                  number: 8080
    - host: grpc-api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: grpc-learning-service
                port:
                  number: 9000
```

### ConfigMap for Configuration

Create `k8s/configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grpc-learning-config
data:
  application.properties: |
    # Production configuration
    quarkus.grpc.server.port=9000
    quarkus.grpc.server.host=0.0.0.0
    quarkus.grpc.server.reflection-service=false

    quarkus.http.port=8080
    quarkus.http.host=0.0.0.0

    quarkus.log.console.json=true
    quarkus.log.level=INFO

    # Performance tuning
    quarkus.grpc.server.max-inbound-message-size=4194304
    quarkus.thread-pool.max-threads=200
```

### Deploy to Kubernetes

```bash
# Apply all configurations
kubectl apply -f k8s/

# Check deployment status
kubectl get deployments
kubectl get pods
kubectl get services

# View logs
kubectl logs -f deployment/grpc-learning-service

# Port forward for testing
kubectl port-forward service/grpc-learning-service 8080:8080 9000:9000
```

## Native Executable

### Building Native Executable

#### With GraalVM Installed

```bash
# Install GraalVM 21
# Set GRAALVM_HOME and update PATH

# Build native executable
./mvnw package -Dnative
```

#### With Container Build (No GraalVM required)

```bash
# Build using container
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

### Running Native Executable

```bash
# Run the native executable
./target/sample-grpc-1.0.0-SNAPSHOT-runner

# With custom configuration
./target/sample-grpc-1.0.0-SNAPSHOT-runner \
  -Dquarkus.http.port=8081 \
  -Dquarkus.grpc.server.port=9001
```

### Native Executable Benefits

- **Fast Startup**: ~50ms vs ~2s for JVM
- **Low Memory**: ~50MB vs ~200MB for JVM
- **No Warmup**: Peak performance immediately
- **Smaller Container**: ~100MB vs ~300MB

### Native Executable Limitations

- **Build Time**: Longer compilation time
- **Reflection**: Limited reflection support
- **Dynamic Loading**: No dynamic class loading
- **Debugging**: Limited debugging capabilities

## Cloud Deployment

### AWS ECS

Create `aws/task-definition.json`:

```json
{
  "family": "grpc-learning-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "grpc-learning-service",
      "image": "your-account.dkr.ecr.region.amazonaws.com/grpc-learning-service:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        },
        {
          "containerPort": 9000,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "QUARKUS_PROFILE",
          "value": "prod"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/grpc-learning-service",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -f http://localhost:8080/q/health || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

### Google Cloud Run

Create `gcp/service.yaml`:

```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: grpc-learning-service
  annotations:
    run.googleapis.com/ingress: all
spec:
  template:
    metadata:
      annotations:
        autoscaling.knative.dev/maxScale: "10"
        run.googleapis.com/cpu-throttling: "false"
    spec:
      containerConcurrency: 100
      containers:
        - image: gcr.io/PROJECT-ID/grpc-learning-service:latest
          ports:
            - containerPort: 8080
            - containerPort: 9000
          env:
            - name: QUARKUS_PROFILE
              value: "prod"
          resources:
            limits:
              cpu: "1"
              memory: "512Mi"
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8080
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 8080
```

### Azure Container Instances

```bash
# Create resource group
az group create --name grpc-learning-rg --location eastus

# Deploy container
az container create \
  --resource-group grpc-learning-rg \
  --name grpc-learning-service \
  --image grpc-learning-service:latest \
  --ports 8080 9000 \
  --cpu 1 \
  --memory 1 \
  --environment-variables QUARKUS_PROFILE=prod \
  --restart-policy Always
```

## Configuration Management

### Environment-Specific Configuration

#### Development

```properties
# application.properties
quarkus.grpc.server.reflection-service=true
quarkus.log.level=DEBUG
```

#### Testing

```properties
# application-test.properties
quarkus.grpc.server.port=0
quarkus.http.port=0
quarkus.log.level=WARN
```

#### Production

```properties
# application-prod.properties
quarkus.grpc.server.reflection-service=false
quarkus.log.level=INFO
quarkus.grpc.server.ssl.certificate=${TLS_CERT_PATH}
quarkus.grpc.server.ssl.key=${TLS_KEY_PATH}
```

### External Configuration

#### Kubernetes ConfigMap

```bash
# Create ConfigMap from file
kubectl create configmap grpc-config --from-file=application.properties

# Mount in deployment
spec:
  containers:
  - name: grpc-learning-service
    volumeMounts:
    - name: config
      mountPath: /deployments/config
  volumes:
  - name: config
    configMap:
      name: grpc-config
```

#### Docker Secrets

```bash
# Create secret
echo "my-secret-value" | docker secret create db-password -

# Use in service
docker service create \
  --name grpc-learning-service \
  --secret db-password \
  grpc-learning-service:latest
```

## Monitoring and Observability

### Health Checks

The service provides multiple health check endpoints:

```bash
# Overall health
curl http://localhost:8080/q/health

# Liveness probe
curl http://localhost:8080/q/health/live

# Readiness probe
curl http://localhost:8080/q/health/ready
```

### Metrics

Prometheus metrics are available:

```bash
# All metrics
curl http://localhost:8080/q/metrics

# Application metrics only
curl http://localhost:8080/q/metrics/application
```

### Logging

#### Structured Logging

```properties
# Enable JSON logging
quarkus.log.console.json=true

# Add service metadata
quarkus.log.console.json.additional-field."service.name".value=grpc-learning-service
quarkus.log.console.json.additional-field."service.version".value=1.0.0
```

#### Log Aggregation

For production, use log aggregation tools:

- **ELK Stack**: Elasticsearch, Logstash, Kibana
- **Fluentd**: Log collection and forwarding
- **Splunk**: Enterprise log management
- **Cloud Logging**: AWS CloudWatch, Google Cloud Logging, Azure Monitor

### Distributed Tracing

Add OpenTelemetry for distributed tracing:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-opentelemetry</artifactId>
</dependency>
```

```properties
# Enable tracing
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://jaeger:14250
```

## Security Considerations

### TLS Configuration

```properties
# Enable TLS
quarkus.grpc.server.ssl.certificate=classpath:tls/server-cert.pem
quarkus.grpc.server.ssl.key=classpath:tls/server-key.pem
quarkus.grpc.server.ssl.trust-store=classpath:tls/ca-cert.pem
```

### Network Security

- Use private networks/VPCs
- Implement network policies in Kubernetes
- Configure firewalls appropriately
- Use service mesh for advanced security

### Authentication and Authorization

```java
// Add authentication interceptor
@ApplicationScoped
public class AuthInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String token = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));

        if (!isValidToken(token)) {
            call.close(Status.UNAUTHENTICATED, headers);
            return new ServerCall.Listener<ReqT>() {};
        }

        return next.startCall(call, headers);
    }
}
```

## Troubleshooting Deployment Issues

### Common Issues

1. **Port Conflicts**: Ensure ports 8080 and 9000 are available
2. **Memory Issues**: Increase container memory limits
3. **Startup Timeouts**: Increase health check initial delay
4. **Network Issues**: Check service discovery and DNS resolution
5. **Configuration Issues**: Verify environment-specific properties

### Debugging Commands

```bash
# Check container logs
docker logs grpc-learning-service

# Check Kubernetes pod logs
kubectl logs -f pod/grpc-learning-service-xxx

# Check service connectivity
kubectl exec -it pod/grpc-learning-service-xxx -- curl localhost:8080/q/health

# Check gRPC connectivity
grpcurl -plaintext localhost:9000 list
```

This deployment guide covers the most common deployment scenarios and provides a foundation for deploying the gRPC Learning Service in various environments.
