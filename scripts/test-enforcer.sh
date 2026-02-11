#!/bin/bash
echo "🔨 COMPILATION AVEC LES JARs SLF4J"

# Vérifie que les JARs existent
if [ ! -f "lib/slf4j-api-2.0.3.jar" ] || [ ! -f "lib/slf4j-simple-2.0.3.jar" ]; then
    echo "❌ JARs SLF4J manquants dans lib/"
    exit 1
fi

# Crée les dossiers
mkdir -p target/classes

# Chemin des JARs
JARS="lib/slf4j-api-2.0.3.jar:lib/slf4j-simple-2.0.3.jar"

javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/enforcer/TestEnforcer.java
javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/enforcer/TestIntegration.java
javac -d target/test-classes -cp "target/classes:lib/*" TestJson.java

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

# Exécute

# java -cp "target/classes:target/test-classes:lib/*" \ com.wifimanager.enforcer.TestEnforcer
#java -cp "target/classes:target/test-classes:lib/*" com.wifimanager.enforcer.TestJson

java -cp "target/classes:target/test-classes:lib/*" \ com.wifimanager.enforcer.TestIntegration