#!/bin/bash
echo "🔧 Compilation du Module Enforcer..."

# Crée le dossier si nécessaire
mkdir -p src/main/java/com/wifimanager/enforcer

# Compile
javac -d target/classes -cp "target/classes:lib/*" \
      src/main/java/com/wifimanager/enforcer/*.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation réussie"
    echo ""
    echo "🚀 Démarrage du Module Enforcer..."
    echo "================================="
    java -cp "target/classes:lib/*" \
         com.wifimanager.enforcer.EnforcerMain
else
    echo "❌ Erreur de compilation"
    exit 1
fi