@echo off
@rem User Management Service
echo [!] Imaging User Management Service
cd ../users-management-service/
set "dockerErr=0"
call docker build . --tag %1/users-management-service --force-rm=true || set dockerErr=1
if "%dockerErr%" == "0" echo [+] User Management Service successfully imaged
if "%dockerErr%" == "1" echo [-] Error imaging User Management Service. Script cannot continue && goto end
:end
cd ../scripts/