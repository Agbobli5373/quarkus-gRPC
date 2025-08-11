####
# This Dockerfile is used in order to build a container that runs the Quarkus application in JVM mode
#
# Before building the container image run:
#
# ./mvnw package
#
# Then, build the image with:
#
# docker build -f Dockerfile -t quarkus/grpc-learning-service .
#
# Then run the container using:
#
# docker run -i --rm -p 8080:8080 -p 9000:9000 quarkus/grpc-learning-service
#
# If you want to include the debug port into your docker image
# you will have to expose the debug port (default 5005 being the default) like this :  EXPOSE 8080 5005.
# Additionally you will have to set -e JAVA_DEBUG=true and -e JAVA_DEBUG_PORT=*:5005
# when running the container
#
# Then run the container using :
#
# docker run -i --rm -p 8080:8080 -p 9000:9000 -p 5005:5005 -e JAVA_DEBUG=true -e JAVA_DEBUG_PORT=*:5005 quarkus/grpc-learning-service
#
# This image uses the `run-java.sh` script to run the application.
# This scripts computes the command line to execute your Java application, and
# includes memory/GC tuning.
# You can configure the behavior using the following environment properties:
# - JAVA_OPTS: JVM options passed to the `java` command (example: "-verbose:class")
# - JAVA_OPTS_APPEND: User specified Java options to be appended to generated options
#   in JAVA_OPTS (example: "-Dsome.property=foo")
# - QUARKUS_OPTS: Quarkus options (example: "--help")
# - AB_OFF: turn off autobahn usage
# - JAVA_APP_JAR: Path to the JAR file, in case you want to override the default
# - JAVA_APP_DIR: Path to the directory containing the JAR file
# - JAVA_DEBUG: If set remote debugging will be switched on. Disabled by default (example: "true")
# - JAVA_DEBUG_PORT: Port used for remote debugging (default: 5005)
# - CONTAINER_HEAP_PERCENT: Percentage of available memory allocated to the heap (default: 50)
# - CONTAINER_MAX_HEAP_PERCENT: Percentage of available memory allocated to the heap (default: 80)
# - GC_MIN_HEAP_FREE_RATIO: Minimum percentage of heap free after GC to avoid expansion (default: 10)
# - GC_MAX_HEAP_FREE_RATIO: Maximum percentage of heap free after GC to avoid shrinking (default: 20)
# - GC_TIME_RATIO: Specifies the ratio of the time spent outside the garbage collection (default: 4)
# - GC_ADAPTIVE_SIZE_POLICY_WEIGHT: The weighting given to the current GC time versus previous GC times (default: 90)
# - GC_METASPACE_SIZE: The initial metaspace size (default: 20m)
# - GC_MAX_METASPACE_SIZE: The maximum metaspace size (default: 100m)
# - GC_CONTAINER_OPTIONS: Specify Java GC to use. The default is G1GC
# - HTTPS_PROXY: The location of the https proxy, this takes precedence over http_proxy
# - HTTP_PROXY: The location of the http proxy
# - NO_PROXY: A comma separated lists of hosts, IP addresses or domains that can be accessed directly

FROM registry.access.redhat.com/ubi8/openjdk-21:1.20

ENV LANGUAGE='en_US:en'

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --chown=185 target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 target/quarkus-app/*.jar /deployments/
COPY --chown=185 target/quarkus-app/app/ /deployments/app/
COPY --chown=185 target/quarkus-app/quarkus/ /deployments/quarkus/

# Expose both HTTP and gRPC ports
EXPOSE 8080 9000

# Set environment variables for production
ENV QUARKUS_PROFILE=prod
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Dquarkus.grpc.server.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/q/health || exit 1

USER 185