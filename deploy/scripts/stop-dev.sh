#!/bin/bash

echo "🛑 Остановка CarSensor Platform..."

# Остановка всех Java процессов
pkill -f 'spring-boot:run'
pkill -f 'jar'

# Остановка Docker контейнеров
docker-compose down

# Остановка frontend
pkill -f 'node'

echo "✅ Все сервисы остановлены"