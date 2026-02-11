# Instructions pour le Module 4 - Dashboard

## 🎯 Objectif
Créer une interface web qui affiche :
1. Liste des clients connectés (depuis Module 1)
2. Quotas actifs (depuis Module 2)
3. Formulaire pour créer des quotas
4. Statistiques en temps réel

## 📦 Technologies recommandées
- Spring Boot (déjà dans pom.xml)
- Thymeleaf pour les templates HTML
- Chart.js pour les graphiques
- WebSocket pour les mises à jour temps réel

## 🔌 APIs à utiliser

### 1. Récupérer tous les clients

# STRUCTURE DES FICHIERS : 

src/main/java/com/wifimanager/dashboard/
│
├── DashboardMain.java              # Point d'entrée Spring Boot
├── WebServer.java                  # Configuration
├── config/
│   ├── WebConfig.java             # Configuration web
│   └── RestClientConfig.java      # Client HTTP
│
├── controller/
│   ├── AdminController.java       # Pages principales
│   ├── ClientController.java      # API clients
│   └── QuotaController.java       # API quotas
│
├── service/
│   ├── DetectorService.java       # Appels vers Module 1
│   ├── EnforcerService.java       # Appels vers Module 2
│   └── DashboardService.java      # Logique métier
│
├── model/
│   ├── Client.java               # DTO Client
│   ├── Quota.java               # DTO Quota
│   └── Stats.java               # DTO Statistiques
│
└── utils/
    └── MacValidator.java        # Validation MAC

src/main/resources/
│
├── application.properties        # Configuration
│
├── static/
│   ├── css/
│   │   ├── bootstrap.min.css    # Bootstrap offline
│   │   ├── bootstrap-icons.css  # Icons offline
│   │   └── dashboard.css        # Custom CSS
│   │
│   └── js/
│       ├── bootstrap.bundle.min.js
│       ├── chart.min.js        # Pour graphiques
│       └── dashboard.js        # Custom JS
│
└── templates/
    ├── layout.html            # Template principal
    ├── index.html            # Page accueil
    ├── clients.html          # Liste clients
    ├── quotas.html           # Gestion quotas
    └── fragments/           # Composants réutilisables
        ├── navbar.html
        ├── stats-cards.html
        └── quota-form.html