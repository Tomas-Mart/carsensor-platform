#!/bin/bash

echo "🚀 Запуск CarSensor Platform"

cd /mnt/d/DiskD/Java/Projects/carsensor-platform

# Запуск БД в Docker
echo "📦 Запуск баз данных..."
cd deploy/docker
docker-compose up -d postgres-auth postgres-car postgres-scheduler redis

# Ждем инициализации БД
echo "⏳ Ожидание инициализации БД..."
sleep 10

# Запуск сервисов (каждый в фоне)
echo "🚀 Запуск Auth Service..."
cd ../../services/auth-service
../../mvnw spring-boot:run -Dspring-boot.run.profiles=dev > ../auth-service.log 2>&1 &
echo $! > ../auth-service.pid

cd ../car-service
echo "🚀 Запуск Car Service..."
../../mvnw spring-boot:run -Dspring-boot.run.profiles=dev > ../car-service.log 2>&1 &
echo $! > ../car-service.pid

cd ../scheduler-service
echo "🚀 Запуск Scheduler Service..."
../../mvnw spring-boot:run -Dspring-boot.run.profiles=dev > ../scheduler-service.log 2>&1 &
echo $! > ../scheduler-service.pid

cd ../gateway-service
echo "🚀 Запуск Gateway Service..."
../../mvnw spring-boot:run -Dspring-boot.run.profiles=dev > ../gateway-service.log 2>&1 &
echo $! > ../gateway-service.pid

echo "✅ Все сервисы запущены!"
echo "📊 Логи сервисов:"
echo "  auth-service.log"
echo "  car-service.log"
echo "  scheduler-service.log"
echo "  gateway-service.log"
echo ""
echo "🔍 Проверка статуса:"
echo "  curl http://localhost:8081/actuator/health"
echo "  curl http://localhost:8082/actuator/health"
echo "  curl http://localhost:8083/actuator/health"
echo "  curl http://localhost:8080/actuator/health"