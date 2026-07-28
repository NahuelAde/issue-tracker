@echo off
REM Lanzador del Gestor de incidencias (ejecuta el JAR con JDK 21 y abre el navegador).
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
title Gestor de incidencias
echo ================================================
echo   Gestor de incidencias
echo   Se abrira http://localhost:5555 en unos segundos.
echo   Cierra esta ventana para DETENER la aplicacion.
echo ================================================
if not exist "target\issue-tracker-1.1.0.jar" (
  echo.
  echo No se encuentra el JAR. Genera primero con: mvnw.cmd clean package
  echo.
  pause
  exit /b 1
)
start "" powershell -NoProfile -WindowStyle Hidden -Command "Start-Sleep -Seconds 10; Start-Process 'http://localhost:5555'"
"%JAVA_HOME%\bin\java.exe" -jar "target\issue-tracker-1.1.0.jar"
