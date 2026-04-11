FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk --no-cache add curl
RUN addgroup -S spring && adduser -S spring -G spring

COPY target/car-service-*.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]