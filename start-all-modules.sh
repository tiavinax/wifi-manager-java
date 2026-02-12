#!/bin/bash
echo "🚀 DÉMARRAGE DE TOUS LES MODULES"
echo "================================="

# Créer les dossiers nécessaires
mkdir -p data logs

echo "1. Compilation de tous les modules..."
./compile-detector.sh || { echo "❌ Échec compilation Module 1"; exit 1; }
./compile-enforcer.sh || { echo "❌ Échec compilation Module 2"; exit 1; }

echo "2. Démarrage Module 1 (Détection)..."
# ✅ SOLUTION: Utiliser sudo -n pour vérifier si mot de passe requis
sudo -n true 2>/dev/null
if [ $? -ne 0 ]; then
    echo "   ⚠️  Le Module 1 a besoin de sudo pour arp-scan"
    echo "   ⚠️  Veuillez entrer votre mot de passe sudo"
    sudo -v  # Rafraîchir le ticket sudo
fi

# Démarrer Module 1 avec sudo en arrière-plan
sudo -b java -cp "target/classes:lib/*" \
     com.wifimanager.detector.DetectorMain \
     --interface wlo1 \
     --subnet 192.168.43.0/24 \
     --port 8081 > logs/detector.log 2>&1
DETECTOR_PID=$!
echo "   PID: $DETECTOR_PID, Port: 8081"
echo "   ⏳ Attente du démarrage du Module 1..."
sleep 5

echo "3. Démarrage Module 2 (Enforcer)..."
# Démarrer Module 2 en arrière-plan (pas besoin de sudo)
java -cp "target/classes:lib/*" \
     com.wifimanager.enforcer.EnforcerMain \
     --real \
     --interface wlo1 \
     --port 8082 > logs/enforcer.log 2>&1 &
ENFORCER_PID=$!
echo "   PID: $ENFORCER_PID, Port: 8082, Mode: RÉEL"
sleep 3

echo "4. Vérification des modules..."
echo "   Module 1: curl -s http://localhost:8081/api/health"
curl -s http://localhost:8081/api/health | grep -q "OK"
if [ $? -eq 0 ]; then
    echo "   ✅ Module 1 OK"
else
    echo "   ❌ Module 1 KO - Vérifie les logs: tail -f logs/detector.log"
fi

echo "   Module 2: curl -s http://localhost:8082/health"
curl -s http://localhost:8082/health | grep -q "OK"
if [ $? -eq 0 ]; then
    echo "   ✅ Module 2 OK"
else
    echo "   ❌ Module 2 KO - Vérifie les logs: tail -f logs/enforcer.log"
fi

echo ""
echo "🎯 TOUS LES MODULES SONT OPÉRATIONNELS !"
echo ""
echo "📡 Module 1 - Détection:"
echo "   ✅ http://localhost:8081/api/clients"
echo "   ✅ http://localhost:8081/api/stats"
echo "   ✅ http://localhost:8081/api/health"
echo ""
echo "⚡ Module 2 - Contrôle (MODE RÉEL):"
echo "   ✅ http://localhost:8082/health"
echo "   ✅ http://localhost:8082/stats"
echo "   ✅ http://localhost:8082/blocked"
echo "   ✅ http://localhost:8082/quota/"
echo ""
echo "🔥 MODE RÉEL ACTIVÉ - Les clients seront VRAIMENT bloqués !"
echo ""
echo "📝 Logs:"
echo "   tail -f logs/detector.log"
echo "   tail -f logs/enforcer.log"
echo ""
echo "🛑 Pour arrêter tous les modules:"
echo "   sudo kill $DETECTOR_PID"
echo "   kill $ENFORCER_PID"
echo ""
echo "================================="
echo "Système en cours d'exécution..."
echo "Appuyez sur Ctrl+C pour arrêter"

# Attendre Ctrl+C
trap "echo ''; echo 'Arrêt des modules...'; sudo kill $DETECTOR_PID 2>/dev/null; kill $ENFORCER_PID 2>/dev/null; echo '✅ Modules arrêtés'; exit 0" INT

# Boucle infinie pour maintenir le script
while true; do
    sleep 1
done