@echo off
echo Starting File Stream Server with 64MB heap limit for memory testing...
cd file-stream-server
java -Xmx64m -Xms32m -jar target\file-stream-server-1.0.0.jar
