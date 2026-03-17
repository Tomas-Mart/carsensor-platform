# Базовый образ с Java и Maven для сборки
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Копирование всего проекта
COPY . /app
WORKDIR /app

# Сборка scheduler-service
RUN mvn clean package -pl services/scheduler-service -am -DskipTests

# Финальный образ
FROM eclipse-temurin:21-jre-alpine

# Копирование собранного JAR из предыдущего этапа
COPY --from=builder /app/services/scheduler-service/target/scheduler-service-*.jar /app/app.jar

# Порт приложения
EXPOSE 8083

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
