@echo off
@rem User Management Service
echo [!] Packaging User Management Service
cd ../users-management-service/
set "gradleErr=0"
call gradlew clean build -x test || set gradleErr=1
if "%gradleErr%" == "0" echo [+] User Management Service successfully packaged
if "%gradleErr%" == "1" echo [-] Error packaging User Management Service. Script cannot continue && goto end
:end
cd ../scripts/