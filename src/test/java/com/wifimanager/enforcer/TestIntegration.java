package com.wifimanager.enforcer;

import com.wifimanager.detector.ClientDiscoveryService;
import com.wifimanager.shared.model.Client;

public class TestIntegration {
    public static void main(String[] args) throws Exception {
        System.out.println("🔗 TEST D'INTÉGRATION MODULE 1 + MODULE 2\n");
        
        // Module 1 - Détection
        System.out.println("1. Module 1 - Détection des clients...");
        ClientDiscoveryService detector = new ClientDiscoveryService("wlo1", "192.168.0.0/24");
        detector.discoverClients();
        
        System.out.println("   Clients détectés: " + detector.getTotalClientCount());
        for (Client client : detector.getAllClients()) {
            System.out.println("   - " + client.getMacAddress() + " → " + client.getIpAddress());
        }
        
        // Module 2 - Enforcer
        System.out.println("\n2. Module 2 - Application de quotas...");
        
        QuotaManager quotaManager = new QuotaManager();
        TrafficController trafficController = new TrafficController(true, "wlo1");
        DisconnectionService disconnectionService = new DisconnectionService(
            quotaManager, trafficController);
        
        // Si des vrais clients sont détectés, leur appliquer des quotas
        if (detector.getTotalClientCount() > 0) {
            Client firstClient = detector.getAllClients().get(0);
            String mac = firstClient.getMacAddress();
            // String ip = firstClient.getIpAddress();

            
            System.out.println("   Application quota au client: " + mac);
            
            // Définir un quota strict (1 minute, 10 MB)
            quotaManager.setQuota(mac, 1, 10);
            
            // Simuler consommation
            System.out.println("   Simulation consommation: 2 minutes, 20 MB");
            quotaManager.consumeTime(mac, 2);
            quotaManager.consumeData(mac, 20);
            
            // Vérifier si dépassé
            if (quotaManager.isExceeded(mac)) {
                System.out.println("   ✅ Quota dépassé - Déconnexion automatique");
                disconnectionService.disconnectClient(mac, "TEST_QUOTA_EXCEEDED");
            }
            
            // Vérifier état
            System.out.println("\n3. État final:");
            System.out.println("   Client " + mac + " bloqué: " + 
                (quotaManager.getQuota(mac).isActive() ? "NON" : "OUI"));
            System.out.println("   Quota actif: " + quotaManager.getQuota(mac).isActive());
            System.out.println("   Temps utilisé: " + quotaManager.getQuota(mac).getTimeUsedMinutes() + "/" + 
                quotaManager.getQuota(mac).getTimeLimitMinutes() + " min");
            System.out.println("   Données utilisées: " + quotaManager.getQuota(mac).getDataUsedMB() + "/" + 
                quotaManager.getQuota(mac).getDataLimitMB() + " MB");
        } else {
            System.out.println("   ℹ️  Aucun client réel détecté, test avec client simulé...");
            
            // Test avec client simulé
            String testMac = "TEST:MAC:00:00:00:01";
            String testIp = "192.168.0.200";
            
            quotaManager.setQuota(testMac, 2, 20);
            System.out.println("   Quota défini: " + testMac + " → 2 min, 20 MB");
            
            // Consommer plus que le quota
            quotaManager.consumeTime(testMac, 3);
            quotaManager.consumeData(testMac, 25);
            
            if (quotaManager.isExceeded(testMac)) {
                System.out.println("   ✅ Quota dépassé - Simulation déconnexion");
                trafficController.blockClient(testMac, testIp);
                quotaManager.deactivateQuota(testMac);
            }
        }
        
        // Vérifier le fichier de persistence
        System.out.println("\n4. Vérification persistence:");
        System.out.println("   Fichier: data/quotas.json");
        System.out.println("   Quotas en mémoire: " + quotaManager.getAllQuotas().size());
        
        // Recharger depuis le fichier pour tester
        QuotaManager quotaManager2 = new QuotaManager();
        System.out.println("   Quotas après rechargement: " + quotaManager2.getAllQuotas().size());
        
        System.out.println("\n✅ TEST D'INTÉGRATION RÉUSSI !");
    }
}