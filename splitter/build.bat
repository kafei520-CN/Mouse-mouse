@echo off
cd /d "%~dp0"

call "D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" 2>nul
if errorlevel 1 call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" 2>nul
if errorlevel 1 call "C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat" 2>nul
if errorlevel 1 call "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat" 2>nul
if errorlevel 1 (
    echo ERROR: VS2022 not found
    pause & exit /b 1
)

if not exist build mkdir build
cd build

cmake .. -G "NMake Makefiles" -DCMAKE_BUILD_TYPE=Release
if errorlevel 1 (echo cmake failed & pause & exit /b 1)

nmake
if errorlevel 1 (echo build failed & pause & exit /b 1)

set "TARGET_DIR=%~dp0..\src\main\resources\assets\mouse"
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"
copy /y splitter.exe "%TARGET_DIR%\splitter.exe"
echo OK
pause
