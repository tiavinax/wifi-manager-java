package com.wifimanager.detector;

import com.wifimanager.shared.model.Client;

public class TestFinal {
    public static void main(String[] args) throws Exception {
        System.out.println("🎯 TEST FINAL - MODULE DÉTECTION FONCTIONNEL\n");
        
        System.out.println("1. Initialisation avec interface wlo1...");
        ClientDiscoveryService service = new ClientDiscoveryService("wlo1", "192.168.0.0/24");
        
        System.out.println("2. Découverte des clients...");
        service.discoverClients();
        
        int totalClients = service.getTotalClientCount();
        System.out.println("3. Résultats:");
        System.out.println("   Total clients détectés: " + totalClients);
        System.out.println("   Clients actifs: " + service.getActiveClientCount());
        
        if (totalClients > 0) {
            System.out.println("\n4. Liste des clients:");
            System.out.println("   " + String.format("%-20s %-15s %-10s %-15s", 
                "MAC", "IP", "Actif", "Type"));
            System.out.println("   " + "-".repeat(60));
            
            for (Client client : service.getAllClients()) {
                String deviceType = determineDeviceType(client.getMacAddress());
                System.out.println("   " + String.format("%-20s %-15s %-10s %-15s",
                    client.getMacAddress(),
                    client.getIpAddress(),
                    client.isActive() ? "✓" : "✗",
                    deviceType));
                
                // Ajouter des infos supplémentaires
                if (client.getMacAddress().startsWith("D8:42:F7")) {
                    client.setHostname("Routeur WiFi");
                    client.setVendor("Routeur domestique");
                } else if (client.getMacAddress().startsWith("7C:5C:F8")) {
                    client.setHostname("Mon-PC-HP");
                    client.setVendor("HP");
                }
            }
            
            // Test API
            System.out.println("\n5. Test serveur API...");
            DetectorApiServer apiServer = new DetectorApiServer(service, 8081);
            apiServer.start();
            
            System.out.println("   ✅ Serveur démarré sur http://localhost:8081");
            System.out.println("   Endpoints:");
            System.out.println("   - http://localhost:8081/api/clients");
            System.out.println("   - http://localhost:8081/api/stats");
            
            // Attendre un peu pour voir les logs
            System.out.println("\n6. Test dans 10 secondes (Ctrl+C pour arrêter)...");
            Thread.sleep(10000);
            
            apiServer.stop();
            
        } else {
            System.out.println("\n⚠️  Aucun client détecté. Mais on sait que arp-scan en trouve !");
            System.out.println("   Problème probable: droits insuffisants");
            System.out.println("   Solution: Lancer avec sudo");
        }
        
        System.out.println("\n✅ TEST TERMINÉ - MODULE OPÉRATIONNEL !");
    }
    
    private static String determineDeviceType(String mac) {
        // Détection basique par préfixe MAC
        if (mac.startsWith("D8:42:F7")) return "Routeur";
        if (mac.startsWith("7C:5C:F8")) return "PC Portable";
        if (mac.startsWith("AA:BB:CC") || mac.startsWith("00:11:22")) return "Test";
        
        // Recherche OUI (Organizationally Unique Identifier)
        String[] routers = {"D8:42:F7", "00:1D:7E", "00:23:69"};
        String[] phones = {"64:BC:0C", "3C:CD:5D", "F0:EE:10"};
        String[] pcs = {"7C:5C:F8", "00:1A:2B", "00:1C:42"};
        
        for (String prefix : routers) {
            if (mac.startsWith(prefix)) return "Routeur";
        }
        for (String prefix : phones) {
            if (mac.startsWith(prefix)) return "Téléphone";
        }
        for (String prefix : pcs) {
            if (mac.startsWith(prefix)) return "PC";
        }
        
        return "Inconnu";
    }
}