package com.wifimanager.detector;

import java.util.Map;

public class TestUltime {
    public static void main(String[] args) {
        System.out.println("=== TEST ULTIME - TOUTES LES MÉTHODES ===\n");
        
        System.out.println("1. Test individuel des commandes:");
        
        // Test ip neigh
        testCommande("ip neigh show dev wlo1");
        
        // Test arp -a
        testCommande("arp -a");
        
        // Test arp-scan
        testCommande("arp-scan --interface=wlo1 --localnet --quiet");
        
        System.out.println("\n2. Test complet ArpScanner:");
        ArpScanner scanner = new ArpScanner("wlo1", "192.168.0.0/24");
        
        System.out.println("   Scan en cours...");
        long startTime = System.currentTimeMillis();
        Map<String, String> results = scanner.scan();
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n3. Résultats:");
        System.out.println("   Temps de scan: " + (endTime - startTime) + "ms");
        System.out.println("   Clients trouvés: " + results.size());
        
        if (!results.isEmpty()) {
            System.out.println("\n   Détails des clients:");
            System.out.println("   " + String.format("%-20s %-15s %-20s", 
                "Adresse MAC", "IP", "Type d'appareil"));
            System.out.println("   " + "-".repeat(60));
            
            for (Map.Entry<String, String> entry : results.entrySet()) {
                String deviceType = scanner.detectDeviceType(entry.getKey());
                System.out.println("   " + String.format("%-20s %-15s %-20s",
                    entry.getKey(),
                    entry.getValue(),
                    deviceType));
            }
            
            // Détails supplémentaires
            System.out.println("\n4. Informations réseau:");
            System.out.println("   Interface: " + scanner.getNetworkInterface());
            System.out.println("   Subnet: " + scanner.getSubnet());
            
        } else {
            System.out.println("\n⚠️  Aucun client détecté.");
            System.out.println("\nSolutions possibles:");
            System.out.println("1. Êtes-vous connecté au WiFi?");
            System.out.println("2. Vérifiez l'interface: 'ip addr show'");
            System.out.println("3. Testez manuellement: 'arp -a'");
            System.out.println("4. Installez arp-scan: sudo apt-get install arp-scan");
        }
        
        System.out.println("\n✅ Test terminé.");
    }
    
    private static void testCommande(String commande) {
        System.out.print("\n   Commande: " + commande + " ... ");
        try {
            Process p = Runtime.getRuntime().exec(commande.split(" "));
            
            // Lire la sortie
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (count <= 3) { // Afficher seulement les 3 premières lignes
                    output.append("\n      ").append(line);
                }
            }
            
            p.waitFor();
            
            if (count > 0) {
                System.out.println("✅ (" + count + " lignes)" + output.toString());
                if (count > 3) {
                    System.out.println("      ... et " + (count - 3) + " lignes supplémentaires");
                }
            } else {
                System.out.println("ℹ️  Aucun résultat");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }
}