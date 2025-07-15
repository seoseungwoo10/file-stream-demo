@echo off
REM POJO Memory Test Script for File Stream Client

echo ========================================
echo POJO File Stream Client Memory Test
echo ========================================

REM 서버가 실행 중인지 확인
curl -s http://localhost:8080/actuator/health > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Server is not running. Please start the server first.
    echo Run: start-server.bat
    pause
    exit /b 1
)

echo Server is running. Starting memory test...
echo.

REM 100MB 테스트 파일이 없으면 생성
if not exist "uploads\test-100mb.dat" (
    echo Creating 100MB test file...
    call create-100mb-test.bat
)

REM Java 실행 파일 경로 확인
where java > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not found in PATH.
    pause
    exit /b 1
)

echo Testing POJO File Stream Client with 100MB file...
echo Target File: uploads\test-100mb.dat
echo Server URL: http://localhost:8080/api/v1/files/upload
echo Memory Limit: 20MB (for testing memory efficiency)
echo.

REM 메모리 제한을 두고 POJO 클라이언트 실행
java -Xmx20m -jar file-stream-pojoclient\target\file-stream-pojoclient-1.0.0.jar --file.path="uploads\test-100mb.dat" --target.url="http://localhost:8080/api/v1/files/upload"

if errorlevel 1 (
    echo.
    echo [ERROR] POJO client memory test failed!
    echo This could be due to:
    echo 1. Memory limit exceeded (20MB)
    echo 2. Server connection issues
    echo 3. File access problems
    pause
    exit /b 1
) else (
    echo.
    echo [SUCCESS] POJO client memory test completed successfully!
    echo The client successfully uploaded 100MB file with only 20MB memory limit.
)

echo.
echo ========================================
echo Memory Test Results:
echo ========================================
echo - 100MB file uploaded with 20MB memory limit
echo - Check uploads\ directory for test files
echo - Check file-stream-server\uploads\ directory for server-side files
echo ========================================

pause
