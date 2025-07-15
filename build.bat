@echo off
echo Building API Stream Demo...

echo.
echo Building parent project...
call mvn clean compile

echo.
echo Building server module...
cd file-stream-server
call mvn clean package
cd ..

echo.
echo Building client module...
cd file-stream-httpclient
call mvn clean package
cd ..

echo.
echo Build completed!
echo.
echo Server JAR: file-stream-server\target\file-stream-server-1.0.0.jar
echo Client JAR: file-stream-httpclient\target\file-stream-httpclient-1.0.0.jar
echo.
echo To start server: cd file-stream-server && java -jar target\file-stream-server-1.0.0.jar
echo To upload file: cd file-stream-httpclient && java -jar target\file-stream-httpclient-1.0.0.jar --file.path="C:\03_sources\seoseungwoo10_srcs\api-stream-demo\test-large.dat" --target.url="http://localhost:8080/api/v1/files/upload"

pause
