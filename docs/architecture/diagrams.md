# Архитектурные диаграммы CarSensor Platform
## 1. Диаграмма контекста (C4 Level 1)
Общий обзор системы и взаимодействие с внешними пользователями и системами.
```mermaid
C4Context
    title System Context diagram for CarSensor Platform

    Person(customer, "Пользователь", "Пользователь, который ищет автомобили")
    Person(admin, "Администратор", "Управляет системой")

    System(carsensor, "CarSensor Platform", "Позволяет просматривать автомобили с японских аукционов")

    System_Ext(externalSite, "CarSensor.net", "Японский сайт с автомобилями")
    System_Ext(emailSystem, "Email System", "Внешняя почтовая система")

    Rel(customer, carsensor, "Просматривает автомобили, использует фильтры")
    Rel(admin, carsensor, "Управляет системой")
    Rel(carsensor, externalSite, "Собирает данные о автомобилях (раз в час)")
    Rel(carsensor, emailSystem, "Отправляет уведомления")
```
## 2. Диаграмма контейнеров (C4 Level 2)
Микросервисная архитектура и взаимодействие между сервисами.
```mermaid
C4Container
    title Container diagram for CarSensor Platform

    Person(customer, "Пользователь", "Ищет автомобили")
    Person(admin, "Администратор", "Управляет системой")

    System_Boundary(carsensor, "CarSensor Platform") {
        Container(web, "Web Application", "Next.js, TypeScript", "Обеспечивает интерфейс для пользователей")
        Container(apiGateway, "API Gateway", "Spring Cloud Gateway", "Маршрутизация запросов, JWT проверка")
        
        Container(auth, "Auth Service", "Spring Boot", "JWT, регистрация, вход")
        Container(car, "Car Service", "Spring Boot", "CRUD, фильтрация, пагинация")
        Container(scheduler, "Scheduler Service", "Spring Boot", "Парсинг CarSensor.net")
        
        ContainerDb(authDb, "Auth Database", "PostgreSQL", "Хранит пользователей и роли")
        ContainerDb(carDb, "Car Database", "PostgreSQL", "Хранит автомобили")
    }
    System_Ext(externalSite, "CarSensor.net", "Японский сайт")

    Rel(customer, web, "Использует", "HTTPS")
    Rel(admin, web, "Управляет", "HTTPS")
    Rel(web, apiGateway, "Отправляет запросы", "HTTP")
    Rel(apiGateway, auth, "Проверяет авторизацию", "HTTP")
    Rel(apiGateway, car, "Запрашивает данные", "HTTP")
    Rel(apiGateway, scheduler, "Запускает парсинг", "HTTP")
    
    Rel(auth, authDb, "Читает/пишет")
    Rel(car, carDb, "Читает/пишет")
    
    Rel(scheduler, externalSite, "Парсит HTML", "HTTP")
    Rel(scheduler, car, "Сохраняет данные", "HTTP")
```
## 3. Диаграмма последовательности для авторизации (UML Sequence)
```mermaid
sequenceDiagram
    participant Пользователь
    participant NextJS as Next.js Frontend
    participant Gateway as API Gateway
    participant Auth as Auth Service
    participant Database as PostgreSQL (Auth)

    Пользователь->>NextJS: Вводит логин/пароль
    NextJS->>Gateway: POST /api/v1/auth/login
    Gateway->>Auth: POST /login
    Auth->>Database: SELECT user WHERE username = ?
    Database-->>Auth: Данные пользователя
    Auth->>Auth: Проверка пароля
    Auth->>Auth: Генерация JWT токена
    Auth-->>Gateway: JWT токен
    Gateway-->>NextJS: JWT токен
    NextJS-->>Пользователь: Успешный вход
```
## 4. Диаграмма последовательности для парсинга (UML Sequence)
```mermaid
sequenceDiagram
    participant Scheduler as Scheduler Service
    participant Parser as CarSensor Parser
    participant CarService as Car Service
    participant Database as PostgreSQL (Cars)

    loop Каждый час
        Scheduler->>Parser: parseCars(5)
        
        Parser->>CarSensor.net: GET /usedcar/search.php
        CarSensor.net-->>Parser: HTML страница
        
        Parser->>Parser: Извлечение данных
        Parser->>CarService: saveAllCars(list)
        CarService->>Database: INSERT INTO cars
        Database-->>CarService: Подтверждение
        
        CarService-->>Parser: Сохраненные автомобили
        Parser-->>Scheduler: Результат парсинга
    end
```
## 5. Диаграмма развертывания (Deployment)
```mermaid
architecture-beta
    group docker(cloud)[Docker Environment]

    service gateway(server)[Gateway Service] in docker
    service auth(server)[Auth Service] in docker
    service car(server)[Car Service] in docker
    service scheduler(server)[Scheduler Service] in docker
    service frontend(server)[Frontend] in docker
    
    service postgresAuth(database)[Auth DB] in docker
    service postgresCar(database)[Car DB] in docker
    
    service user(internet)[User Browser]
    service carsensor(internet)[CarSensor.net]

    user:R -- L:gateway
    gateway:R -- L:auth
    gateway:R -- L:car
    gateway:R -- L:scheduler
    gateway:R -- L:frontend
    
    auth:R -- L:postgresAuth
    car:R -- L:postgresCar
    
    scheduler:R -- L:carsensor
```
## 6. Диаграмма классов (сущность Car)
```mermaid
classDiagram
    class Car {
        +Long id
        +String brand
        +String model
        +int year
        +int mileage
        +BigDecimal price
        +String description
        +String exteriorColor
        +String transmission
        +String driveType
        +String engineCapacity
        +String[] photoUrls
        +LocalDateTime parsedAt
        +getFullInfo()
        +formatPrice()
    }

    class CarRepository {
        +findByBrand(String brand)
        +findByYearRange(int from, int to)
        +findByPriceRange(BigDecimal min, BigDecimal max)
    }

    class CarService {
        +getCarsWithFilters()
        +getCarById()
        +createCar()
        +updateCar()
    }

    CarRepository --> Car
    CarService --> CarRepository
```
## 7. ER-диаграмма (База данных)
```mermaid
erDiagram
    USER ||--o{ ROLE : has
    USER {
        bigint id PK
        varchar username
        varchar password
        varchar email
        boolean active
        timestamp created_at
    }
    
    ROLE {
        bigint id PK
        varchar name
    }
    
    USER_ROLE {
        bigint user_id FK
        bigint role_id FK
    }
    
    CAR {
        bigint id PK
        varchar brand
        varchar model
        int year
        int mileage
        decimal price
        varchar exterior_color
        varchar transmission
        varchar drive_type
        varchar engine_capacity
        text photo_urls
        timestamp parsed_at
    }
    
    USER ||--o{ USER_ROLE : has
    ROLE ||--o{ USER_ROLE : has
```
## 8. Диаграмма состояний парсинга
```mermaid
stateDiagram-v2
    [*] --> IDLE
    
    IDLE --> PARSING_LIST: Начать парсинг
    PARSING_LIST --> PARSING_DETAILS: Найдены автомобили
    PARSING_DETAILS --> SAVING: Данные получены
    SAVING --> COMPLETED: Сохранение завершено
    SAVING --> FAILED: Ошибка сохранения
    PARSING_LIST --> STOPPED: Пользователь остановил
    
    COMPLETED --> IDLE
    FAILED --> IDLE
    STOPPED --> IDLE
```
## 📊 Легенда
```text
---------------------------------------------------------------------
|Иконка|       Элемент        |                Описание             |                         
|:----:|:--------------------:|:-----------------------------------:|
|  🧑  | **Пользователь**     | Внешний пользователь системы        |
|  🖥️  | **Сервис**           | Микросервис (Spring Boot)           |
|  🗄️   | **База данных**      | PostgreSQL хранилище               |
|  🌐  | **Внешняя система**  | CarSensor.net и другие внешние API  |
|  🔄  | **Поток данных**     | Направление взаимодействия          |
--------------------------------------------------------------------|
```
## 📝 **Как использовать**
```bash
1. Просто скопируйте этот код в файл `diagrams.md`
2. GitHub автоматически отобразит все диаграммы
3. В VS Code установите плагин **Markdown Preview Mermaid Support**
```