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

echo "1. Client.java..."
javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/detector/TestModuleComplet.java
javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/detector/TestReseauReel.java
javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/detector/TestAvecSudo.java
javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/detector/TestAvecBonnesValeurs.java
javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/detector/TestFinal.java
javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/detector/TestSansSudo.java
javac -d target/classes -cp "target/classes:$JARS"  src/test/java/com/wifimanager/detector/TestUltime.java


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
java -cp "target/classes:target/test-classes:lib/*" \
     com.wifimanager.detector.TestSansSudo