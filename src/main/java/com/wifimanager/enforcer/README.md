# Module 2 : Enforcer (Contrôle de quotas)

Fonctionnalités :

Application de règles (temps/données)

Déconnexion automatique quand quota dépassé

Limitation bande passante

API pour communiquer avec Module 1 et 4

# Structure :

enforcer/
├── QuotaManager.java          # Gestion quotas temps/données
├── TrafficController.java     # iptables/TC pour limiter trafic
├── DisconnectionService.java  # Déconnexion automatique
├── EnforcerApiServer.java     # API pour dashboard
└── EnforcerMain.java          # Point d'entrée

## 🎯 **Utilité du Module 2**

Le **Module 2 (Enforcer)** est le **cœur de la gestion WiFi**. Il applique les règles définies 
par l'administrateur pour contrôler l'accès Internet des clients.

### Responsabilités principales :
1. **Gestion des quotas** - Limiter le temps de connexion et la consommation de données
2. **Contrôle du trafic** - Limiter la bande passante par client (upload/download)
3. **Déconnexion automatique** - Bloquer les clients qui dépassent leurs quotas
4. **Communication inter-modules** - Recevoir les règles du Dashboard (Module 4) et notifier les autres modules

### Pourquoi c'est critique ?
- Sans ce module, n'importe quel client pourrait utiliser le WiFi sans limites
- C'est ce qui transforme un point d'accès WiFi simple en solution de gestion professionnelle
- C'est le module qui génère des revenus pour le restaurant/cybercafé

---

## ✅ **Checklist des tâches**

### ⚙️ **1. Design de l'API REST (Module 2)** 
**Statut**: ✅ Terminé  
**Importance**: 🔴 CRITIQUE

#### Pourquoi c'est important :
- Définit le **contrat** entre Module 2 et les autres modules
- Permet de tester le module **indépendamment** avant l'intégration
- Facilite le débogage avec des endpoints clairs

#### Endpoints définis :
```
Port: 8082 (http://localhost:8082)

GET  /health              → Vérifier si le module fonctionne
POST /quota/set           → Définir quota pour un client (JSON: {mac, timeMinutes, dataMB})
GET  /quota/{mac}         → Consulter quota actuel d'un client
POST /quota/consume       → Simuler consommation (JSON: {mac, timeMinutes, dataMB})
POST /disconnect/{mac}    → Déconnecter un client manuellement
GET  /stats               → Statistiques globales (nombre clients, quotas actifs)
```

---

### 📊 **2. QuotaManager.java - Gestion des quotas**
**Statut**: ✅ Terminé  
**Importance**: 🔴 CRITIQUE

#### Pourquoi c'est important :
- C'est le **cerveau** du module - il suit la consommation de chaque client
- Doit être **fiable** : une erreur ici = perte de revenus ou clients mécontents
- Doit persister les données pour survivre aux redémarrages

#### Fonctionnalités requises :
```java
class QuotaManager {
    // Définir un nouveau quota pour un client
    void setQuota(String macAddress, int timeMinutes, int dataMB)
    
    // Récupérer le quota actuel
    Quota getQuota(String macAddress)
    
    // Consommer du temps (appelé toutes les minutes)
    boolean consumeTime(String macAddress, int minutes)
    
    // Consommer des données (appelé quand des paquets passent)
    boolean consumeData(String macAddress, int megabytes)
    
    // Vérifier si un client a dépassé ses limites
    boolean isExceeded(String macAddress)
    
    // Sauvegarder les quotas sur disque (fichier JSON)
    void persist()
    
    // Charger les quotas depuis le disque
    void load()
}
```

#### Détails techniques :
- Stockage en mémoire : `Map<String, Quota>` (MAC → Quota)
- Persistence : fichier JSON `quotas.json` dans `target/data/`
- Thread séparé pour sauvegarder automatiquement toutes les 10 secondes
- Gestion des cas limites : nouveau client, quota expiré, données négatives

---

### 🚦 **3. TrafficController.java - Contrôle réseau**
**Statut**: ✅ Terminé  
**Importance**: 🟠 HAUTE

#### Pourquoi c'est important :
- Interface avec le système Linux (iptables/tc) pour **vraiment** bloquer les clients
- En mode développement, simule les commandes pour tester sans droits root
- Permet la limitation de bande passante (ex: 1 Mbps pour les gratuits, 10 Mbps pour les payants)

#### Fonctionnalités requises :
```java
class TrafficController {
    // Mode simulation (développement) ou réel (production)
    TrafficController(boolean simulationMode)
    
    // Bloquer complètement un client
    void blockClient(String macAddress, String ipAddress)
    
    // Débloquer un client
    void unblockClient(String macAddress, String ipAddress)
    
    // Limiter la bande passante d'un client
    void limitBandwidth(String macAddress, String ipAddress, int kbps)
    
    // Supprimer la limitation de bande passante
    void removeBandwidthLimit(String macAddress, String ipAddress)
}
```

#### Détails techniques :
- **Mode simulation** : affiche les commandes dans les logs sans les exécuter
  ```
  [SIMULATION] iptables -A FORWARD -m mac --mac-source AA:BB:CC:DD:EE:FF -j DROP
  ```
- **Mode réel** : exécute via `Runtime.getRuntime().exec()`
  ```bash
  sudo iptables -A FORWARD -m mac --mac-source <MAC> -j DROP
  sudo tc qdisc add dev wlan0 root tbf rate <RATE>kbit burst 32kbit latency 400ms
  ```
- Gestion des erreurs : si iptables échoue, logger et notifier l'admin

---

### 🔌 **4. DisconnectionService.java - Déconnexion automatique**
**Statut**: ⏳ À faire  
**Importance**: 🟠 HAUTE

#### Pourquoi c'est important :
- Automatise l'application des quotas sans intervention humaine
- Vérifie périodiquement (toutes les 30 secondes) si des clients ont dépassé
- Enregistre les déconnexions pour statistiques et facturation

#### Fonctionnalités requises :
```java
class DisconnectionService {
    DisconnectionService(QuotaManager quotaManager, TrafficController controller)
    
    // Lancer la vérification automatique (thread en arrière-plan)
    void startMonitoring()
    
    // Vérifier tous les clients et déconnecter ceux qui dépassent
    void checkAndDisconnect()
    
    // Déconnecter manuellement un client
    void disconnectClient(String macAddress, String reason)
    
    // Obtenir l'historique des déconnexions
    List<DisconnectionEvent> getDisconnectionHistory()
}
```

#### Détails techniques :
- Thread qui tourne en boucle : `while(true) { checkAndDisconnect(); sleep(30000); }`
- Pour chaque client actif :
  1. Demander à `QuotaManager` si quota dépassé
  2. Si oui → appeler `TrafficController.blockClient()`
  3. Enregistrer l'événement : `{mac, timestamp, reason: "QUOTA_TIME" ou "QUOTA_DATA"}`
- Persister l'historique dans `disconnections.json`

---

### 🌐 **5. EnforcerMain.java - Serveur HTTP**
**Statut**: ⏳ À faire  
**Importance**: 🔴 CRITIQUE

#### Pourquoi c'est important :
- Point d'entrée du module - sans lui, rien ne démarre
- Expose l'API REST sur le port 8082
- Permet de tester le module **indépendamment** avec curl/Postman

#### Fonctionnalités requises :
```java
class EnforcerMain {
    public static void main(String[] args) {
        // 1. Initialiser les composants
        QuotaManager quotaManager = new QuotaManager();
        TrafficController controller = new TrafficController(true); // mode simulation
        DisconnectionService disconnectionService = new DisconnectionService(...);
        
        // 2. Démarrer le serveur HTTP (port 8082)
        HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);
        
        // 3. Enregistrer les routes
        server.createContext("/health", new HealthHandler());
        server.createContext("/quota/set", new SetQuotaHandler(quotaManager));
        server.createContext("/quota/", new GetQuotaHandler(quotaManager));
        server.createContext("/disconnect/", new DisconnectHandler(disconnectionService));
        
        // 4. Démarrer
        server.start();
        System.out.println("✅ Module 2 (Enforcer) démarré sur http://localhost:8082");
    }
}
```

#### Tests manuels avec curl :
```bash
# Tester si le module répond
curl http://localhost:8082/health

# Créer un quota (5 minutes, 100 MB)
curl -X POST http://localhost:8082/quota/set \
  -H "Content-Type: application/json" \
  -d '{"mac":"AA:BB:CC:DD:EE:FF", "timeMinutes":5, "dataMB":100}'

# Consulter le quota
curl http://localhost:8082/quota/AA:BB:CC:DD:EE:FF

# Simuler consommation (2 minutes, 50 MB)
curl -X POST http://localhost:8082/quota/consume \
  -H "Content-Type: application/json" \
  -d '{"mac":"AA:BB:CC:DD:EE:FF", "timeMinutes":2, "dataMB":50}'

# Déconnecter manuellement
curl -X POST http://localhost:8082/disconnect/AA:BB:CC:DD:EE:FF
```

---

### 🧪 **6. Tests unitaires**
**Statut**: ⏳ À faire  
**Importance**: 🟡 MOYENNE

#### Pourquoi c'est important :
- Garantit que le code fonctionne **avant** l'intégration avec les autres modules
- Facilite les modifications futures sans casser ce qui marche
- Démontre la qualité du code lors de la présentation

#### Tests à créer :
```
src/test/java/com/wifimanager/enforcer/
├── QuotaManagerTest.java
│   ├── testSetQuota()
│   ├── testConsumeTime()
│   ├── testConsumeData()
│   ├── testIsExceeded()
│   └── testPersistence()
│
├── TrafficControllerTest.java
│   ├── testBlockClient_SimulationMode()
│   ├── testUnblockClient_SimulationMode()
│   └── testLimitBandwidth_SimulationMode()
│
└── DisconnectionServiceTest.java
    ├── testCheckAndDisconnect_QuotaExceeded()
    ├── testCheckAndDisconnect_QuotaNotExceeded()
    └── testDisconnectionHistory()
```

---

## 🚀 **Ordre de développement recommandé**

1. **QuotaManager** (2-3 heures)
   - Classe la plus importante, base de tout le reste
   - Tester manuellement avec `main()` temporaire

2. **EnforcerMain + API REST** (1-2 heures)
   - Permet de tester `QuotaManager` via HTTP
   - Facilite le debug avec curl

3. **TrafficController** (1 heure)
   - Mode simulation d'abord (facile)
   - Mode réel plus tard si nécessaire

4. **DisconnectionService** (1 heure)
   - Combine `QuotaManager` + `TrafficController`
   - Tester avec des quotas très courts (10 secondes)

5. **Tests unitaires** (1-2 heures)
   - Une fois que tout fonctionne manuellement

---

## 📝 **Notes pour l'intégration future**

### Communication avec Module 4 (Dashboard)
Le Dashboard enverra des requêtes POST pour créer/modifier des quotas :
```java
// Le Dashboard appellera :
POST http://localhost:8082/quota/set
{
  "mac": "AA:BB:CC:DD:EE:FF",
  "timeMinutes": 30,
  "dataMB": 500
}
```

### Communication avec Module 1 (Detector)
Le Detector enverra l'IP associée à chaque MAC :
```java
// Format à prévoir pour v2 :
POST http://localhost:8082/client/register
{
  "mac": "AA:BB:CC:DD:EE:FF",
  "ip": "192.168.1.105"
}
```

### Communication avec Module 3 (Inspector)
L'Inspector notifiera la consommation de données :
```java
// Format à prévoir pour v2 :
POST http://localhost:8082/quota/consume
{
  "mac": "AA:BB:CC:DD:EE:FF",
  "dataMB": 15
}
```

---

## 🎓 **Concepts réseau utilisés**

### iptables (Filtrage de paquets)
- Firewall Linux natif
- Règles par MAC address ou IP
- Action DROP = bloquer complètement

### tc (Traffic Control)
- Limitation de bande passante
- QoS (Quality of Service)
- Algorithme TBF (Token Bucket Filter)

### Quotas
- Temps : tracking par intervalle de 1 minute
- Données : tracking par paquet IP (taille en octets)

---

**Date de création** : 8 février 2026  
**Responsable** : Sandria (Module 2 - Contrôle)
