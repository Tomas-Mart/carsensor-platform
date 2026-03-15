#!/bin/bash

echo "🚀 Запуск CarSensor Platform в режиме разработки"

# Функция для проверки ошибок
check_error() {
    if [ $? -ne 0 ]; then
        echo "❌ Ошибка: $1"
        exit 1
    fi
}

# Запуск баз данных
echo "📦 Запуск PostgreSQL..."
docker-compose up -d postgres-auth postgres-car
check_error "Не удалось запустить PostgreSQL"

# Ожидание готовности баз данных
echo "⏳ Ожидание готовности баз данных..."
sleep 10

# Запуск backend сервисов
echo "🔧 Запуск Auth Service..."
cd auth-service && ../mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
cd ..

echo "🔧 Запуск Car Service..."
cd car-service && ../mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
cd ..

echo "🔧 Запуск Scheduler Service..."
cd scheduler-service && ../mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
cd ..

echo "🔧 Запуск Gateway Service..."
cd gateway-service && ../mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
cd ..

# Запуск frontend
echo "🎨 Запуск Frontend..."
cd frontend && npm run dev &

echo "✅ Все сервисы запущены!"
echo "📌 Frontend: http://localhost:3000"
echo "📌 API Gateway: http://localhost:8080"
echo "📌 Auth Service: http://localhost:8081"
echo "📌 Car Service: http://localhost:8082"
echo "📌 Scheduler Service: http://localhost:8083"

# Ожидание завершения всех процессов
wait