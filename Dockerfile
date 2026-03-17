# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Копируем только POM файлы для кэширования зависимостей
COPY pom.xml .
COPY common/common-dto/pom.xml common/common-dto/
COPY common/common-exception/pom.xml common/common-exception/
COPY common/common-util/pom.xml common/common-util/
COPY common/common-test/pom.xml common/common-test/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/car-service/pom.xml services/car-service/
COPY services/scheduler-service/pom.xml services/scheduler-service/
COPY services/gateway-service/pom.xml services/gateway-service/

# Скачиваем зависимости (с кэшированием)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B \
    -pl services/gateway-service \
    -am

# Копируем исходники
COPY common/common-dto/src ./common/common-dto/src
COPY common/common-exception/src ./common/common-exception/src
COPY common/common-util/src ./common/common-util/src
COPY common/common-test/src ./common/common-test/src
COPY services/gateway-service/src ./services/gateway-service/src

# Собираем только gateway-service и его зависимости
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl services/gateway-service -am -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Добавляем curl для healthcheck
RUN apk --no-cache add curl

# Создаем непривилегированного пользователя
RUN addgroup -S spring && adduser -S spring -G spring

# Копируем JAR из builder stage
COPY --from=builder /build/services/gateway-service/target/gateway-service-*.jar app.jar

# Права на папку
RUN chown -R spring:spring /app

# Метки для Railway
LABEL maintainer="CarSensor Team" \
      version="1.0.0" \
      description="Gateway Service for CarSensor Platform" \
      railway-deploy="true"

# Порт для Railway (Railway автоматически проксирует 8080)
EXPOSE 8080

# Переключаемся на непривилегированного пользователя
USER spring:spring

# Healthcheck для Railway
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Запуск с оптимизациями для контейнера
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker}", \
    "-jar", "app.jar"]