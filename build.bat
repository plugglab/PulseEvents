```bat
@echo off
setlocal EnableDelayedExpansion

title Project Builder

:START
cls

echo ==========================================
echo              PROJECT BUILDER
echo ==========================================
echo.

set "PROJECT_TYPE="

REM ==============================
REM Detect Maven
REM ==============================

if exist "pom.xml" (
    set "MAVEN=1"
) else (
    set "MAVEN=0"
)

REM ==============================
REM Detect Gradle
REM ==============================

if exist "gradlew.bat" (
    set "GRADLE=1"
) else (
    if exist "gradlew" (
        set "GRADLE=1"
    ) else (
        set "GRADLE=0"
    )
)

REM ==============================
REM Detect project
REM ==============================

if "%MAVEN%"=="1" if "%GRADLE%"=="0" (
    set "PROJECT_TYPE=MAVEN"
)

if "%GRADLE%"=="1" if "%MAVEN%"=="0" (
    set "PROJECT_TYPE=GRADLE"
)

REM ==============================
REM Both detected
REM ==============================

if "%MAVEN%"=="1" if "%GRADLE%"=="1" (
    echo Wykryto Maven oraz Gradle.
    echo.
    echo [1] Maven
    echo [2] Gradle
    echo.

    choice /c 12 /n /m "Wybierz: "

    if errorlevel 2 (
        set "PROJECT_TYPE=GRADLE"
    ) else (
        set "PROJECT_TYPE=MAVEN"
    )
)

REM ==============================
REM Nothing detected
REM ==============================

if not defined PROJECT_TYPE (
    echo.
    echo [ERROR] Nie wykryto projektu!
    echo.
    echo Brak:
    echo   pom.xml
    echo   gradlew.bat / gradlew
    echo.
    goto MENU
)

REM ==============================
REM Build
REM ==============================

echo.
echo ==========================================
echo Wykryto: %PROJECT_TYPE%
echo ==========================================
echo.

if "%PROJECT_TYPE%"=="MAVEN" (
    echo ^> mvn package
    echo.
    
    mvn package
    
    set "RESULT=!errorlevel!"
)

if "%PROJECT_TYPE%"=="GRADLE" (
    echo ^> gradlew build
    echo.

    if exist "gradlew.bat" (
        call gradlew.bat build
    ) else (
        call gradlew build
    )

    set "RESULT=!errorlevel!"
)

echo.
echo ==========================================

if "!RESULT!"=="0" (
    echo             BUILD SUCCESS
) else (
    echo             BUILD FAILED
)

echo ==========================================
echo.

:MENU

echo [1] Sprobuj ponownie
echo [2] Wyjdz
echo.

choice /c 12 /n /m "Wybierz opcje: "

if errorlevel 2 goto CLOSE
if errorlevel 1 goto START

:CLOSE

echo.
echo Konsola pozostanie otwarta.
echo Nacisnij dowolny klawisz, aby zamknac...
pause >nul

endlocal
```
