#!/bin/bash

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "================================="
echo -e "${CYAN}ЗАПУСК ТЕСТА: LinkedPracticesIntegrationTest${NC}"
echo "================================="

# Временный файл для вывода
TEMP_OUTPUT=$(mktemp)

# Запускаем тест и фильтруем вывод
mvn -Dtest=LinkedPracticesIntegrationTest test -q 2>&1 | \
    grep -v -E "^\[INFO\]|^\[ERROR\] Tests run|^\[ERROR\] Failures|^\[ERROR\] org\.opentest4j|^\[ERROR\] at |^\[ERROR\] \.\.\.|^\[ERROR\] Failed to execute|^\[ERROR\] See |^\[ERROR\] ->|^Downloading|^Downloaded|^Running|^Results|^Total time|^Finished at|^\[WARNING\]|^\[ERROR\] Re-run|^\[ERROR\] For more information|^Java HotSpot|^\[ERROR\] \[" | \
    grep -E "===|🔍|📌|✅|❌|⏳|🔄|Core\.|Yoga\." > "$TEMP_OUTPUT" 2>&1

# Получаем код возврата Maven (не теста)
EXIT_CODE=$?

# Показываем отфильтрованный вывод
cat "$TEMP_OUTPUT"



# Удаляем временный файл
rm "$TEMP_OUTPUT"

