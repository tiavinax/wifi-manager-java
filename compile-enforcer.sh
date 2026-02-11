#!/bin/bash
echo "🔨 COMPILATION MODULE 2 - ENFORCER"
echo "=================================="

# Crée les dossiers
mkdir -p target/classes
mkdir -p data

# Chemin des JARs
JARS="lib/slf4j-api-2.0.3.jar:lib/slf4j-simple-2.0.3.jar"

echo "1. Quota.java..."
javac -d target/classes src/main/java/com/wifimanager/shared/model/Quota.java

echo "2. QuotaManager.java..."
javac -d target/classes -cp "target/classes:$JARS" \
      src/main/java/com/wifimanager/enforcer/QuotaManager.java

echo "3. TrafficController.java..."
javac -d target/classes -cp "target/classes:$JARS" \
      src/main/java/com/wifimanager/enforcer/TrafficController.java

echo "4. DisconnectionService.java..."
javac -d target/classes -cp "target/classes:$JARS" \
      src/main/java/com/wifimanager/enforcer/DisconnectionService.java

echo "5. EnforcerApiServer.java..."
javac -d target/classes -cp "target/classes:$JARS" \
      src/main/java/com/wifimanager/enforcer/EnforcerApiServer.java

echo "6. EnforcerMain.java..."
javac -d target/classes -cp "target/classes:$JARS" \
      src/main/java/com/wifimanager/enforcer/EnforcerMain.java

if [ $? -eq 0 ]; then
    echo "✅ MODULE 2 COMPILÉ AVEC SUCCÈS !"
    echo "📁 Classes:"
    find target/classes/com/wifimanager/enforcer -name "*.class" | wc -l
    echo " classes compilées"
else
    echo "❌ Erreur de compilation"
    exit 1
fi