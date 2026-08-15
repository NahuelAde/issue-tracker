@echo off
REM Lanzador del Gestor de incidencias (ejecuta el JAR con Java 21 y abre el navegador).
cd /d "%~dp0"
title Gestor de incidencias
echo ================================================
echo   Gestor de incidencias
echo   Se abrira http://localhost:58423 en unos segundos.
echo   Cierra esta ventana para DETENER la aplicacion.
echo ================================================
if not exist "target\issue-tracker-1.2.3.jar" (
  echo.
  echo No se encuentra el JAR. Genera primero con: mvnw.cmd clean package
  echo.
  pause
  exit /b 1
)

REM Busca una instalacion de Java 21 en JAVA_HOME, PATH o los JDK de IntelliJ.
set "JAVA_EXE="
if defined JAVA_HOME call :use_java_21 "%JAVA_HOME%\bin\java.exe"
for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA_EXE call :use_java_21 "%%J"
for /d %%D in ("%USERPROFILE%\.jdks\*") do if not defined JAVA_EXE call :use_java_21 "%%~fD\bin\java.exe"
if not defined JAVA_EXE (
  echo.
  echo No se encuentra Java 21. Instala Java 21 o configura JAVA_HOME o PATH.
  echo.
  pause
  exit /b 1
)

REM El propio programa abre el navegador solo al terminar de arrancar (BrowserLauncher);
REM ya no hace falta el truco de PowerShell con espera a ciegas de antes.
"%JAVA_EXE%" -jar "target\issue-tracker-1.2.3.jar"
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo La aplicacion ha terminado con el codigo de error %EXIT_CODE%.
  echo Revisa el mensaje anterior para conocer la causa.
  echo.
  pause
)
exit /b %EXIT_CODE%

:use_java_21
if not exist "%~1" exit /b 1
"%~1" -XshowSettings:properties -version 2>&1 | "%SystemRoot%\System32\findstr.exe" /c:"java.specification.version = 21" >nul
if errorlevel 1 exit /b 1
set "JAVA_EXE=%~1"
exit /b 0
