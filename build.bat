@echo off
echo Building File Stream Demo Project...
echo ====================================

echo Building root project...
call mvn clean install
if %errorlevel% neq 0 (
    echo ERROR: Root project build failed
    pause
    exit /b 1
)

echo Building file-stream-server...
cd file-stream-server
call mvn clean package
if %errorlevel% neq 0 (
    echo ERROR: file-stream-server build failed
    pause
    exit /b 1
)
cd ..

echo Building file-multipart-server...
cd file-multipart-server
call mvn clean package
if %errorlevel% neq 0 (
    echo ERROR: file-multipart-server build failed
    pause
    exit /b 1
)
cd ..

echo Building file-stream-httpclient...
cd file-stream-httpclient
call mvn clean package
if %errorlevel% neq 0 (
    echo ERROR: file-stream-httpclient build failed
    pause
    exit /b 1
)
cd ..

echo Building file-stream-pojoclient...
cd file-stream-pojoclient
call mvn clean package
if %errorlevel% neq 0 (
    echo ERROR: file-stream-pojoclient build failed
    pause
    exit /b 1
)
cd ..

echo Building file-multipart-pojoclient...
cd file-multipart-pojoclient
call mvn clean package
if %errorlevel% neq 0 (
    echo ERROR: file-multipart-pojoclient build failed
    pause
    exit /b 1
)
cd ..

echo.
echo All projects built successfully!
echo.
echo Available JARs:
echo - file-stream-server/target/file-stream-server-1.0.0.jar
echo - file-multipart-server/target/file-multipart-server-1.0.0.jar
echo - file-stream-httpclient/target/file-stream-client-1.0.0.jar
echo - file-stream-pojoclient/target/file-stream-pojoclient-1.0.0.jar
echo - file-multipart-pojoclient/target/file-multipart-pojoclient-1.0.0.jar
echo.

pause
