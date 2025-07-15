@echo off
REM Test Multipart File Upload Script

echo ========================================
echo Testing Multipart File Upload Server
echo ========================================

REM 서버가 실행 중인지 확인
curl -s http://localhost:8081/actuator/health > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Multipart server is not running. Please start the server first.
    echo Run: start-multipart-server.bat
    pause
    exit /b 1
)

echo Multipart server is running. Starting file upload test...
echo.

REM 기본 테스트 파일이 없으면 생성
if not exist "uploads\test-100mb.dat" (
    echo Creating test file...
    call create-test-files.bat
)

REM JSON 메타데이터 생성
set METADATA={"description":"Test multipart upload","category":"test","uploadedBy":"batch-script","tags":["test","demo","multipart"],"customFields":{"source":"batch-test","priority":"high"}}

echo Testing multipart upload...
echo Target Files: uploads\test-100mb.dat
echo Server URL: http://localhost:8081/api/v1/multipart/upload
echo Metadata: %METADATA%
echo.

REM 다중 파일 업로드 테스트
curl -X POST ^
  -F "files=@uploads\test-100mb.dat" ^
  -F "metadata=%METADATA%" ^
  http://localhost:8081/api/v1/multipart/upload

if errorlevel 1 (
    echo.
    echo [ERROR] Multipart upload test failed!
    pause
    exit /b 1
) else (
    echo.
    echo [SUCCESS] Multipart upload test completed successfully!
)

echo.
echo ========================================
echo Test Results:
echo ========================================
echo - Check file-multipart-server\uploads\ directory for server-side files
echo - Server runs on port 8081 (different from streaming server)
echo ========================================

pause
