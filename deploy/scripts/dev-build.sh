#!/bin/bash

# ============================================================
# Скрипт для ежедневной разработки CarSensor Platform
# ============================================================
# Использование: ./dev-build.sh [OPTION]
#
# Опции:
#   full    - Полная пересборка с очисткой
#   test    - Запуск всех тестов
#   fast    - Быстрая сборка без тестов (по умолчанию)
#   deploy  - Сборка для деплоя
#   help    - Показать справку
# ============================================================

set -e  # Остановка при любой ошибке

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Конфигурация
PROJECT_NAME="CarSensor Platform"
AUTH_SERVICE="services/auth-service"
MAVEN_OPTS="-Xmx2g -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Логирование
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверка наличия Maven
check_maven() {
    if ! command -v mvn &> /dev/null; then
        log_error "Maven not found. Please install Maven first."
        exit 1
    fi
}

# Проверка наличия Java 21
check_java() {
    if ! java -version 2>&1 | grep -q "version \"21\""; then
        log_warning "Java 21 is recommended. Current version:"
        java -version
    fi
}

# Очистка временных файлов
clean_temp_files() {
    log_info "Cleaning temporary files..."

    # Очистка target директорий
    find . -name "target" -type d -prune -exec rm -rf {} \; 2>/dev/null || true

    # Очистка временных файлов embedded-postgres
    rm -rf ~/.embedded-postgres/ 2>/dev/null || true
    rm -rf services/*/target/tmp/ 2>/dev/null || true
    rm -rf services/*/target/embedded-postgres-data/ 2>/dev/null || true

    log_success "Temporary files cleaned"
}

# Очистка Maven кэша проекта
clean_maven_cache() {
    log_info "Cleaning Maven cache for project..."
    rm -rf ~/.m2/repository/com/carsensor/ 2>/dev/null || true
    log_success "Maven cache cleaned"
}

# Полная очистка
full_clean() {
    log_info "Performing full clean..."
    clean_temp_files
    clean_maven_cache
    log_success "Full clean completed"
}

# Обновление зависимостей
update_dependencies() {
    log_info "Updating dependencies..."
    mvn dependency:purge-local-repository -DreResolve=false -DactTransitively=false 2>/dev/null || true
    log_success "Dependencies updated"
}

# Быстрая сборка без тестов
fast_build() {
    log_info "Starting fast build (without tests)..."

    export MAVEN_OPTS="$MAVEN_OPTS"

    mvn clean install -DskipTests \
        -Dmaven.javadoc.skip=true \
        -Dmaven.source.skip=true \
        -Dmaven.test.skip=true \
        -Dcheckstyle.skip=true \
        -Dpmd.skip=true

    if [ $? -eq 0 ]; then
        log_success "Fast build completed successfully"
        show_artifacts
    else
        log_error "Fast build failed"
        exit 1
    fi
}

# Полная сборка с тестами
full_build() {
    log_info "Starting full build with tests..."

    export MAVEN_OPTS="$MAVEN_OPTS"

    mvn clean install

    if [ $? -eq 0 ]; then
        log_success "Full build completed successfully"
        show_artifacts
    else
        log_error "Full build failed"
        exit 1
    fi
}

# Запуск всех тестов
run_all_tests() {
    log_info "Running all tests..."

    export MAVEN_OPTS="$MAVEN_OPTS"

    mvn test

    if [ $? -eq 0 ]; then
        log_success "All tests passed"
        show_test_report
    else
        log_error "Some tests failed"
        show_failed_tests
        exit 1
    fi
}

# Запуск тестов только auth-service
run_auth_tests() {
    log_info "Running auth-service tests..."

    export MAVEN_OPTS="$MAVEN_OPTS"

    mvn test -pl $AUTH_SERVICE

    if [ $? -eq 0 ]; then
        log_success "Auth-service tests passed"
    else
        log_error "Auth-service tests failed"
        show_failed_tests
        exit 1
    fi
}

# Запуск конкретного теста
run_specific_test() {
    local test_name=$1
    if [ -z "$test_name" ]; then
        log_error "Please specify test name"
        exit 1
    fi

    log_info "Running test: $test_name"

    export MAVEN_OPTS="$MAVEN_OPTS"

    mvn test -pl $AUTH_SERVICE -Dtest="$test_name"

    if [ $? -eq 0 ]; then
        log_success "Test $test_name passed"
    else
        log_error "Test $test_name failed"
        exit 1
    fi
}

# Показать артефакты
show_artifacts() {
    log_info "Generated artifacts:"
    find services/*/target -name "*.jar" -type f 2>/dev/null | while read -r jar; do
        size=$(du -h "$jar" | cut -f1)
        echo "  - $jar ($size)"
    done
}

# Показать отчет о тестах
show_test_report() {
    log_info "Test summary:"
    find . -path "*/target/surefire-reports/*.txt" -name "*.txt" 2>/dev/null | while read -r report; do
        if grep -q "Tests run:" "$report" 2>/dev/null; then
            echo "  - $(basename "$report"): $(grep "Tests run:" "$report" | head -1)"
        fi
    done
}

# Показать упавшие тесты
show_failed_tests() {
    log_error "Failed tests:"
    find . -path "*/target/surefire-reports/*.txt" -name "*.txt" 2>/dev/null | while read -r report; do
        if grep -q "FAILURE\|ERROR" "$report" 2>/dev/null; then
            echo "  - $(basename "$report"):"
            grep -E "Tests run:|<<< FAILURE|<<< ERROR" "$report" | head -5
        fi
    done
}

# Сборка для деплоя
deploy_build() {
    log_info "Building for deployment..."

    export MAVEN_OPTS="$MAVEN_OPTS"

    mvn clean package -DskipTests \
        -Dmaven.javadoc.skip=false \
        -Dmaven.source.skip=false \
        -Pproduction

    if [ $? -eq 0 ]; then
        log_success "Deployment build completed"

        # Копирование артефактов в директорию deploy
        mkdir -p target/deploy
        cp services/*/target/*.jar target/deploy/ 2>/dev/null || true

        log_info "Artifacts copied to target/deploy/"
        show_artifacts
    else
        log_error "Deployment build failed"
        exit 1
    fi
}

# Запуск сервисов локально
run_locally() {
    log_info "Starting auth-service locally..."

    cd $AUTH_SERVICE
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
}

# Проверка здоровья приложения
health_check() {
    log_info "Checking application health..."

    local port=8081
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            log_success "Application is healthy on port $port"
            return 0
        fi
        log_info "Waiting for application to start (attempt $attempt/$max_attempts)..."
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "Application health check failed"
    return 1
}

# Показать справку
show_help() {
    cat << EOF
${BLUE}CarSensor Platform - Development Script${NC}

Usage: ./dev-build.sh [COMMAND]

Commands:
  fast        Fast build without tests (default)
  full        Full clean build with all tests
  test        Run all tests
  test-auth   Run only auth-service tests
  test-spec   Run specific test: ./dev-build.sh test-spec AuthContractTest
  deploy      Build for deployment (production profile)
  run         Run auth-service locally
  clean       Clean temporary files only
  purge       Full clean including Maven cache
  health      Check application health
  help        Show this help message

Examples:
  ./dev-build.sh fast          # Quick build for development
  ./dev-build.sh test-auth     # Run auth-service tests
  ./dev-build.sh deploy        # Build for production
  ./dev-build.sh run           # Start auth-service locally

EOF
}

# Главная функция
main() {
    # Проверка зависимостей
    check_maven
    check_java

    # Получение команды
    local command=${1:-fast}

    case $command in
        fast)
            full_clean
            fast_build
            ;;
        full)
            full_clean
            full_build
            ;;
        test)
            full_clean
            fast_build
            run_all_tests
            ;;
        test-auth)
            full_clean
            fast_build
            run_auth_tests
            ;;
        test-spec)
            full_clean
            fast_build
            run_specific_test "$2"
            ;;
        deploy)
            full_clean
            deploy_build
            ;;
        run)
            run_locally
            ;;
        clean)
            clean_temp_files
            ;;
        purge)
            full_clean
            ;;
        health)
            health_check
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Запуск
main "$@"