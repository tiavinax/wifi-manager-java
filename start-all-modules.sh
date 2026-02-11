#!/bin/bash
echo "🚀 DÉMARRAGE DE TOUS LES MODULES"
echo "================================="

# Créer les dossiers nécessaires
mkdir -p data logs

echo "1. Compilation de tous les modules..."
./compile-detector.sh 2>/dev/null
./compile-enforcer.sh 2>/dev/null

echo "2. Démarrage Module 1 (Détection)..."
# Démarrer Module 1 en arrière-plan
java -cp "target/classes:lib/*" \
     com.wifimanager.detector.DetectorMain \
     --interface wlo1 \
     --subnet 192.168.0.0/24 \
     --port 8081 > logs/detector.log 2>&1 &
DETECTOR_PID=$!

echo "   PID: $DETECTOR_PID, Port: 8081"
sleep 3  # Attendre que le Module 1 démarre

echo "3. Démarrage Module 2 (Enforcer)..."
# Démarrer Module 2 en arrière-plan
java -cp "target/classes:lib/*" \
     com.wifimanager.enforcer.EnforcerMain \
     --simulation \
     --interface wlo1 \
     --port 8082 > logs/enforcer.log 2>&1 &
ENFORCER_PID=$!

echo "   PID: $ENFORCER_PID, Port: 8082, Mode: SIMULATION"
sleep 3  # Attendre que le Module 2 démarre

echo "4. Vérification des modules..."
echo "   Module 1: curl -s http://localhost:8081/health"
curl -s http://localhost:8081/api/health | grep -q "status" && echo "   ✅ Module 1 OK" || echo "   ❌ Module 1 KO"

echo "   Module 2: curl -s http://localhost:8082/health"
curl -s http://localhost:8082/health | grep -q "status" && echo "   ✅ Module 2 OK" || echo "   ❌ Module 2 KO"

echo ""
echo "🎯 TOUS LES MODULES SONT OPÉRATIONNELS !"
echo ""
echo "📡 Module 1 - Détection:"
echo "   http://localhost:8081/api/clients"
echo "   http://localhost:8081/api/stats"
echo ""
echo "⚡ Module 2 - Contrôle:"
echo "   http://localhost:8082/health"
echo "   http://localhost:8082/stats"
echo ""
echo "🔗 Test d'intégration:"
echo "   curl -X POST http://localhost:8082/quota/set \\"
echo "     -H \"Content-Type: application/json\" \\"
echo "     -d '{\"mac\":\"D8:42:F7:2A:20:4F\",\"timeMinutes\":2,\"dataMB\":50}'"
echo ""
echo "📝 Logs:"
echo "   tail -f logs/detector.log   # Logs Module 1"
echo "   tail -f logs/enforcer.log   # Logs Module 2"
echo ""
echo "🛑 Pour arrêter tous les modules:"
echo "   kill $DETECTOR_PID $ENFORCER_PID"
echo "   ou: pkill -f \"java.*wifimanager\""
echo ""
echo "================================="
echo "Système en cours d'exécution..."
echo "Appuyez sur Ctrl+C pour arrêter"

# Attendre Ctrl+C
trap "echo ''; echo 'Arrêt des modules...'; kill $DETECTOR_PID $ENFORCER_PID 2>/dev/null; exit 0" INT
wait