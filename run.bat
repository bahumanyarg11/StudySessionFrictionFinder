@echo off
setlocal
cd /d "%~dp0"

if not exist "lib\*.jar" (
    echo MySQL Connector/J not found in lib\.
    echo Download mysql-connector-j from https://dev.mysql.com/downloads/connector/j/
    echo and place the jar in the lib folder.
    exit /b 1
)

if exist out rmdir /s /q out
dir /s /b src\*.java > "%TEMP%\ssff-sources.txt"
javac -d out "@%TEMP%\ssff-sources.txt"
set "RESULT=%ERRORLEVEL%"
del "%TEMP%\ssff-sources.txt" >nul 2>&1
if not "%RESULT%"=="0" exit /b %RESULT%

java -cp "out;lib\*" com.friction.Main
