@echo off
echo Starting Jenkins...
docker start jenkins
timeout /t 10 /nobreak
echo Fixing Docker socket permissions...
docker exec -u root jenkins chmod 666 /var/run/docker.sock
echo Starting ngrok...
start cmd /k "cd /d %~dp0 && .\ngrok.exe http --domain=doorknob-predator-duress.ngrok-free.dev 8090"
echo.
echo Jenkins is ready at http://localhost:8090
echo ngrok is running in a separate window
pause