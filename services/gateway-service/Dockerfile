# Этап 1: Сборка приложения
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Копирование родительского POM и общих модулей
COPY pom.xml .
COPY common/common-dto/pom.xml common/common-dto/
COPY common/common-exception/pom.xml common/common-exception/
COPY common/common-util/pom.xml common/common-util/

# Загрузка зависимостей (кэширование)
RUN mvn dependency:go-offline -B

# Копирование исходного кода общих модулей
COPY common/common-dto/src ./common/common-dto/src
COPY common/common-exception/src ./common/common-exception/src
COPY common/common-util/src ./common/common-util/src

# Сборка общих модулей
RUN mvn clean install -pl common/common-dto,common/common-exception,common/common-util -am -DskipTests

# Копирование POM и исходного кода gateway-service
COPY services/gateway-service/pom.xml services/gateway-service/
COPY services/gateway-service/src ./services/gateway-service/src

# Сборка gateway-service
RUN mvn clean package -pl services/gateway-service -am -DskipTests

# Этап 2: Запуск приложения
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Создание непривилегированного пользователя
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Установка curl для healthcheck
RUN apk --no-cache add curl

# Копирование собранного JAR файла из этапа сборки
COPY --from=builder /build/services/gateway-service/target/*.jar app.jar

# Порт, на котором работает приложение
EXPOSE 8080

# Метки для информации
LABEL maintainer="CarSensor Team"
LABEL version="1.0.0"
LABEL description="Gateway Service for CarSensor Platform"

# Запуск приложения с оптимизациями для контейнера
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker}", \
    "-jar", \
    "app.jar"]

# Healthcheck для проверки работоспособности
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1