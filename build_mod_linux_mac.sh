#!/usr/bin/env bash
set -e

echo "============================================================================"
echo "  Better Leveling: Cosmic Skill Tree (Forge 1.16.5 - 36.2.39, LWJGL 3.2.2)"
echo "  Автоматическая сборка готового .JAR файла мода"
echo "============================================================================"

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew build
elif command -v gradle >/dev/null 2>&1; then
    gradle build
else
    echo "Скачивание портативного Gradle 7.5.1..."
    curl -L "https://services.gradle.org/distributions/gradle-7.5.1-bin.zip" -o gradle-bin.zip
    unzip -q gradle-bin.zip -d .gradle-portable
    rm -f gradle-bin.zip
    ./.gradle-portable/gradle-7.5.1/bin/gradle build
fi

echo ""
echo "[УСПЕХ!] Готовый JAR файл находится в: build/libs/betterleveling-cosmic-addon-1.16.5-1.0.0.jar"
