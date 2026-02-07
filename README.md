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

### Module 1 : Détection (Personne 1)
**Fichiers principaux :** `detector/`
- Détection appareils connectés (ARP scanning)
- Surveillance DHCP
- Base de données clients

### Module 2 : Contrôle (Personne 2)
**Fichiers principaux :** `enforcer/`
- Application quotas temps/données
- Limitation bande passante
- Déconnexion automatique

### Module 3 : Analyse (Personne 3)
**Fichiers principaux :** `inspector/`
- Deep Packet Inspection
- Détection services (YouTube, Netflix)
- Page captive portal

### Module 4 : Interface (Personne 4-5)
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

