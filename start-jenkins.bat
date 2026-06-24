@echo off
echo Stopping Mercato to free RAM...
cd /d C:\Users\AliHa\Mercato\mercato
docker-compose stop

cd /d C:\Users\AliHa\Mercato
echo Starting Nexus...
docker-compose -f nexus-compose.yml up -d

echo Starting Jenkins...
docker start jenkins
timeout /t 10 /nobreak
echo Fixing Docker socket permissions...
docker exec -u root jenkins sh -c "chmod 666 /var/run/docker.sock"
echo Starting ngrok...
start cmd /k "cd /d %~dp0 && .\ngrok.exe http --domain=doorknob-predator-duress.ngrok-free.dev 8090"
echo.
echo Jenkins is ready at http://localhost:8090
echo Nexus is ready at http://localhost:8091 (wait 2 mins)
pause