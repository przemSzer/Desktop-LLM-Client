@echo off
echo Building and running the Agent application...
echo.

REM Build the project
call mvnw.cmd clean install -q
if %errorlevel% neq 0 (
    echo Error: Build failed!
    pause
    exit /b 1
)

REM Run the JavaFX application
call mvnw.cmd javafx:run -pl ui
if %errorlevel% neq 0 (
    echo Error: Application failed to start!
    pause
    exit /b 1
)

pause 