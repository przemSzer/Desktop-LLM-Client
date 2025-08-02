@echo off
setlocal enabledelayedexpansion

REM Set the user home directory with proper quoting
set "USER_HOME=%USERPROFILE%"

REM Create a temporary directory for Maven if it doesn't exist
set "MAVEN_HOME=%USER_HOME%\.m2\wrapper\dists\apache-maven-3.9.11\bin"
set "MAVEN_CMD=%MAVEN_HOME%\mvn.cmd"

REM Check if Maven is already downloaded
if exist "%MAVEN_CMD%" (
    echo Found existing Maven installation
    "%MAVEN_CMD%" %*
    exit /b %errorlevel%
)

REM Download Maven if not present
echo Downloading Maven...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient = New-Object System.Net.WebClient; $webclient.DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip', '%TEMP%\maven.zip')}"

REM Extract Maven
echo Extracting Maven...
powershell -Command "& {Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%USER_HOME%\.m2\wrapper\dists' -Force}"

REM Rename the extracted directory
if exist "%USER_HOME%\.m2\wrapper\dists\apache-maven-3.9.11" (
    echo Maven downloaded successfully
    "%MAVEN_CMD%" %*
    exit /b %errorlevel%
) else (
    echo Failed to download Maven
    exit /b 1
) 