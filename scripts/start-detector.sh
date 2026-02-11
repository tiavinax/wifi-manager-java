#!/bin/bash
echo "🚀 DÉMARRAGE DU MODULE DE DÉTECTION"
echo "====================================="

# Vérifie la compilation
echo "1. Vérification compilation..."
./compile-detector.sh > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Erreur de compilation"
    exit 1
fi
echo "✅ Module compilé"

# Démarrer
echo -e "\n2. Démarrage du module..."
echo "   Interface: wlo1"
echo "   Subnet: 192.168.0.0/24"
echo "   Port API: 8081"
echo -e "\n📡 Le module va maintenant:"
echo "   - Scanner le réseau toutes les 30 secondes"
echo "   - Démarrer l'API REST sur le port 8081"
echo "   - Détecter automatiquement les nouveaux clients"
echo -e "\n🔗 Testez l'API avec:"
echo "   curl http://localhost:8081/api/clients"
echo "   curl http://localhost:8081/api/stats"
echo -e "\n🔄 Logs en direct:"
echo "====================================="

# Lancer le module
java -cp "target/classes:lib/*" \
     com.wifimanager.detector.DetectorMain \
     --interface wlo1 \
     --subnet 192.168.0.0/24 \
     --port 8081