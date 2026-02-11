#!/bin/bash
# Ce script ne dépend d'aucun téléchargement Maven

echo "=== Lancement du Dashboard WiFi Manager (mode 100% local) ==="

# Vérifier que les classes sont compilées
if [ ! -d "target/classes" ]; then
    echo "Compilation des classes..."
    javac -d target/classes \
        -cp "lib/*:lib/*" \
        $(find src/main/java -name "*.java")
fi

# Lancer l'application
java -cp "target/classes:lib/*:lib/*" \
    -Dspring.profiles.active=offline \
    -Dserver.port=8080 \
    com.wifimanager.dashboard.DashboardMain