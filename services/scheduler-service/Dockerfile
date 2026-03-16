# Этап 1: Сборка приложения
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Копирование родительского POM и общих модулей
COPY pom.xml .
COPY common/common-dto/pom.xml common/common-dto/
COPY common/common-exception/pom.xml common/common-exception/
COPY common/common-util/pom.xml common/common-util/
COPY common/common-test/pom.xml common/common-test/

# Копирование POM-файлов всех сервисов (нужно для родительского POM)
COPY services/auth-service/pom.xml services/auth-service/
COPY services/car-service/pom.xml services/car-service/
COPY services/scheduler-service/pom.xml services/scheduler-service/
COPY services/gateway-service/pom.xml services/gateway-service/

# Копирование исходного кода общих модулей
COPY common/common-dto/src ./common/common-dto/src
COPY common/common-exception/src ./common/common-exception/src
COPY common/common-util/src ./common/common-util/src
COPY common/common-test/src ./common/common-test/src

# Сборка общих модулей
RUN mvn clean install -pl common/common-dto,common/common-exception,common/common-util,common/common-test -am -DskipTests

# Копирование POM и исходного кода scheduler-service
COPY services/scheduler-service/pom.xml services/scheduler-service/
COPY services/scheduler-service/src ./services/scheduler-service/src

# Сборка scheduler-service
RUN mvn clean package -pl services/scheduler-service -am -DskipTests

# Этап 2: Запуск приложения
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Установка curl ДО создания пользователя (ОДИН РАЗ)
RUN apk --no-cache add curl

# Создание непривилегированного пользователя
RUN addgroup -S spring && adduser -S spring -G spring

# Копирование собранного JAR файла из этапа сборки
COPY --from=builder /build/services/scheduler-service/target/*.jar app.jar

# Даем права на файл пользователю spring
RUN chown -R spring:spring /app

# Порт, на котором работает приложение
EXPOSE 8083

# Переключаемся на непривилегированного пользователя (ПОСЛЕ установки curl)
USER spring:spring

# Метки для информации
LABEL maintainer="CarSensor Team"
LABEL version="1.0.0"
LABEL description="Scheduler Service for CarSensor Platform"

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
  CMD curl -f http://localhost:8083/actuator/health || exit 1