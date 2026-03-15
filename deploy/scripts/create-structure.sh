#!/bin/bash

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Создание структуры проекта CarSensor Platform${NC}"
echo -e "${BLUE}========================================${NC}"

# Создание корневой директории
PROJECT_NAME="carsensor-platform"
echo -e "${BLUE}Создание проекта: ${PROJECT_NAME}${NC}"
mkdir -p $PROJECT_NAME
cd $PROJECT_NAME

# Создание родительского pom.xml
echo -e "${GREEN}Создание родительского pom.xml${NC}"
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.carsensor.platform</groupId>
    <artifactId>carsensor-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>CarSensor Platform</name>
    <description>Микросервисная платформа для парсинга CarSensor.net</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.2</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-cloud.version>2024.0.0</spring-cloud.version>
        <jsoup.version>1.18.1</jsoup.version>
        <resilience4j.version>2.2.0</resilience4j.version>
        <jjwt.version>0.12.6</jjwt.version>
        <lombok.version>1.18.34</lombok.version>
        <mapstruct.version>1.6.2</mapstruct.version>
        <testcontainers.version>1.20.4</testcontainers.version>
    </properties>

    <modules>
        <module>common/common-dto</module>
        <module>common/common-exception</module>
        <module>common/common-util</module>
        <module>auth-service</module>
        <module>car-service</module>
        <module>scheduler-service</module>
        <module>gateway-service</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.carsensor.platform</groupId>
                <artifactId>common-dto</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.carsensor.platform</groupId>
                <artifactId>common-exception</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.carsensor.platform</groupId>
                <artifactId>common-util</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.jsoup</groupId>
                <artifactId>jsoup</artifactId>
                <version>${jsoup.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-spring-boot3</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <version>0.8.12</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
EOF

# Создание .gitignore
echo -e "${GREEN}Создание .gitignore${NC}"
cat > .gitignore << 'EOF'
# Compiled files
*.class
target/
*.log
*.jar
*.war
*.ear
*.lock

# IDE
.idea/
*.iws
*.iml
*.ipr
.settings/
.classpath
.project
.factorypath
.springBeans
.vscode/
.mvn/wrapper/maven-wrapper.jar

# Logs
logs/
log/

# OS
.DS_Store
Thumbs.db

# Environment
.env
application-local.yml
application-dev.yml

# Frontend
node_modules/
.next/
out/
build/
dist/
.npm
.yarn
coverage/

# Docker
*.pid
docker-compose.override.yml
EOF

# Создание common модулей
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Создание common модулей${NC}"
echo -e "${BLUE}========================================${NC}"

mkdir -p common/common-dto/src/main/java/com/carsensor/platform/dto
mkdir -p common/common-dto/src/test/java
mkdir -p common/common-exception/src/main/java/com/carsensor/platform/exception/handler
mkdir -p common/common-exception/src/test/java
mkdir -p common/common-util/src/main/java/com/carsensor/platform/util
mkdir -p common/common-util/src/test/java

# common-dto pom.xml
cat > common/common-dto/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.carsensor.platform</groupId>
        <artifactId>carsensor-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>common-dto</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
EOF

# common-exception pom.xml
cat > common/common-exception/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.carsensor.platform</groupId>
        <artifactId>carsensor-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>common-exception</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
EOF

# common-util pom.xml
cat > common/common-util/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.carsensor.platform</groupId>
        <artifactId>carsensor-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>common-util</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
</project>
EOF

# Создание структуры тестовых папок
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Создание структуры тестов${NC}"
echo -e "${BLUE}========================================${NC}"

# Auth Service тесты
mkdir -p auth-service/src/test/java/com/carsensor/auth/unit/service
mkdir -p auth-service/src/test/java/com/carsensor/auth/unit/security
mkdir -p auth-service/src/test/java/com/carsensor/auth/integration/controller
mkdir -p auth-service/src/test/java/com/carsensor/auth/integration/repository
mkdir -p auth-service/src/test/java/com/carsensor/auth/contract
mkdir -p auth-service/src/test/resources/db/test
mkdir -p auth-service/src/test/resources/html

# Car Service тесты
mkdir -p car-service/src/test/java/com/carsensor/car/unit/service
mkdir -p car-service/src/test/java/com/carsensor/car/unit/mapper
mkdir -p car-service/src/test/java/com/carsensor/car/integration/controller
mkdir -p car-service/src/test/java/com/carsensor/car/integration/repository
mkdir -p car-service/src/test/java/com/carsensor/car/performance
mkdir -p car-service/src/test/resources/db/test

# Scheduler Service тесты
mkdir -p scheduler-service/src/test/java/com/carsensor/scheduler/unit/parser
mkdir -p scheduler-service/src/test/java/com/carsensor/scheduler/unit/dictionary
mkdir -p scheduler-service/src/test/java/com/carsensor/scheduler/integration/scheduler
mkdir -p scheduler-service/src/test/resources/html

# Common тесты
mkdir -p common/common-test/src/test/java/com/carsensor/common/test

# Создание файла AbstractIntegrationTest.java (пустой)
touch common/common-test/src/test/java/com/carsensor/common/test/AbstractIntegrationTest.java

# Создание application-test.properties для каждого сервиса
cat > auth-service/src/test/resources/application-test.properties << 'EOF'
spring.datasource.url=jdbc:tc:postgresql:16-alpine:///testdb
spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.flyway.enabled=false
logging.level.com.carsensor=DEBUG
EOF

cat > car-service/src/test/resources/application-test.properties << 'EOF'
spring.datasource.url=jdbc:tc:postgresql:16-alpine:///testdb
spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.flyway.enabled=false
logging.level.com.carsensor=DEBUG
EOF

cat > scheduler-service/src/test/resources/application-test.properties << 'EOF'
logging.level.com.carsensor=DEBUG
car-service.url=http://localhost:8082
EOF

# Создание тестового HTML файла для парсера
cat > scheduler-service/src/test/resources/html/carsensor-sample.html << 'EOF'
<html>
<body>
    <div class="cassetteItem">
        <h2>トヨタ カローラ</h2>
        <div class="year">2020年</div>
        <div class="mileage">45,000km</div>
        <div class="price">250万円</div>
        <div class="color">ホワイト</div>
        <div class="transmission">AT</div>
        <div class="drive">4WD</div>
        <img src="/images/car1.jpg" />
        <img src="/images/car1_2.jpg" />
    </div>
    <div class="cassetteItem">
        <h2>ホンダ フィット</h2>
        <div class="year">2021年</div>
        <div class="mileage">35,000km</div>
        <div class="price">180万円</div>
        <div class="color">シルバー</div>
        <div class="transmission">CVT</div>
        <div class="drive">2WD</div>
        <img src="/images/car2.jpg" />
    </div>
</body>
</html>
EOF

# Создание SQL файлов для тестовых данных
cat > auth-service/src/test/resources/db/test/insert-test-user.sql << 'EOF'
-- Очистка перед вставкой
TRUNCATE TABLE users CASCADE;

-- Вставка тестового пользователя (пароль: admin123)
INSERT INTO users (id, username, email, password, first_name, last_name, is_active, created_at, updated_at)
VALUES (
    1,
    'admin',
    'admin@test.com',
    '$2a$10$rTqUJvKzLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQpLQ',
    'Admin',
    'User',
    true,
    NOW(),
    NOW()
);

-- Назначение ролей
INSERT INTO user_roles (user_id, role_id)
SELECT 1, id FROM roles WHERE name = 'ROLE_ADMIN';
EOF

cat > auth-service/src/test/resources/db/test/cleanup.sql << 'EOF'
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE user_roles CASCADE;
EOF

cat > car-service/src/test/resources/db/test/insert-test-cars.sql << 'EOF'
-- Очистка перед вставкой
TRUNCATE TABLE cars CASCADE;

-- Вставка тестовых автомобилей
INSERT INTO cars (id, brand, model, year, mileage, price, exterior_color, transmission, drive_type, parsed_at, created_at)
VALUES
    (1, 'Toyota', 'Camry', 2020, 50000, 2500000, 'White', 'AT', '2WD', NOW(), NOW()),
    (2, 'Toyota', 'RAV4', 2021, 30000, 3500000, 'Black', 'CVT', '4WD', NOW(), NOW()),
    (3, 'Honda', 'Civic', 2022, 10000, 1800000, 'Red', 'MT', '2WD', NOW(), NOW());

-- Сброс последовательности
SELECT setval('cars_id_seq', (SELECT MAX(id) FROM cars));
EOF

cat > car-service/src/test/resources/db/test/cleanup-cars.sql << 'EOF'
TRUNCATE TABLE cars CASCADE;
EOF

echo -e "${GREEN}✅ Структура тестов создана${NC}"

# Создание Dockerfile для сервисов
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Создание Dockerfile для сервисов${NC}"
echo -e "${BLUE}========================================${NC}"

# Auth Service Dockerfile
cat > auth-service/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Car Service Dockerfile
cat > car-service/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Scheduler Service Dockerfile
cat > scheduler-service/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Gateway Service Dockerfile
cat > gateway-service/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Создание docker-compose.yml
echo -e "${GREEN}Создание docker-compose.yml${NC}"
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  postgres-auth:
    image: postgres:16-alpine
    container_name: carsensor-postgres-auth
    environment:
      POSTGRES_DB: auth_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5433:5432"
    volumes:
      - postgres-auth-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d auth_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - carsensor-network

  postgres-car:
    image: postgres:16-alpine
    container_name: carsensor-postgres-car
    environment:
      POSTGRES_DB: car_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5434:5432"
    volumes:
      - postgres-car-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d car_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - carsensor-network

  auth-service:
    build: ./auth-service
    container_name: carsensor-auth
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres-auth
      DB_PORT: 5432
      DB_NAME: auth_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      JWT_SECRET: ${JWT_SECRET:-mySuperSecretKeyForJWTTokenGeneration}
    ports:
      - "8081:8081"
    depends_on:
      postgres-auth:
        condition: service_healthy
    networks:
      - carsensor-network

  car-service:
    build: ./car-service
    container_name: carsensor-car
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres-car
      DB_PORT: 5432
      DB_NAME: car_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      JWT_SECRET: ${JWT_SECRET:-mySuperSecretKeyForJWTTokenGeneration}
    ports:
      - "8082:8082"
    depends_on:
      postgres-car:
        condition: service_healthy
    networks:
      - carsensor-network

  scheduler-service:
    build: ./scheduler-service
    container_name: carsensor-scheduler
    environment:
      SPRING_PROFILES_ACTIVE: docker
      CAR_SERVICE_URL: http://car-service:8082
    ports:
      - "8083:8083"
    depends_on:
      car-service:
        condition: service_healthy
    networks:
      - carsensor-network

  gateway-service:
    build: ./gateway-service
    container_name: carsensor-gateway
    environment:
      SPRING_PROFILES_ACTIVE: docker
      AUTH_SERVICE_URL: http://auth-service:8081
      CAR_SERVICE_URL: http://car-service:8082
      JWT_SECRET: ${JWT_SECRET:-mySuperSecretKeyForJWTTokenGeneration}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:3000}
    ports:
      - "8080:8080"
    depends_on:
      auth-service:
        condition: service_healthy
      car-service:
        condition: service_healthy
    networks:
      - carsensor-network

  frontend:
    build: ./frontend
    container_name: carsensor-frontend
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8080
    ports:
      - "3000:3000"
    depends_on:
      gateway-service:
        condition: service_healthy
    networks:
      - carsensor-network

volumes:
  postgres-auth-data:
  postgres-car-data:

networks:
  carsensor-network:
    driver: bridge
EOF

# Создание README.md
echo -e "${GREEN}Создание README.md${NC}"
cat > README.md << 'EOF'
# CarSensor Platform

Микросервисная платформа для парсинга и просмотра автомобилей с японского сайта CarSensor.net.

## Технологический стек

### Backend
- **Java 21** + Spring Boot 3.4.x
- **Spring Cloud Gateway** - API Gateway
- **Spring Security** + JWT
- **PostgreSQL 16** + Flyway
- **Jsoup** - парсинг HTML
- **Resilience4j** - Circuit Breaker

### Frontend
- **Next.js 16** + TypeScript
- **Tailwind CSS 4** + shadcn/ui
- **React Hook Form** + Zod

## Быстрый старт

1. Клонировать репозиторий:
   ```bash
   git clone <your-repo-url>
   cd carsensor-platform
Запустить все сервисы:

bash
docker-compose up -d
Открыть приложение:

Frontend: http://localhost:3000

API: http://localhost:8080

Тестовые учетные данные
Логин: admin

Пароль: admin123

Структура проекта
text
carsensor-platform/
├── auth-service/           # Сервис аутентификации
├── car-service/            # Сервис автомобилей
├── scheduler-service/      # Сервис парсера
├── gateway-service/        # API Gateway
├── common/                 # Общие модули
│   ├── common-dto/        # Общие DTO
│   ├── common-exception/  # Обработка ошибок
│   └── common-util/       # Утилиты
├── frontend/               # Next.js приложение
├── monitoring/             # Конфигурация мониторинга
├── docker-compose.yml      # Docker Compose
└── README.md               # Документация
EOF

Создание структуры для frontend
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Создание структуры frontend${NC}"
echo -e "${BLUE}========================================${NC}"

package.json для frontend
cat > frontend/package.json << 'EOF'
{
"name": "carsensor-frontend",
"version": "1.0.0",
"private": true,
"scripts": {
"dev": "next dev",
"build": "next build",
"start": "next start",
"lint": "next lint"
},
"dependencies": {
"next": "16.0.0",
"react": "19.0.0",
"react-dom": "19.0.0",
"typescript": "5.7.2",
"tailwindcss": "4.0.0",
"axios": "1.7.9",
"swr": "2.3.0",
"react-hook-form": "7.54.1",
"zod": "3.24.1",
"lucide-react": "0.468.0"
}
}
EOF

Dockerfile для frontend
cat > frontend/Dockerfile << 'EOF'
FROM node:20-alpine AS base

Install dependencies only when needed
FROM base AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci

Rebuild the source code only when needed
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

Production image, copy all the files and run next
FROM base AS runner
WORKDIR /app

ENV NODE_ENV production

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["node", "server.js"]
EOF

Создание мониторинга
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Создание конфигурации мониторинга${NC}"
echo -e "${BLUE}========================================${NC}"

mkdir -p monitoring

prometheus.yml
cat > monitoring/prometheus.yml << 'EOF'
global:
scrape_interval: 15s
evaluation_interval: 15s

scrape_configs:

job_name: 'auth-service'
metrics_path: '/actuator/prometheus'
static_configs:

targets: ['auth-service:8081']
labels:
application: 'auth-service'

job_name: 'car-service'
metrics_path: '/actuator/prometheus'
static_configs:

targets: ['car-service:8082']
labels:
application: 'car-service'

job_name: 'scheduler-service'
metrics_path: '/actuator/prometheus'
static_configs:

targets: ['scheduler-service:8083']
labels:
application: 'scheduler-service'

job_name: 'gateway-service'
metrics_path: '/actuator/prometheus'
static_configs:

targets: ['gateway-service:8080']
labels:
application: 'gateway-service'
EOF

Финальное сообщение
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✅ Структура проекта успешно создана!${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Проект создан в: ${PWD}${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Следующие шаги:"
echo -e "1. ${GREEN}cd ${PROJECT_NAME}${NC}"
echo -e "2. ${GREEN}chmod +x create-structure.sh${NC} (если еще не сделано)"
echo -e "3. ${GREEN}./create-structure.sh${NC} (для создания структуры)"
echo -e "4. ${GREEN}docker-compose up -d${NC} (для запуска всех сервисов)"
echo -e "${BLUE}========================================${NC}"

text

## Как использовать скрипт:

1. **Сохраните этот полный скрипт в файл `create-structure.sh`**

2. **Сделайте его исполняемым:**
bash
chmod +x create-structure.sh

Запустите скрипт:
bash
./create-structure.sh