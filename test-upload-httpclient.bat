@echo off
setlocal

if "%~1"=="" (
    echo Usage: test-upload.bat "path\to\file"
    echo Example: test-upload.bat "C:\data\test.txt"
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
