@echo off
echo Creating 100MB test file for memory usage validation...

echo.
echo Creating 100MB test file...
fsutil file createnew test-100mb.dat 104857600
echo Created: test-100mb.dat (100MB)

echo.
echo Test file created successfully!
echo.
echo To test with memory constraints:
echo 1. Start server: start-server-memory-test.bat
echo 2. Upload file: test-upload-memory.bat "test-100mb.dat"
echo.
echo Both processes will have 64MB heap limit to validate 20MB usage requirement.

pause
