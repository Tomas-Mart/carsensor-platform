#!/bin/bash

# ============================================================
# FULL PROJECT ERROR REPORT - CarSensor Platform
# ============================================================

PROJECT_ROOT="/home/mina/projects/carsensor-platform"
REPORT_FILE="full-project-report-$(date +%Y%m%d-%H%M%S).log"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}   CAR SENSOR PLATFORM - FULL REPORT${NC}"
echo -e "${BLUE}=========================================${NC}"
echo "Дата: $(date)"
echo ""

{
echo "========================================="
echo "   CAR SENSOR PLATFORM - FULL REPORT"
echo "========================================="
echo "Дата: $(date)"
echo "Хост: $(hostname)"
echo "Пользователь: $(whoami)"
echo "Директория: $(pwd)"
echo ""
echo ""

# ============================================================
# 1. ИНФОРМАЦИЯ О СБОРКЕ
# ============================================================
echo "========================================="
echo "1. BUILD ENVIRONMENT INFORMATION"
echo "========================================="
echo ""
echo "Java Version:"
java -version 2>&1
echo ""
echo "Maven Version:"
mvn --version 2>&1 | head -5
echo ""
echo "OS Info:"
uname -a
echo ""

# ============================================================
# 2. СТРУКТУРА ПРОЕКТА
# ============================================================
echo "========================================="
echo "2. PROJECT STRUCTURE"
echo "========================================="
echo ""
echo "Modules found:"
find . -maxdepth 2 -name "pom.xml" -type f | grep -v target | sort | while read pom; do
    dir=$(dirname "$pom")
    echo "  - $dir"
done
echo ""

# ============================================================
# 3. ПРОВЕРКА ВСЕХ МОДУЛЕЙ
# ============================================================
echo "========================================="
echo "3. MODULES STATUS"
echo "========================================="
echo ""

for module in common/common-dto common/common-exception common/common-util common/common-test services/auth-service services/car-service services/gateway-service services/scheduler-service; do
    echo "--- MODULE: $module ---"
    if [ -d "$module" ]; then
        echo "  ✓ Module directory exists"
        
        # Проверка pom.xml
        if [ -f "$module/pom.xml" ]; then
            echo "  ✓ pom.xml exists"
            
            # Проверка версии
            version=$(grep -m1 "<version>" "$module/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
            echo "  Version: $version"
        else
            echo "  ✗ pom.xml MISSING"
        fi
        
        # Проверка target директории
        if [ -d "$module/target" ]; then
            jar_count=$(find "$module/target" -name "*.jar" -type f 2>/dev/null | wc -l)
            echo "  Target exists, JAR files: $jar_count"
        else
            echo "  Target directory not found (not built yet)"
        fi
        
        # Проверка тестовых отчетов
        if [ -d "$module/target/surefire-reports" ]; then
            test_count=$(find "$module/target/surefire-reports" -name "*.txt" -type f 2>/dev/null | wc -l)
            echo "  Test reports: $test_count files"
        fi
    else
        echo "  ✗ Module directory NOT FOUND"
    fi
    echo ""
done

# ============================================================
# 4. AUTH-SERVICE DETAILED ANALYSIS
# ============================================================
echo "========================================="
echo "4. AUTH-SERVICE DETAILED ANALYSIS"
echo "========================================="
echo ""

# 4.1 Проверка ресурсов
echo "--- 4.1 Test Resources ---"
echo ""

echo "Source test resources:"
if [ -d "services/auth-service/src/test/resources" ]; then
    find services/auth-service/src/test/resources -type f -name "*.sql" -o -name "*.yml" -o -name "*.properties" -o -name "*.xml" 2>/dev/null | sort | while read file; do
        size=$(stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null)
        echo "  ✓ $(basename "$file") ($size bytes)"
    done
else
    echo "  ✗ src/test/resources not found"
fi
echo ""

echo "Target test resources:"
if [ -d "services/auth-service/target/test-classes" ]; then
    find services/auth-service/target/test-classes -type f -name "*.sql" -o -name "*.yml" -o -name "*.properties" -o -name "*.xml" 2>/dev/null | sort | while read file; do
        size=$(stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null)
        echo "  ✓ $(basename "$file") ($size bytes)"
    done
else
    echo "  ✗ target/test-classes not found (run mvn test-compile first)"
fi
echo ""

# 4.2 Проверка schema.sql
echo "--- 4.2 schema.sql Validation ---"
echo ""

SOURCE_SCHEMA="services/auth-service/src/test/resources/db/test/schema.sql"
TARGET_SCHEMA="services/auth-service/target/test-classes/db/test/schema.sql"

echo "Source schema.sql:"
if [ -f "$SOURCE_SCHEMA" ]; then
    echo "  ✓ File exists"
    echo "  Size: $(stat -c%s "$SOURCE_SCHEMA" 2>/dev/null || stat -f%z "$SOURCE_SCHEMA" 2>/dev/null) bytes"
    echo "  Modified: $(stat -c%y "$SOURCE_SCHEMA" 2>/dev/null || stat -f%Sm "$SOURCE_SCHEMA" 2>/dev/null)"
    echo "  Lines: $(wc -l < "$SOURCE_SCHEMA")"
    echo "  Semicolon check:"
    if grep -q "NEW.updated_at = CURRENT_TIMESTAMP;" "$SOURCE_SCHEMA"; then
        echo "    ✓ Semicolon present"
    else
        echo "    ✗ Semicolon MISSING!"
        echo "    Current line:"
        grep "NEW.updated_at =" "$SOURCE_SCHEMA"
    fi
else
    echo "  ✗ File NOT FOUND"
fi
echo ""

echo "Target schema.sql:"
if [ -f "$TARGET_SCHEMA" ]; then
    echo "  ✓ File exists"
    echo "  Size: $(stat -c%s "$TARGET_SCHEMA" 2>/dev/null || stat -f%z "$TARGET_SCHEMA" 2>/dev/null) bytes"
    echo "  Modified: $(stat -c%y "$TARGET_SCHEMA" 2>/dev/null || stat -f%Sm "$TARGET_SCHEMA" 2>/dev/null)"
    echo "  Lines: $(wc -l < "$TARGET_SCHEMA")"
    echo "  Semicolon check:"
    if grep -q "NEW.updated_at = CURRENT_TIMESTAMP;" "$TARGET_SCHEMA"; then
        echo "    ✓ Semicolon present"
    else
        echo "    ✗ Semicolon MISSING!"
        echo "    Current line:"
        grep "NEW.updated_at =" "$TARGET_SCHEMA"
    fi
else
    echo "  ✗ File NOT FOUND"
fi
echo ""

# 4.3 Сравнение файлов
if [ -f "$SOURCE_SCHEMA" ] && [ -f "$TARGET_SCHEMA" ]; then
    echo "--- 4.3 File Comparison ---"
    echo ""
    if diff -q "$SOURCE_SCHEMA" "$TARGET_SCHEMA" > /dev/null 2>&1; then
        echo "  ✓ Files are IDENTICAL"
    else
        echo "  ✗ Files are DIFFERENT"
        echo ""
        echo "  Differences:"
        diff -u "$SOURCE_SCHEMA" "$TARGET_SCHEMA" | head -30
    fi
    echo ""
fi

# ============================================================
# 5. TEST RESULTS BY MODULE
# ============================================================
echo "========================================="
echo "5. TEST RESULTS BY MODULE"
echo "========================================="
echo ""

for module in services/auth-service services/car-service services/gateway-service services/scheduler-service; do
    echo "--- $module ---"
    if [ -d "$module/target/surefire-reports" ]; then
        echo "  Test reports found:"
        
        # Подсчет тестов
        total=0
        failures=0
        errors=0
        
        for report in "$module"/target/surefire-reports/*.txt; do
            if [ -f "$report" ]; then
                if grep -q "Tests run:" "$report" 2>/dev/null; then
                    t=$(grep "Tests run:" "$report" | head -1 | sed 's/.*Tests run: \([0-9]*\).*/\1/')
                    f=$(grep "Tests run:" "$report" | head -1 | sed 's/.*Failures: \([0-9]*\).*/\1/')
                    e=$(grep "Tests run:" "$report" | head -1 | sed 's/.*Errors: \([0-9]*\).*/\1/')
                    total=$((total + t))
                    failures=$((failures + f))
                    errors=$((errors + e))
                fi
            fi
        done
        
        echo "  Total tests: $total"
        echo "  Failures: $failures"
        echo "  Errors: $errors"
        
        if [ $failures -gt 0 ] || [ $errors -gt 0 ]; then
            echo "  Failed tests:"
            grep -l "FAILURE\|ERROR" "$module"/target/surefire-reports/*.txt 2>/dev/null | while read file; do
                echo "    - $(basename "$file")"
            done
        else
            echo "  ✓ All tests passed"
        fi
    else
        echo "  No test reports found (tests not run)"
    fi
    echo ""
done

# ============================================================
# 6. ALL FAILED TESTS DETAILS
# ============================================================
echo "========================================="
echo "6. ALL FAILED TESTS DETAILS"
echo "========================================="
echo ""

for module in services/auth-service services/car-service services/gateway-service services/scheduler-service; do
    if [ -d "$module/target/surefire-reports" ]; then
        for report in "$module"/target/surefire-reports/*.txt; do
            if [ -f "$report" ] && grep -q "FAILURE\|ERROR" "$report" 2>/dev/null; then
                echo ""
                echo "========================================="
                echo "MODULE: $module"
                echo "FILE: $(basename "$report")"
                echo "========================================="
                echo ""
                
                # Выводим ошибку
                grep -A 30 "FAILURE\|ERROR\|Exception" "$report" | head -50
                echo ""
                echo "---"
                echo ""
            fi
        done
    fi
done

# ============================================================
# 7. POM.XML VALIDATION
# ============================================================
echo "========================================="
echo "7. POM.XML VALIDATION"
echo "========================================="
echo ""

for pom in services/auth-service/pom.xml services/car-service/pom.xml services/gateway-service/pom.xml services/scheduler-service/pom.xml; do
    if [ -f "$pom" ]; then
        echo "--- $pom ---"
        
        # Проверка testResources
        if grep -q "<testResources>" "$pom"; then
            echo "  ✓ testResources configured"
        else
            echo "  ✗ testResources NOT configured"
        fi
        
        # Проверка surefire
        if grep -q "maven-surefire-plugin" "$pom"; then
            echo "  ✓ surefire-plugin configured"
        else
            echo "  ✗ surefire-plugin NOT configured"
        fi
        
        echo ""
    fi
done

# ============================================================
# 8. COMMON-TEST MODULE ANALYSIS
# ============================================================
echo "========================================="
echo "8. COMMON-TEST MODULE ANALYSIS"
echo "========================================="
echo ""

if [ -d "common/common-test" ]; then
    echo "Common-test module found"
    
    # Проверка основных классов
    for class in AbstractIntegrationTest AbstractJpaTest DatabasePropertyFactory; do
        if [ -f "common/common-test/src/main/java/com/carsensor/common/test/$class.java" ]; then
            echo "  ✓ $class.java exists"
        else
            echo "  ✗ $class.java MISSING"
        fi
    done
    
    echo ""
    echo "Key configurations in AbstractIntegrationTest:"
    if [ -f "common/common-test/src/main/java/com/carsensor/common/test/AbstractIntegrationTest.java" ]; then
        grep -E "autoCommit|maximumPoolSize|connectionTimeout" "common/common-test/src/main/java/com/carsensor/common/test/AbstractIntegrationTest.java" | head -5
    fi
fi
echo ""

# ============================================================
# 9. ROOT CAUSES AND SOLUTIONS
# ============================================================
echo "========================================="
echo "9. ROOT CAUSES AND SOLUTIONS"
echo "========================================="
echo ""

echo "IDENTIFIED ISSUES:"
echo ""

# Проверка 1: schema.sql в target
if [ -f "services/auth-service/src/test/resources/db/test/schema.sql" ] && [ ! -f "services/auth-service/target/test-classes/db/test/schema.sql" ]; then
    echo "✗ ISSUE 1: schema.sql not copied to target/test-classes"
    echo "  SOLUTION: Add testResources to pom.xml or run:"
    echo "    mkdir -p services/auth-service/target/test-classes/db/test/"
    echo "    cp services/auth-service/src/test/resources/db/test/schema.sql services/auth-service/target/test-classes/db/test/"
    echo ""
fi

# Проверка 2: точка с запятой
if [ -f "services/auth-service/src/test/resources/db/test/schema.sql" ]; then
    if ! grep -q "NEW.updated_at = CURRENT_TIMESTAMP;" "services/auth-service/src/test/resources/db/test/schema.sql"; then
        echo "✗ ISSUE 2: Missing semicolon in trigger function"
        echo "  SOLUTION: sed -i 's/NEW.updated_at = CURRENT_TIMESTAMP$/NEW.updated_at = CURRENT_TIMESTAMP;/' services/auth-service/src/test/resources/db/test/schema.sql"
        echo ""
    fi
fi

# Проверка 3: Различия файлов
if [ -f "services/auth-service/src/test/resources/db/test/schema.sql" ] && [ -f "services/auth-service/target/test-classes/db/test/schema.sql" ]; then
    if ! diff -q "services/auth-service/src/test/resources/db/test/schema.sql" "services/auth-service/target/test-classes/db/test/schema.sql" > /dev/null 2>&1; then
        echo "✗ ISSUE 3: Source and target files differ"
        echo "  SOLUTION: cp services/auth-service/src/test/resources/db/test/schema.sql services/auth-service/target/test-classes/db/test/schema.sql"
        echo ""
    fi
fi

# Проверка 4: testResources в pom.xml
if [ -f "services/auth-service/pom.xml" ] && ! grep -q "<testResources>" "services/auth-service/pom.xml"; then
    echo "✗ ISSUE 4: testResources not configured in pom.xml"
    echo "  SOLUTION: Add to services/auth-service/pom.xml:"
    echo "    <testResources>"
    echo "        <testResource>"
    echo "            <directory>src/test/resources</directory>"
    echo "            <filtering>false</filtering>"
    echo "            <includes>"
    echo "                <include>**/*.sql</include>"
    echo "                <include>**/*.yml</include>"
    echo "            </includes>"
    echo "        </testResource>"
    echo "    </testResources>"
    echo ""
fi

# Проверка 5: Hikari autoCommit
if [ -f "common/common-test/src/main/java/com/carsensor/common/test/AbstractIntegrationTest.java" ]; then
    if grep -q "autoCommit.*false" "common/common-test/src/main/java/com/carsensor/common/test/AbstractIntegrationTest.java"; then
        echo "✗ ISSUE 5: Hikari autoCommit=false may cause issues"
        echo "  SOLUTION: Set autoCommit=true in AbstractIntegrationTest.databaseProperties()"
        echo ""
    fi
fi

echo "========================================="
echo "   END OF REPORT"
echo "========================================="

} 2>&1 | tee "$REPORT_FILE"

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}REPORT SAVED TO: $REPORT_FILE${NC}"
echo -e "${GREEN}=========================================${NC}"

# Показ краткого резюме
echo ""
echo -e "${YELLOW}Quick Summary:${NC}"
grep -E "ISSUE|✓|✗" "$REPORT_FILE" | tail -20
