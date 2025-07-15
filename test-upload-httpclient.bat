@echo off
setlocal

if "%~1"=="" (
    echo Usage: test-upload-httpclient.bat "path\to\file"
    echo Example: test-upload-httpclient.bat "C:\data\test.txt"
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

echo Testing file upload...
echo File: %FILE_PATH%
echo Server: %TARGET_URL%
echo.

cd file-file-stream-httpclient
java -jar target\file-file-stream-httpclient-1.0.0.jar --file.path="%FILE_PATH%" --target.url="%TARGET_URL%"

pause
