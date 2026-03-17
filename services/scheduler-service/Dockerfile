FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY common/common-dto/pom.xml common/common-dto/
COPY common/common-exception/pom.xml common/common-exception/
COPY common/common-util/pom.xml common/common-util/
COPY common/common-test/pom.xml common/common-test/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/car-service/pom.xml services/car-service/
COPY services/scheduler-service/pom.xml services/scheduler-service/
COPY services/gateway-service/pom.xml services/gateway-service/
COPY common/common-dto/src ./common/common-dto/src
COPY common/common-exception/src ./common/common-exception/src
COPY common/common-util/src ./common/common-util/src
COPY common/common-test/src ./common/common-test/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean install -pl common/common-dto,common/common-exception, \
    common/common-util,common/common-test -am -DskipTests
COPY services/scheduler-service/pom.xml services/scheduler-service/
COPY services/scheduler-service/src ./services/scheduler-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl services/scheduler-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk --no-cache add curl
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /build/services/scheduler-service/target/*.jar app.jar
RUN chown -R spring:spring /app
EXPOSE 8083
USER spring:spring
LABEL maintainer="CarSensor Team" version="1.0.0" description="Scheduler Service for CarSensor Platform"
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java",
"-XX:+UseContainerSupport",
"-XX:MaxRAMPercentage=75.0",
"-XX:+UseG1GC", "-XX:+HeapDumpOnOutOfMemoryError",
"-XX:HeapDumpPath=/tmp/heapdump.hprof",
"-Djava.security.egd=file:/dev/./urandom",
"-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker}",
"-jar", "app.jar"]