#!/bin/bash

# Création des dossiers nécessaires
mkdir -p build/classes
mkdir -p dist

# Compilation des fichiers Java
echo "Compilation des fichiers Java..."
javac -d build/classes -cp "lib/*" src/framework/*.java /home/miaritsoa/ITU/Framework/Framework/annotation/*.java

# Création du fichier JAR
echo "Création du fichier JAR..."
cd build/classes
jar cvf ../../dist/Framework.jar .
cd ../..

# Nettoyage
echo "Nettoyage des fichiers temporaires..."
rm -rf build

echo "Build terminé. Le fichier JAR se trouve dans le dossier 'dist'"