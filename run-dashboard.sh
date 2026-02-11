#!/bin/bash
echo "🚀 DÉMARRAGE DASHBOARD - SERVEUR SIMPLE"
echo "========================================"

# Compile le serveur simple
javac -d target/classes \
      src/test/java/com/wifimanager/dashboard/SimpleHttpServer.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation réussie"
    echo ""
    
    # Lance le serveur
    java -cp "target/classes" \
         com.wifimanager.dashboard.SimpleHttpServer
else
    echo "❌ Erreur de compilation"
    exit 1
fi