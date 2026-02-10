package com.wifimanager.detector;

public class TestAvecSudo {
   public static void main(String[] args) {
        System.out.println("=== TEST AVEC DROITS ADMIN ===\n");
        
        System.out.println("Vérification des droits...");
        System.out.println("User: " + System.getProperty("user.name"));
        
        try {
            // Test 1: Vérifier si arp-scan est installé
            System.out.println("\n1. Vérification arp-scan...");
            Process p = Runtime.getRuntime().exec("which arp-scan");
            p.waitFor();
            if (p.exitValue() == 0) {
                System.out.println("   ✅ arp-scan est installé");
                
                // Tester arp-scan
                System.out.println("\n2. Test arp-scan sur wlo1...");
                String[] cmd = {"sudo", "arp-scan", "--interface=wlo1", "--localnet"};
                System.out.println("   Commande: " + String.join(" ", cmd));
                
                p = Runtime.getRuntime().exec(cmd);
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
                
                int count = 0;
                String line;
                System.out.println("\n   Résultats:");
                while ((line = reader.readLine()) != null) {
                    if (line.contains("192.168.0.")) {
                        System.out.println("   " + line);
                        count++;
                    }
                }
                
                System.out.println("\n   Total clients détectés par arp-scan: " + count);
                
                if (count == 0) {
                    System.out.println("\n   ℹ️  Aucun client trouvé. Essaies-tu de scanner ton propre réseau WiFi?");
                    System.out.println("   Ton IP: 192.168.0.132");
                    System.out.println("   Ton routeur: probablement 192.168.0.1");
                }
                
            } else {
                System.out.println("   ❌ arp-scan n'est pas installé");
                System.out.println("   Installer avec: sudo apt-get install arp-scan");
            }
            
            // Test 2: Table ARP système
            System.out.println("\n3. Table ARP système...");
            p = Runtime.getRuntime().exec("ip neigh show");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            
            System.out.println("   Résultats ip neigh:");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("   " + line);
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ Erreur: " + e.getMessage());
            System.err.println("Essaie de lancer ce programme avec: sudo java ...");
        }
    }
}