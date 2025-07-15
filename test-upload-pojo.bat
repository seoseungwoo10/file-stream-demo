@echo off
REM Start POJO File Stream Client Test Script

echo ========================================
echo Starting POJO File Stream Client Test
echo ========================================

REM 서버가 실행 중인지 확인
curl -s http://localhost:8080/actuator/health > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Server is not running. Please start the server first.
    echo Run: start-server.bat
    pause
    exit /b 1
)

echo Server is running. Starting file upload test...
echo.

REM 기본 테스트 파일이 없으면 생성
if not exist "uploads\test-large.dat" (
    echo Creating test file...
    call create-test-files.bat
)

REM Java 실행 파일 경로 확인
where java > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not found in PATH.
    pause
    exit /b 1
)

echo Testing POJO File Stream Client...
echo Target File: uploads\test-large.dat
echo Server URL: http://localhost:8080/api/v1/files/upload
echo.

REM POJO 클라이언트 실행
java -jar file-stream-pojoclient\target\file-stream-pojoclient-1.0.0.jar --file.path="uploads\test-large.dat" --target.url="http://localhost:8080/api/v1/files/upload"

if errorlevel 1 (
    echo.
    echo [ERROR] POJO client test failed!
    pause
    exit /b 1
) else (
    echo.
    echo [SUCCESS] POJO client test completed successfully!
)

echo.
echo ========================================
echo Test Results:
echo ========================================
echo - Check uploads\ directory for uploaded files
echo - Check file-stream-server\uploads\ directory for server-side files
echo ========================================

pause
