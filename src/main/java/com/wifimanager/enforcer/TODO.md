✅ PHASE 2 - BACKEND (Module 2 - Enforcer)


2.1 Créer BlockedClientManager.java 👤
Fichier: src/main/java/com/wifimanager/enforcer/BlockedClientManager.java

Responsabilités:

Stocker la liste des clients bloqués (Map<String, BlockedClient>)

Ajouter/retirer de la liste quand iptables est modifié

Persister les blocages (sauvegarde dans fichier JSON)

Restaurer les blocages au démarrage

2.2 Modifier TrafficController.java 🔧
Fichier: src/main/java/com/wifimanager/enforcer/TrafficController.java

Modifications:

Ajouter paramètre --real ou --mode=real dans EnforcerMain

PROTECTION: Bloquer le blocage de sa propre MAC

Ajouter getBlockedClients() pour exposer la liste

Ajouter isClientBlocked(String macAddress)

Améliorer les logs (afficher la commande AVANT exécution)

Vérifier le code retour (0 = succès)

2.3 Modifier EnforcerApiServer.java 🌐
Fichier: src/main/java/com/wifimanager/enforcer/EnforcerApiServer.java

Nouveaux endpoints:

GET /blocked - Liste des clients bloqués

POST /unblock/{mac} - Débloquer un client

GET /blocked/count - Nombre de clients bloqués

Endpoints existants à modifier:

POST /disconnect/{mac} - Ajouter à la liste des bloqués

POST /reconnect/{mac} - Retirer de la liste des bloqués

✅ PHASE 3 - COMMUNICATION (Module Bridge)
3.1 Modifier ModuleBridge.java 🔌
Fichier: src/main/java/com/wifimanager/shared/communication/ModuleBridge.java

Nouvelles méthodes:

getBlockedClientsFromEnforcer() - Récupérer liste

unblockClientViaEnforcer(String macAddress) - Débloquer

getBlockedCount() - Nombre de bloqués

✅ PHASE 4 - FRONTEND (Module 4 - Dashboard)
4.1 Nouvelle page HTML 📄
Fichier: src/main/resources/templates/blocked.html

Éléments:

Tableau des clients bloqués (MAC, IP, date blocage, raison)

Bouton "Débloquer" pour chaque client

Bouton "Débloquer tout"

Statistiques (total bloqués, dépassés aujourd'hui)

4.2 Nouveau JavaScript 📜
Fichier: src/main/resources/static/js/blocked.js

Fonctions:

loadBlockedClients() - GET /api/enforcer/blocked

unblockClient(mac) - POST /api/enforcer/unblock/{mac}

unblockAll() - POST /api/enforcer/rules/reset

updateBlockedBadge() - Mettre à jour le compteur

4.3 Modifier layout.html 🎨
Fichier: src/main/resources/templates/layout.html

Ajouts:

Nouvel item dans sidebar: "🚫 Clients bloqués"

Badge avec le nombre de clients bloqués

Lien vers /blocked

html
<li class="nav-item" id="nav-blocked">
    <a class="nav-link" href="/blocked">
        <i class="bi bi-shield-lock"></i>
        <span>Clients bloqués</span>
        <span class="nav-badge" id="blockedCount">0</span>
    </a>
</li>
4.4 Modifier dashboard.js 🔄
Fichier: src/main/resources/static/js/dashboard.js

Ajouts:

updateBlockedCount() - Appel périodique

Intégrer dans le rafraîchissement automatique

✅ PHASE 5 - TESTS (Validation)
5.1 Tests manuels 🧪
Scénario 1 - Protection anti-blocage soi-même

bash
# 1. Démarrer en mode RÉEL
# 2. Essayer de bloquer sa propre MAC
# 3. ❌ DOIT échouer avec message d'erreur
Scénario 2 - Blocage réel

bash
# 1. Créer quota pour un téléphone
# 2. Attendre dépassement
# 3. ✅ Vérifier iptables -L FORWARD
# 4. ✅ Tester ping depuis téléphone → ÉCHEC
Scénario 3 - Déblocage

bash
# 1. Cliquer "Débloquer" dans dashboard
# 2. ✅ Vérifier iptables -L FORWARD (règle supprimée)
# 3. ✅ Tester ping depuis téléphone → OK
Scénario 4 - Persistance

bash
# 1. Bloquer un client
# 2. Redémarrer Module 2
# 3. ✅ Le client doit encore être bloqué
✅ PHASE 6 - PRÉSENTATION (Démo)
6.1 Script de démonstration 🎬
Fichier: scripts/demo-blocage-reel.sh

bash
#!/bin/bash
echo "🎬 DÉMO BLOCAGE RÉEL"
echo "===================="

# 1. Montrer état initial
echo "1️⃣ AVANT: Client connecté"
ping -c 2 192.168.43.71

# 2. Déclencher blocage via dashboard
echo "2️⃣ BLOCAGE: Quota dépassé"
echo "   → iptables -A FORWARD -m mac --mac-source 80:A5:89:D5:8C:89 -j DROP"

# 3. Montrer état après blocage
echo "3️⃣ APRÈS: Client bloqué"
ping -c 2 192.168.43.71

# 4. Montrer iptables
echo "4️⃣ VÉRIFICATION:"
sudo iptables -L FORWARD -n -v | grep -E "80:A5:89:D5:8C:89|DROP"

# 5. Débloquer
echo "5️⃣ DÉBLOCAGE:"
curl -X POST http://localhost:8082/unblock/80:A5:89:D5:8C:89
6.2 Slides de présentation 📊
Slide: "Problème: Comment empêcher l'accès Internet ?"

Slide: "Solution: iptables FORWARD DROP"

Slide: "Protection: Ne pas se bloquer soi-même"

Slide: "Démonstration en direct"

Slide: "Résultats et métriques"

📋 CHECKLIST RÉCAPITULATIVE
BACKEND (Module 2)
BlockedClientManager.java - Nouvelle classe

TrafficController.java - Mode RÉEL + protection

EnforcerApiServer.java - Endpoints /blocked et /unblock

Persistance (sauvegarde/restauration)

FRONTEND (Module 4)
blocked.html - Nouvelle page

blocked.js - Nouveau script

layout.html - Sidebar mis à jour

dashboard.js - Badge compteur

TESTS
Protection anti-blocage soi-même

Blocage réel (ping échoue)

Déblocage (ping réussit)

Persistance après redémarrage

Interface réactive

🚀 ORDRE D'EXÉCUTION RECOMMANDÉ
text
JOUR 1 : PHASE 1 + 2.1 + 2.2
      → Config sudo, créer BlockedClientManager, modifier TrafficController

JOUR 2 : PHASE 2.3 + 3
      → Ajouter endpoints API, modifier ModuleBridge

JOUR 3 : PHASE 4
      → Créer page HTML, JavaScript, intégrer sidebar

JOUR 4 : PHASE 5 + 6
      → Tests, debug, préparation démo
⚠️ POINTS CRITIQUES À NE PAS OUBLIER
🛡️ PROTECTION: if (macAddress.equals(myMac)) return false;

💾 PERSISTANCE: Sauvegarder les blocages dans quotas.json

📝 LOGS: Ajouter logger.info("🔥 MODE RÉEL ACTIVÉ") bien visible

✅ VÉRIFICATION: Toujours vérifier exitCode == 0

🔄 RESTAURATION: Re-bloquer les clients au démarrage