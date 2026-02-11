#!/bin/bash
echo "🔨 COMPILATION MODULE 4 - DASHBOARD (SPRING BOOT)"
echo "================================================="

# Crée les dossiers
mkdir -p target/classes
mkdir -p target/dependency

# Construction du CLASSPATH avec TOUTES les dépendances
CP="target/classes"

# 1. SLF4J (dans lib/)
for jar in lib/*.jar; do
    CP="$CP:$jar"
done

# 2. Spring Boot et dépendances (dans lib-spring/)
if [ -d "lib-spring" ]; then
    for jar in lib-spring/*.jar; do
        CP="$CP:$jar"
    done
    echo "📚 Spring Boot: $(ls lib-spring/*.jar | wc -l) JARs ajoutés"
fi

echo "📚 Classpath total: $(echo $CP | tr ':' '\n' | wc -l) JARs"

# Compilation dans l'ordre
echo -e "\n1. Modèles (DTO)..."
javac -cp "$CP" -d target/classes \
    src/main/java/com/wifimanager/dashboard/model/*.java

echo "2. Configuration..."
javac -cp "$CP" -d target/classes \
    src/main/java/com/wifimanager/dashboard/config/*.java

echo "3. Services..."
javac -cp "$CP" -d target/classes \
    src/main/java/com/wifimanager/dashboard/service/*.java

echo "4. Contrôleurs..."
javac -cp "$CP" -d target/classes \
    src/main/java/com/wifimanager/dashboard/controller/*.java

echo "5. Point d'entrée..."
javac -cp "$CP" -d target/classes \
    src/main/java/com/wifimanager/dashboard/DashboardMain.java

if [ $? -eq 0 ]; then
    echo -e "\n✅ MODULE 4 COMPILÉ AVEC SUCCÈS !"
    
    # Compter les classes
    CLASS_COUNT=$(find target/classes/com/wifimanager/dashboard -name "*.class" | wc -l)
    echo "📁 Classes compilées: $CLASS_COUNT"
    
    # Créer le répertoire pour les ressources
    mkdir -p target/classes/static
    mkdir -p target/classes/templates
    
    # Copier les ressources statiques
    cp -r src/main/resources/static/* target/classes/static/ 2>/dev/null || true
    cp -r src/main/resources/templates/* target/classes/templates/ 2>/dev/null || true
    cp src/main/resources/application.properties target/classes/ 2>/dev/null || true
    
    echo "📄 Ressources copiées"
    
    # Créer un script de lancement
    cat > run-dashboard.sh << 'EOF'
#!/bin/bash
echo "🚀 DÉMARRAGE DASHBOARD - SPRING BOOT"
echo "====================================="
CP="target/classes"
for jar in lib/*.jar; do CP="$CP:$jar"; done
for jar in lib-spring/*.jar; do CP="$CP:$jar"; done
java -cp "$CP" com.wifimanager.dashboard.DashboardMain
EOF
    chmod +x run-dashboard.sh
    echo "✅ Script run-dashboard.sh créé"
    
else
    echo -e "\n❌ Erreur de compilation"
    exit 1
fi