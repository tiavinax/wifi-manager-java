┌─────────────────────────────────────┐
│        DETECTOR MODULE              │
│  (Tiavina - Port 8081)              │
├─────────────┬─────────────┬─────────┤
│ ARP Scanner │ DHCP Monitor│ API     │
└──────┬──────┴──────┬──────┴────┬────┘
       │             │           │
       ▼             ▼           ▼
┌─────────────┐ ┌─────────┐ ┌─────────┐
│ Découvre    │ │ Attri-  │ │ Fournit │
│ appareils   │ │ bue IP  │ │ données │
│ (MAC)       │ │ (DHCP)  │ │ via API │
└──────┬──────┘ └────┬────┘ └────┬────┘
       └─────────────┼────────────┘
                     ▼
           ┌─────────────────┐
           │ Base de données │
           │   SQLite        │
           └─────────────────┘
                     │
           ┌─────────┴─────────┐
           ▼                   ▼
┌─────────────────┐ ┌─────────────────┐
│ Module 2        │ │ Module 4        │
│ (Contrôle)      │ │ (Dashboard)     │
│ Consulte les    │ │ Affiche les     │
│ clients pour    │ │ clients en      │
│ appliquer       │ │ temps réel      │
│ les quotas      │ │                 │
└─────────────────┘ └─────────────────┘

Diagramme de flux :

┌─────────────────────────────────┐
│      DetectorMain (main)        │ ← Démarre tout
├─────────────────────────────────┤
│ 1. Crée ClientDiscoveryService  │
│ 2. ClientDiscoveryService crée: │
│    • ArpScanner                 │
│    • DhcpMonitor                │
│ 3. Démarrer boucle découverte   │
│ 4. Démarrer serveur API         │
└───────────────┬─────────────────┘
                │
┌───────────────▼─────────────────┐
│    ClientDiscoveryService       │ ← Cœur du module
├─────────────────────────────────┤
│ • discoverClients()             │
│   ├── ArpScanner.scan()         │
│   ├── DhcpMonitor.getLeases()   │
│   └── Fusionne résultats        │
│ • Met à jour clientsCache       │
│ • Marque clients inactifs       │
└───────────────┬─────────────────┘
                │
┌───────────────▼─────────────────┐
│       DetectorApiServer         │ ← Exposition des données
├─────────────────────────────────┤
│ • GET /api/clients              │
│ • GET /api/clients/active       │
│ • GET /api/clients/{mac}        │
│ • GET /api/stats                │
└─────────────────────────────────┘