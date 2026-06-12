@echo off
echo Starting Nexus Repository Manager...
docker-compose -f nexus-compose.yml up -d
echo.
echo Nexus is starting. It takes ~2 minutes to be ready.
echo UI: http://localhost:8091
echo.
echo To get the initial admin password run:
echo   docker exec nexus cat /nexus-data/admin.password
