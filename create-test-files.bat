@echo off
echo Creating test files for upload testing...

echo.
echo Creating small test file (1KB)...
echo This is a test file content for API streaming demo. > test-small.txt
echo Created: test-small.txt

echo.
echo Creating medium test file (1MB)...
fsutil file createnew test-medium.dat 1048576
echo Created: test-medium.dat (1MB)

echo.
echo Creating large test file (10MB)...
fsutil file createnew test-large.dat 10485760
echo Created: test-large.dat (10MB)

echo.
echo Creating very large test file (100MB)...
fsutil file createnew test-verylarge.dat 104857600
echo Created: test-verylarge.dat (100MB)

echo.
echo Test files created successfully!
echo.
echo You can now test with:
echo test-upload.bat "test-small.txt"
echo test-upload.bat "test-medium.dat"
echo test-upload.bat "test-large.dat"
echo test-upload.bat "test-verylarge.dat"

pause
