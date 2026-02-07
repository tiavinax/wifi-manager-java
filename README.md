# wifi-manager-java
Gestionnaire WiFi intelligent pour restaurants et cybercafés - Projet Réseau


# WiFi Manager for Businesses - Projet Réseau Java

## 📋 Description
Application de gestion WiFi pour restaurants, cybercafés et hôtels avec :
- Limitation temps/connexion
- Quotas de données
- Détection type trafic (YouTube, Netflix)
- Dashboard d'administration

## 👥 Équipe (5 personnes)

### Module 1 : Détection (Tiavina)
**Fichiers principaux :** `detector/`
- Détection appareils connectés (ARP scanning)
- Surveillance DHCP
- Base de données clients

### Module 2 : Contrôle (Sandria)
**Fichiers principaux :** `enforcer/`
- Application quotas temps/données
- Limitation bande passante
- Déconnexion automatique

### Module 3 : Analyse (Larissa)
**Fichiers principaux :** `inspector/`
- Deep Packet Inspection
- Détection services (YouTube, Netflix)
- Page captive portal

### Module 4 : Interface (Miangola et Stephanie)
**Fichiers principaux :** `dashboard/`
- Dashboard web admin
- Gestion règles
- Graphiques et reporting

## 🚀 Installation

### Prérequis
- Java 11+
- Maven 3.6+
- Linux (pour iptables/tc)
- droits sudo pour gestion réseau

### Installation rapide
```bash
git clone [repo-url]
cd wifi-manager-java
chmod +x scripts/setup.sh
sudo ./scripts/setup.sh



## 🔄 **Diagramme de Dépendances**

┌─────────────────────────────────────────────────────────────┐
│ DASHBOARD (Module 4) │
│ Port: 8080 - Spring Boot Web Interface │
├───────────────┬───────────────┬─────────────────┬───────────┤
│ GET /clients │ POST /rules │ GET /stats │ WebSocket│
└───────┬───────┴───────┬───────┴────────┬────────┴─────┬─────┘
│ │ │ │
▼ ▼ ▼ ▼
┌───────────────┐ ┌───────────────┐ ┌──────────────┐ ┌─────────────┐
│ DETECTOR │ │ ENFORCER │ │ INSPECTOR │ │ CLIENT │
│ (Module 1) │ │ (Module 2) │ │ (Module 3) │ │ Browser │
│ Port: 8081 │ │ Port: 8082 │ │ Port: 8083 │ │ │
├───────────────┤ ├───────────────┤ ├──────────────┤ └─────────────┘
│ • ARP Scan │ │ • Quotas │ │ • DPI │
│ • DHCP Monitor│ │ • Traffic Ctrl│ │ • Captive │
│ • Client DB │ │ • Auto-disconnect│ Portal │
└───────┬───────┘ └───────┬───────┘ └──────┬───────┘
│ │ │
└─────────────────┼─────────────────┘
│
┌─────▼─────┐
│ SYSTEM │
│ NETWORK │
│ (Linux) │
│ iptables │
│ tc │
└───────────┘

# structure du projet

wifi-manager-java/
│
├── README.md                          # Documentation principale
├── pom.xml                            # Configuration Maven
├── docker-compose.yml                 # Environnement de test
│
├── src/main/java/com/wifimanager/
│   │
│   ├── shared/                        # Modules partagés
│   │   ├── model/
│   │   │   ├── Client.java           # POJO Client
│   │   │   ├── NetworkRule.java      # POJO Rule
│   │   │   ├── TrafficStat.java      # POJO Traffic
│   │   │   └── Quota.java           # POJO Quota
│   │   │
│   │   ├── database/
│   │   │   ├── DatabaseManager.java  # Gestion SQLite
│   │   │   └── DatabaseSchema.java   # Création tables
│   │   │
│   │   ├── config/
│   │   │   ├── AppConfig.java        # Configuration
│   │   │   └── Constants.java        # Constantes réseau
│   │   │
│   │   └── api/
│   │       ├── ApiClient.java        # Client HTTP pour APIs internes
│   │       ├── ApiResponse.java      # Format réponse standard
│   │       └── ApiServer.java        # Serveur HTTP minimal (Jetty/Spark)
│   │
│   ├── detector/                      # MODULE 1 : Détection
│   │   ├── ArpScanner.java           # Scan ARP du réseau
│   │   ├── DhcpMonitor.java          # Surveillance DHCP
│   │   ├── ClientDiscoveryService.java # Service principal
│   │   └── DetectorMain.java         # Point d'entrée Module 1
│   │
│   ├── enforcer/                      # MODULE 2 : Contrôle
│   │   ├── QuotaManager.java         # Gestion quotas temps/données
│   │   ├── TrafficController.java    # iptables/TC via Java
│   │   ├── DisconnectionService.java # Déconnexion automatique
│   │   └── EnforcerMain.java         # Point d'entrée Module 2
│   │
│   ├── inspector/                     # MODULE 3 : Analyse
│   │   ├── PacketSniffer.java        # Capture paquets (JNetPcap)
│   │   ├── DpiEngine.java            # Détection YouTube/Netflix
│   │   ├── CaptivePortal.java        # Page de login
│   │   └── InspectorMain.java        # Point d'entrée Module 3
│   │
│   └── dashboard/                     # MODULE 4 : Interface
│       ├── WebServer.java            # Serveur Web (Spring Boot)
│       ├── AdminController.java      # Endpoints admin
│       ├── ClientController.java     # Endpoints clients
│       ├── templates/                # Fichiers HTML (Thymeleaf)
│       │   ├── index.html
│       │   ├── clients.html
│       │   └── rules.html
│       └── DashboardMain.java        # Point d'entrée Module 4
│
├── src/main/resources/
│   ├── application.properties         # Config Spring Boot
│   ├── log4j2.xml                    # Configuration logs
│   └── static/                       Assets statiques
│       ├── css/
│       │   └── style.css
│       └── js/
│           └── dashboard.js
│
├── scripts/                          # Scripts utilitaires
│   ├── setup.sh                      # Installation dépendances
│   ├── start-all.sh                  # Lancement tous modules
│   └── demo.sh                       # Script démo
│
├── lib/                              # Bibliothèques externes
│   ├── jnetpcap.jar                  # Capture réseau
│   └── sqlite-jdbc.jar               # Driver SQLite
│
├── docs/                             # Documentation
│   ├── API.md                        # Documentation API
│   ├── NETWORK.md                    # Explications réseau
│   └── PRESENTATION.md               # Script présentation
│
└── tests/
    ├── integration/
    │   └── IntegrationTest.java      # Tests d'intégration
    └── unit/
        ├── DetectorTest.java
        ├── EnforcerTest.java
        ├── InspectorTest.java
        └── DashboardTest.java