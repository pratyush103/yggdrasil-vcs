@echo off
setlocal
set SCRIPT_DIR=%~dp0
set JAR_FILE=%SCRIPT_DIR%yggdrasil\target\yggdrasil-1.0-SNAPSHOT.jar

if not exist "%JAR_FILE%" (
    echo [ygg] Building project jar...
    call "%SCRIPT_DIR%mvnw.cmd" package -DskipTests -q
)

java -jar "%JAR_FILE%" %*
