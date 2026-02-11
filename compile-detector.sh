#!/bin/bash
echo "🔨 COMPILATION MODULE 1 - DETECTOR AVEC LES JARs SLF4J"
echo "=================================="

# Vérifie que les JARs existent
if [ ! -f "lib/slf4j-api-2.0.3.jar" ] || [ ! -f "lib/slf4j-simple-2.0.3.jar" ]; then
    echo "❌ JARs SLF4J manquants dans lib/"
    exit 1
fi

# Crée les dossiers
mkdir -p target/classes

# Chemin des JARs
JARS="lib/slf4j-api-2.0.3.jar:lib/slf4j-simple-2.0.3.jar"

echo "1. Client.java..."
javac -d target/classes src/main/java/com/wifimanager/shared/model/Client.java

echo "2. ModuleBridge.java..."
javac -d target/classes src/main/java/com/wifimanager/shared/communication/ModuleBridge.java

echo "2. Constants.java..."
javac -d target/classes -cp "target/classes" src/main/java/com/wifimanager/shared/config/Constants.java

echo "3. ApiResponse.java..."
javac -d target/classes -cp "target/classes" src/main/java/com/wifimanager/shared/api/ApiResponse.java

echo "4. ArpScanner.java..."
javac -d target/classes -cp "target/classes:$JARS" src/main/java/com/wifimanager/detector/ArpScanner.java

echo "5. DhcpMonitor.java..."
javac -d target/classes -cp "target/classes:$JARS" src/main/java/com/wifimanager/detector/DhcpMonitor.java

echo "6. ClientDiscoveryService.java..."
javac -d target/classes -cp "target/classes:$JARS" src/main/java/com/wifimanager/detector/ClientDiscoveryService.java

echo "7. DetectorApiServer.java..."
javac -d target/classes -cp "target/classes" src/main/java/com/wifimanager/detector/DetectorApiServer.java

echo "8. DetectorMain.java..."
javac -d target/classes -cp "target/classes:$JARS" src/main/java/com/wifimanager/detector/DetectorMain.java

# Vérifie
if [ $? -eq 0 ]; then
    echo "✅ COMPILATION RÉUSSIE !"
    echo "📁 Classes dans target/classes/:"
    find target/classes -name "*.class" | wc -l
    echo " classes compilées"
else
    echo "❌ Erreur de compilation"
    exit 1
fi