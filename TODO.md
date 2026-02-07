## 🎯 **Tâches par Module (Checklist)** 

# Technologie : SPRING-BOOT

### **Module 1 - Détection** (tiavina)
- [ ] `ArpScanner.java` - Scan réseau toutes les 30s
- [ ] `DhcpMonitor.java` - Écoute paquets DHCP
- [ ] `ClientDiscoveryService.java` - Service principal
- [ ] API REST sur port 8081
- [ ] Base SQLite `clients` table
- [ ] Tests unitaires

### **Module 2 - Contrôle** (Sandria)  
- [ ] `QuotaManager.java` - Suivi quotas temps/données
- [ ] `TrafficController.java` - Interface avec iptables/tc
- [ ] `DisconnectionService.java` - Déconnexion auto
- [ ] API REST sur port 8082
- [ ] Écoute règles depuis Module 4
- [ ] Tests avec scénarios réels

### **Module 3 - Analyse** (Larissa)
- [ ] `PacketSniffer.java` - Capture paquets JNetPcap
- [ ] `DpiEngine.java` - Détection patterns YouTube/Netflix
- [ ] `CaptivePortal.java` - Page login / rechargement
- [ ] API REST sur port 8083
- [ ] Envoi stats vers Modules 1 & 4
- [ ] Tests avec captures réelles

### **Module 4 - Dashboard** (Miangola et Stephanie)
- [ ] `WebServer.java` - Spring Boot app
- [ ] `AdminController.java` - Endpoints admin
- [ ] Templates HTML (Thymeleaf)
- [ ] Interface drag-drop pour règles
- [ ] Graphiques temps réel (Chart.js)
- [ ] WebSocket pour updates live

### **Partagé** (Tous)
- [ ] `pom.xml` - Dépendances Maven
- [ ] `DatabaseManager.java` - Accès DB unique
- [ ] `ApiClient.java` - Communication interne
- [ ] Tests d'intégration
- [ ] Scripts déploiement


Cette structure garantit que :
1. **Chaque module est indépendant** mais communique via APIs
2. **Les interfaces sont claires** et documentées
3. **Le build est unifié** avec Maven
4. **La démo est reproductible** avec les scripts
5. **La théorie réseau est visible** dans chaque module

**Prochaine étape** : Vous voulez que je détaille le `pom.xml` avec toutes les dépendances spécifiques pour chaque module ?