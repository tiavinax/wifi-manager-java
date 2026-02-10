#!/bin/bash
cd ~/RESEAU-ADMIN_SYS/wifi-manager-java
echo "🔨 Compilation..."
mvn compile > /dev/null 2>&1

echo "📦 Téléchargement des dépendances..."
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q

echo "🚀 Exécution du test..."
echo "========================================"
java -cp "target/classes:target/dependency/*:lib/*" \ com.wifimanager.detector.TestSimple
