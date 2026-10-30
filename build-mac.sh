#!/bin/bash
set -e
mvn clean package
rm -rf dist input
mkdir input
cp target/ghost.jar input/
jpackage --type app-image --name GhostInTheShell --input input --main-jar ghost.jar --main-class ghost.Launcher --dest dist
jpackage --type dmg --name GhostInTheShell --input input --main-jar ghost.jar --main-class ghost.Launcher --dest dist
rm -rf input
echo "Done: dist/GhostInTheShell.app and dist/GhostInTheShell-1.0.dmg"
