#!/bin/bash

echo "🔍 Проверка проекта CarSensor Platform"
echo "======================================"

# Проверка Java
echo "📌 Java version:"
java -version

# Проверка Maven
echo -e "\n📌 Maven version:"
./mvnw -version

# Проверка Docker
echo -e "\n📌 Docker version:"
docker --version
docker-compose --version

# Проверка Node.js
echo -e "\n📌 Node.js version:"
node --version
npm --version

# Компиляция проекта
echo -e "\n🛠 Компиляция проекта..."
./mvnw clean compile -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Компиляция успешна!"
else
    echo "❌ Ошибка компиляции!"
    exit 1
fi

# Проверка структуры
echo -e "\n📁 Структура проекта:"
ls -la

echo -e "\n✅ Все проверки пройдены!"