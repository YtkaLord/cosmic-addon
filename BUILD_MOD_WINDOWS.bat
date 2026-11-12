@echo off
chcp 65001 >nul
echo ============================================================================
echo   Better Leveling: Cosmic Skill Tree (Forge 1.16.5 - 36.2.39, LWJGL 3.2.2)
echo   Автоматическая сборка готового .JAR файла мода в 1 клик
echo ============================================================================
echo.

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ОШИБКА] Java не найдена в PATH! Пожалуйста, установите Java 8 JDK ^(Adoptium Temurin 8^) и запустите этот файл снова.
    pause
    exit /b 1
)

if exist "gradlew.bat" (
    echo [1/2] Запуск сборки через Gradle Wrapper...
    call gradlew.bat build
) else (
    where gradle >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        echo [1/2] Генерация Gradle Wrapper и сборка мода...
        call gradle wrapper --gradle-version 7.5.1
        call gradlew.bat build
    ) else (
        echo [1/2] Скачивание портативного Gradle 7.5.1 для автоматической сборки...
        powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-7.5.1-bin.zip' -OutFile 'gradle-bin.zip'; Expand-Archive -Path 'gradle-bin.zip' -DestinationPath '.gradle-portable' -Force; Remove-Item 'gradle-bin.zip'"
        if exist ".gradle-portable\gradle-7.5.1\bin\gradle.bat" (
            call ".gradle-portable\gradle-7.5.1\bin\gradle.bat" build
        ) else (
            echo [ОШИБКА] Не удалось скачать Gradle. Откройте эту папку в IntelliJ IDEA и нажмите Build!
            pause
            exit /b 1
        )
    )
)

echo.
if exist "build\libs\betterleveling-cosmic-addon-1.16.5-1.0.0.jar" (
    echo ============================================================================
    echo [УСПЕХ!] Готовый мод успешно скомпилирован и реобфусцирован под 1.16.5!
    echo Файл находится здесь:
    echo   %CD%\build\libs\betterleveling-cosmic-addon-1.16.5-1.0.0.jar
    echo Скопируйте его в папку mods вашей сборки Minecraft 1.16.5!
    echo ============================================================================
    explorer "build\libs"
) else (
    echo [ВНИМАНИЕ] Сборка завершилась с ошибкой. Проверьте, что используется JDK 8 ^(Java 1.8^).
)
pause
