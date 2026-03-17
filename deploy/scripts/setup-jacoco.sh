#!/bin/bash

echo "========================================="
echo "🚀 JaCoCo Aggregate Report Setup Script"
echo "========================================="
echo

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Функция для вывода с цветом
print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Корень проекта (находим автоматически)
SCRIPT_PATH="$(readlink -f "$0")"
PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$SCRIPT_PATH")")")"

echo -e "Script location: ${YELLOW}$SCRIPT_PATH${NC}"
echo -e "Project root: ${YELLOW}$PROJECT_ROOT${NC}"
echo

# Переходим в корень проекта
cd "$PROJECT_ROOT" || {
    print_error "Cannot change to project root: $PROJECT_ROOT"
    exit 1
}

# Шаг 1: Создание папки jacoco-aggregate
print_step "1. Creating jacoco-aggregate directory..."
if [ ! -d "jacoco-aggregate" ]; then
    mkdir -p jacoco-aggregate
    print_success "Directory created: jacoco-aggregate"
else
    print_warning "Directory already exists: jacoco-aggregate"
fi
echo

# Шаг 2: Создание pom.xml для jacoco-aggregate
print_step "2. Creating jacoco-aggregate/pom.xml..."

cat > jacoco-aggregate/pom.xml << 'EOF'
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
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>jacoco-aggregate</artifactId>
    <packaging>pom</packaging>
    <name>JaCoCo Aggregate Report</name>
    <description>Aggregated code coverage report for all modules</description>

    <properties>
        <maven.deploy.skip>true</maven.deploy.skip>
        <maven.install.skip>true</maven.install.skip>
    </properties>

    <dependencies>
        <!-- Common modules -->
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
            <groupId>com.carsensor.platform</groupId>
            <artifactId>common-test</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Service modules -->
        <dependency>
            <groupId>com.carsensor.platform</groupId>
            <artifactId>auth-service</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.carsensor.platform</groupId>
            <artifactId>car-service</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.carsensor.platform</groupId>
            <artifactId>scheduler-service</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.carsensor.platform</groupId>
            <artifactId>gateway-service</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>report-aggregate</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>report-aggregate</goal>
                        </goals>
                        <configuration>
                            <title>CarSensor Platform Coverage Report</title>
                            <outputDirectory>${project.reporting.outputDirectory}/jacoco-aggregate</outputDirectory>
                            <includes>
                                <include>com/carsensor/platform/**/*.class</include>
                            </includes>
                            <excludes>
                                <exclude>**/test/**/*.class</exclude>
                                <exclude>**/Test*.class</exclude>
                                <exclude>**/*Test.class</exclude>
                                <exclude>**/*Tests.class</exclude>
                                <exclude>**/*IT.class</exclude>
                            </excludes>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF

print_success "File created: jacoco-aggregate/pom.xml"
echo

# Шаг 3: Проверка наличия модуля в родительском pom.xml
print_step "3. Checking parent pom.xml for jacoco-aggregate module..."

if [ ! -f "pom.xml" ]; then
    print_error "Parent pom.xml not found!"
    exit 1
fi

if grep -q "<module>jacoco-aggregate</module>" pom.xml; then
    print_warning "Module already exists in parent pom.xml"
else
    print_step "Adding jacoco-aggregate to parent pom.xml..."
    # Создаем резервную копию
    cp pom.xml pom.xml.bak
    # Добавляем модуль после последнего существующего модуля
    sed -i '/<modules>/a \ \ \ \ <module>jacoco-aggregate</module>' pom.xml
    print_success "Module added to parent pom.xml (backup created: pom.xml.bak)"
fi
echo

# Шаг 4: Очистка и сборка проекта
print_step "4. Cleaning and building project..."
echo -e "${YELLOW}This may take a few minutes...${NC}"
echo

mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    print_success "Project built successfully"
else
    print_error "Project build failed"
    exit 1
fi
echo

# Шаг 5: Запуск тестов
print_step "5. Running tests..."
echo -e "${YELLOW}Running all tests to generate coverage data...${NC}"
echo

mvn test

if [ $? -eq 0 ]; then
    print_success "Tests completed successfully"
else
    print_error "Tests failed"
    exit 1
fi
echo

# Шаг 6: Генерация агрегированного отчета
print_step "6. Generating JaCoCo aggregate report..."
echo

mvn verify -pl jacoco-aggregate

if [ $? -eq 0 ]; then
    print_success "Report generated successfully"
else
    print_error "Report generation failed"
    exit 1
fi
echo

# Шаг 7: Открытие отчета
print_step "7. Opening report..."

REPORT_PATH="$PROJECT_ROOT/jacoco-aggregate/target/site/jacoco-aggregate/index.html"

if [ -f "$REPORT_PATH" ]; then
    echo -e "${GREEN}Report location:${NC} $REPORT_PATH"
    echo

    # Для Windows (особая обработка)
    if [[ "$REPORT_PATH" == /mnt/* ]]; then
        # Конвертируем WSL путь в Windows путь
        WIN_PATH=$(wslpath -w "$REPORT_PATH" 2>/dev/null)
        if [ -n "$WIN_PATH" ]; then
            echo -e "${YELLOW}Opening in Windows: $WIN_PATH${NC}"
            explorer.exe "$WIN_PATH" 2>/dev/null || \
            cmd.exe /c start "$WIN_PATH" 2>/dev/null || \
            echo -e "${YELLOW}Please open manually: $WIN_PATH${NC}"
        else
            echo -e "${YELLOW}Please open manually: $REPORT_PATH${NC}"
        fi
    else
        # Linux/Mac
        if command -v xdg-open &> /dev/null; then
            xdg-open "$REPORT_PATH"
        elif command -v firefox &> /dev/null; then
            firefox "$REPORT_PATH"
        elif command -v google-chrome &> /dev/null; then
            google-chrome "$REPORT_PATH"
        else
            echo -e "${YELLOW}Please open manually: $REPORT_PATH${NC}"
        fi
    fi
else
    print_error "Report file not found at: $REPORT_PATH"
fi
echo

# Шаг 8: Показываем результаты тестов
print_step "8. Test summary from previous run:"
echo

# Показываем последние строки с количеством тестов
grep -E "Tests run: [0-9]+" <(mvn test | tail -50) 2>/dev/null || echo "   No test summary available"

echo

# Шаг 9: Итоговая информация
echo "========================================="
echo -e "${GREEN}✅ Setup Complete!${NC}"
echo "========================================="
echo
echo -e "${BLUE}Report Summary:${NC}"
echo "  📊 Location: jacoco-aggregate/target/site/jacoco-aggregate/index.html"
echo "  📁 Full path: $REPORT_PATH"
echo
echo -e "${BLUE}Quick Commands from project root:${NC}"
echo "  🔄 Regenerate report:  mvn verify -pl jacoco-aggregate"
echo "  🧪 Run tests only:     mvn test"
echo "  🏗️  Build only:        mvn clean install -DskipTests"
echo "  📈 Run all + report:   mvn clean test verify -pl jacoco-aggregate"
echo
echo -e "${BLUE}Project Structure:${NC}"
echo "  📁 $PROJECT_ROOT"
echo "      ├── jacoco-aggregate/"
echo "      │   └── pom.xml"
echo "      ├── common/"
echo "      ├── services/"
echo "      └── pom.xml (updated)"
echo
echo "========================================="