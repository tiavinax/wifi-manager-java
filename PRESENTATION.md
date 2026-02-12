SLIDE DE PRÉSENTATION :

┌─────────────────────────────────────────────────────────┐
│                                                         │
│     🏠 ARCHITECTURE CIBLE : POINT D'ACCÈS               │
│                                                         │
│     ┌─────────────────────────────────────┐             │
│     │         POINT D'ACCÈS               │             │
│     │    (Routeur / Box / AP)             │             │
│     │                                     │             │
│     │  ┌───────────────────────────┐      │             │
│     │  │   NOTRE PROGRAMME         │      │             │
│     │  │   - Détection clients     │      │             │
│     │  │   - Gestion quotas        │      │             │
│     │  │   - Blocage MAC (iptables)│      │             │
│     │  └───────────────────────────┘      │             │
│     └─────────────────────────────────────┘             │
│                    │            │                       │
│        ┌───────────┴────┐ ┌────┴───────────┐            │
│        │   CLIENT 1     │ │   CLIENT 2     │            │
│        │   Connecté     │ │   Bloqué       │            │
│        │   ✓ Internet   │ │   ✗ Internet   │            │
│        └────────────────┘ └────────────────┘            │
│                                                         │
└─────────────────────────────────────────────────────────┘


Introduction :
"Notre solution est destinée aux fournisseurs d'accès WiFi : restaurants, hôtels, cybercafés, bibliothèques... Ces établissements ont tous un point d'accès WiFi (une borne, un routeur) qui distribue Internet."

Le problème :
"Aujourd'hui, ces établissements doivent gérer manuellement les connexions :
- Soit via l'interface web du routeur (long, fastidieux)
- Soit en notant les temps sur un carnet (impossible à grande échelle)"

Notre solution :
"Notre programme s'installe directement sur le point d'accès WiFi.
Il se branche sur le cœur du réseau, là où tout le trafic passe."

La démonstration :
"Pour notre démo, faute d'avoir un routeur dédié, nous avons exécuté le programme sur un PC classique.
Résultat : les commandes sont correctes, la logique est validée, le dashboard fonctionne.
En production, sur un vrai routeur, ces mêmes commandes bloqueraient immédiatement les clients."