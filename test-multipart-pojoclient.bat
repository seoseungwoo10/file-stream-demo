@echo off
REM Test script for multipart POJO client

echo Testing File Multipart POJO Client
echo ===================================

REM Check if multipart server is running
echo Checking if multipart server is running...
curl -s http://localhost:8081/actuator/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Multipart server is not running on port 8081
    echo Please start the server first: java -jar file-multipart-server/target/file-multipart-server-1.0.0.jar
    pause
    exit /b 1
)
echo Multipart server is running.
echo.

REM Single file upload test
echo Test 1: Single file upload with default metadata
java -jar file-multipart-pojoclient/target/file-multipart-pojoclient-1.0.0.jar ^
    --files="test-multipart.txt" ^
    --target.url="http://localhost:8081/api/v1/multipart/upload"

if %errorlevel% neq 0 (
    echo ERROR: Single file upload failed
    pause
    exit /b 1
)

echo.
echo Test 1 completed successfully!
echo.
pause

REM Multiple file upload test with metadata file
echo Test 2: Multiple file upload with metadata file
java -jar file-multipart-pojoclient/target/file-multipart-pojoclient-1.0.0.jar ^
    --files="test-multipart.txt,test-multipart2.txt" ^
    --target.url="http://localhost:8081/api/v1/multipart/upload" ^
    --metadata-file="test-metadata.json"

if %errorlevel% neq 0 (
    echo ERROR: Multiple file upload failed
    pause
    exit /b 1
)

echo.
echo Test 2 completed successfully!
echo.

echo All tests completed successfully!
echo Check the uploads folder in file-multipart-server for uploaded files.
pause
