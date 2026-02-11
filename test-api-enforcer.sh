#!/bin/bash
echo "🌐 TEST DE L'API ENFORCER"
echo "=========================="

echo "1. Démarrage du module en arrière-plan..."
java -cp "target/classes:lib/*" \
     com.wifimanager.enforcer.EnforcerMain \
     --simulation \
     --interface wlo1 \
     --port 8082 \
     > enforcer.log 2>&1 &
ENFORCER_PID=$!

echo "   PID: $ENFORCER_PID"
echo "   Attente démarrage..."
sleep 3

echo -e "\n2. Test des endpoints API:"

echo -e "\na) Santé du service:"
curl -s http://localhost:8082/health | python3 -m json.tool

echo -e "\nb) Définir un quota:"
curl -s -X POST http://localhost:8082/quota/set \
  -H "Content-Type: application/json" \
  -d '{"mac":"11:22:33:44:55:66","timeMinutes":3,"dataMB":50}' | \
  python3 -m json.tool

echo -e "\nc) Consulter le quota:"
curl -s http://localhost:8082/quota/11:22:33:44:55:66 | \
  python3 -m json.tool

echo -e "\nd) Simuler consommation:"
curl -s -X POST http://localhost:8082/quota/consume \
  -H "Content-Type: application/json" \
  -d '{"mac":"11:22:33:44:55:66","timeMinutes":2,"dataMB":30}' | \
  python3 -m json.tool

echo -e "\ne) Vérifier si dépassé:"
curl -s http://localhost:8082/quota/11:22:33:44:55:66 | \
  python3 -c "
import json,sys
data=json.load(sys.stdin)
print('Temps restant:', data.get('timeRemainingMinutes', 'N/A'), 'min')
print('Données restantes:', data.get('dataRemainingMB', 'N/A'), 'MB')
print('Dépassé:', data.get('isExceeded', 'N/A'))
"

echo -e "\nf) Déconnecter manuellement:"
curl -s -X POST http://localhost:8082/disconnect/11:22:33:44:55:66 | \
  python3 -m json.tool

echo -e "\ng) Statistiques:"
curl -s http://localhost:8082/stats | python3 -m json.tool

echo -e "\nh) Afficher tous les quotas:"
curl -s http://localhost:8082/quota/ | python3 -m json.tool

echo -e "\n3. Arrêt du module..."
kill $ENFORCER_PID 2>/dev/null
wait $ENFORCER_PID 2>/dev/null

echo -e "\n📊 Logs du module:"
echo "----------------"
tail -20 enforcer.log

echo -e "\n✅ Test API terminé !"