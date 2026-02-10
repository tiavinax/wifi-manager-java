package com.wifimanager.detector;

public class TestAvecBonnesValeurs {
    public static void main(String[] args) {
        System.out.println("=== TEST AVEC BONNES VALEURS ===\n");
        
        try {
            System.out.println("1. Création service avec interface wlo1...");
            ClientDiscoveryService service = new ClientDiscoveryService("wlo1", "192.168.0.0/24");
            
            System.out.println("2. Découverte des clients (sans sudo)...");
            service.discoverClients();
            
            int clients = service.getTotalClientCount();
            System.out.println("   Clients trouvés: " + clients);
            
            if (clients == 0) {
                System.out.println("\n3. Essai avec sudo (commande système)...");
                testAvecSudo();
            }
            
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
    
    private static void testAvecSudo() {
        try {
            System.out.println("   Commande: arp -a");
            Process p = Runtime.getRuntime().exec("arp -a");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            
            String line;
            boolean found = false;
            System.out.println("   Résultats:");
            while ((line = reader.readLine()) != null) {
                if (line.contains("192.168.0.")) {
                    System.out.println("   ✅ " + line);
                    found = true;
                }
            }
            
            if (!found) {
                System.out.println("   ℹ️  Aucun client trouvé avec arp -a");
                System.out.println("\n   Essaie: ip neigh show");
                p = Runtime.getRuntime().exec("ip neigh show");
                reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
                
                while ((line = reader.readLine()) != null) {
                    System.out.println("   " + line);
                }
            }
            
        } catch (Exception e) {
            System.err.println("   Erreur: " + e.getMessage());
        }
    }
}