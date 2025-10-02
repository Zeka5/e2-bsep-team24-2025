@echo off
REM Extract certificate and key from localhost.p12

cd bsep_backend\src\main\resources\keystore

echo Extracting certificate from localhost.p12...
openssl pkcs12 -in localhost.p12 -clcerts -nokeys -out localhost.crt -passin pass:localhost

echo Extracting private key from localhost.p12...
openssl pkcs12 -in localhost.p12 -nocerts -nodes -out localhost.key -passin pass:localhost

echo Copying files to frontend certs folder...
xcopy /Y localhost.crt ..\..\..\..\bsep-frontend\certs\
xcopy /Y localhost.key ..\..\..\..\bsep-frontend\certs\

echo Done! Certificate and key are in bsep-frontend/certs/
cd ..\..\..\..
