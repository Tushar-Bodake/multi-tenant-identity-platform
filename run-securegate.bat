@echo off
title SecureGate Launcher
echo ================================================================
echo   SecureGate - One-Click Starter for Windows
echo ================================================================
echo.

REM ---------- 1. Check required tools ----------
echo [Step 1/5] Checking required tools...
echo.

set "MISSING=0"

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] java NOT found
    echo     Install JDK 21: https://adoptium.net/
    set "MISSING=1"
) else (
    for /f "tokens=*" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do echo [OK] java found - %%v
)

where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] mvn NOT found
    echo     Install Maven 3.9+: https://maven.apache.org/download.cgi
    set "MISSING=1"
) else (
    echo [OK] mvn found
)

where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] node NOT found
    echo     Install Node 20 LTS: https://nodejs.org/
    set "MISSING=1"
) else (
    for /f "tokens=*" %%v in ('node -v') do echo [OK] node found - %%v
)

where npm >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] npm NOT found
    echo     npm ships with Node - reinstall Node if missing
    set "MISSING=1"
) else (
    echo [OK] npm found
)

if "%MISSING%"=="1" (
    echo.
    echo Please install the missing tools above and re-run this script.
    pause
    exit /b 1
)

echo.
echo All required tools are installed!
echo.

REM ---------- 2. Prepare .env ----------
echo [Step 2/5] Checking .env file...
if not exist "%~dp0.env" (
    if exist "%~dp0.env.example" (
        copy "%~dp0.env.example" "%~dp0.env" >nul
        echo [OK] Copied .env.example to .env
    ) else (
        echo [X] .env.example not found - cannot create .env
        pause
        exit /b 1
    )
) else (
    echo [OK] .env already exists
)
echo.

REM ---------- 3. Info about optional services ----------
echo [Step 3/5] Checking optional services...
echo.
echo     NOTE: Dev mode uses H2 in-memory database and does NOT
echo           require PostgreSQL or Redis. They are optional.
echo.

netstat -an 2>nul | findstr ":5432.*LISTENING" >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [~] PostgreSQL not detected on port 5432 - OK, using H2 in-memory DB
) else (
    echo [OK] PostgreSQL is running on port 5432
)

netstat -an 2>nul | findstr ":6379.*LISTENING" >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [~] Redis not detected on port 6379 - OK, Redis is disabled in dev mode
) else (
    echo [OK] Redis is running on port 6379
)

netstat -an 2>nul | findstr ":8025.*LISTENING" >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [~] Mailpit not detected on port 8025 - emails will be logged to console
) else (
    echo [OK] Mailpit is running on port 8025
)
echo.

REM ---------- 4. Build and Start Backend ----------
echo [Step 4/5] Building and starting the backend...
echo     This may take a minute on first run...
echo.

set "SPRING_PROFILES_ACTIVE=dev"
set "SERVER_PORT=8081"

pushd "%~dp0backend"
call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [X] Maven build failed! Check the output above.
    popd
    pause
    exit /b 1
)
echo [OK] Backend built successfully
echo [>>] Starting backend on http://localhost:8081 ...
start "SecureGate Backend" mvn spring-boot:run
popd

REM Wait for backend to boot by checking if port 8081 is listening
echo     Waiting for backend to be ready...
set "RETRIES=0"
:wait_backend
ping -n 6 127.0.0.1 >nul 2>nul
set /a RETRIES+=1
netstat -an 2>nul | findstr ":8081.*LISTENING" >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    if %RETRIES% LSS 12 (
        echo     Still waiting... attempt %RETRIES% of 12
        goto wait_backend
    ) else (
        echo [!] Backend did not start within 60s - continuing anyway.
        echo     Check the "SecureGate Backend" window for errors.
    )
) else (
    echo [OK] Backend is running on port 8081!
)
echo.

REM ---------- 5. Install and Start Frontend ----------
echo [Step 5/5] Installing and starting the frontend...
pushd "%~dp0frontend"
call npm install --silent
echo [OK] Frontend dependencies installed
echo [>>] Starting frontend on http://localhost:3000 ...
start "SecureGate Frontend" npm run dev
popd

echo.
echo ================================================================
echo   SecureGate is up and running!
echo ================================================================
echo.
echo   Frontend UI          : http://localhost:3000
echo   Backend API          : http://localhost:8081/api/v1
echo   Backend Health Check : http://localhost:8081/actuator/health
echo   Swagger API Docs     : http://localhost:8081/swagger-ui.html
echo   Mailpit (if running) : http://localhost:8025
echo.
echo   Default admin login:
echo     Email    : admin@securegate.local
echo     Password : AdminPassword123!
echo.
echo   To stop: close the "SecureGate Backend" and
echo            "SecureGate Frontend" windows.
echo ================================================================
echo.
pause
