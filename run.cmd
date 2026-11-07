@echo off
rem Builds the project (skipping tests) and starts the web app in one step.
rem Usage: run      then open http://localhost:8080   (set PORT to use another port)
setlocal
pushd "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 (
    echo Error: Maven ^(mvn^) was not found on PATH. Install Maven or open a new terminal. 1>&2
    popd
    exit /b 1
)

rem Build output is shown only when the build fails.
set "BUILD_LOG=%TEMP%\floodalert-build.log"
call mvn -q -B -DskipTests clean package > "%BUILD_LOG%" 2>&1
if errorlevel 1 (
    type "%BUILD_LOG%" 1>&2
    echo Error: build failed 1>&2
    popd
    exit /b 1
)

java -jar target\app.jar
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%
