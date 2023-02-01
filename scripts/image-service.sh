#!/bin/bash

echo [!] Imaging User Management Service
cd ../users-management-service/
dockerErr=0
docker build . --tag $1/users-management-service --force-rm=true || dockerErr=1
if [ $dockerErr == 0 ]
then
    echo [+] User Management Service successfully imaged
fi
if [ $dockerErr == 1 ]
then
    echo [-] Error imaging User Management Service. Script cannot continue
    cd ../scripts/
    exit
fi

cd ../scripts/