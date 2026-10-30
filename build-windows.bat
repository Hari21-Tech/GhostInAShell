@echo off
call mvn clean package
if errorlevel 1 exit /b 1
rmdir /s /q dist input 2>nul
mkdir input
copy target\ghost.jar input\
jpackage --type app-image --name GhostInTheShell --input input --main-jar ghost.jar --main-class ghost.Launcher --dest dist
rmdir /s /q input
echo Done: dist\GhostInTheShell\GhostInTheShell.exe
