@echo off
setlocal

if "%~1"=="" (
    echo Usage: test-upload-httpclient-memory.bat "path\to\file"
    echo Example: test-upload-httpclient-memory.bat "C:\data\test.txt"
    echo This script runs the client with 64MB heap limit for memory testing
    pause
    exit /b 1
)

REM 서버가 실행 중인지 확인
curl -s http://localhost:8080/actuator/health > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Server is not running. Please start the server first.
    echo Run: start-server.bat
    pause
    exit /b 1
)

set FILE_PATH=%~1
set TARGET_URL=http://localhost:8080/api/v1/files/upload

echo Testing file upload with memory constraints...
echo File: %FILE_PATH%
echo Server: %TARGET_URL%
echo Client heap limit: 64MB
echo.

cd file-file-stream-httpclient
java -Xmx64m -Xms32m -jar target\file-file-stream-httpclient-1.0.0.jar --file.path="%FILE_PATH%" --target.url="%TARGET_URL%"

pause
