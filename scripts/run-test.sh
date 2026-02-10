#!/bin/bash
# Script pour exécuter les tests manuels

echo "🔨 Compilation avec Maven..."
mvn clean compile test-compile

echo "📦 Téléchargement des dépendances..."
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q

echo "🚀 Exécution du test manuel..."
echo "========================================"
java -cp "target/classes:target/test-classes:target/dependency/*:lib/*" \
     com.wifimanager.detector.TestManual