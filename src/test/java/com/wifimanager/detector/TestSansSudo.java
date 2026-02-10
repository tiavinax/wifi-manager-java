package com.wifimanager.detector;

import java.util.Map;

public class TestSansSudo {
    public static void main(String[] args) {
        System.out.println("=== TEST SANS SUDO (avec droits configurés) ===\n");
        
        System.out.println("1. Vérification des droits...");
        System.out.println("   User: " + System.getProperty("user.name"));
        
        try {
            // Test commande ip sans sudo
            System.out.println("\n2. Test commande 'ip neigh show dev wlo1'...");
            Process p = Runtime.getRuntime().exec(new String[]{"ip", "neigh", "show", "dev", "wlo1"});
            
            StringBuilder output = new StringBuilder();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append("   ").append(line).append("\n");
            }
            
            p.waitFor();
            
            if (output.length() > 0) {
                System.out.println("✅ Commande réussie:");
                System.out.print(output.toString());
            } else {
                System.out.println("ℹ️  Aucun résultat (table ARP vide ou droits insuffisants)");
                
                // Vérifier les erreurs
                reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    System.out.println("   Erreur: " + line);
                }
            }
            
            // Test ArpScanner
            System.out.println("\n3. Test ArpScanner.scan()...");
            ArpScanner scanner = new ArpScanner("wlo1", "192.168.0.0/24");
            Map<String, String> results = scanner.scan();
            
            System.out.println("   Clients trouvés: " + results.size());
            if (!results.isEmpty()) {
                System.out.println("   Détails:");
                for (Map.Entry<String, String> entry : results.entrySet()) {
                    System.out.println("   - " + entry.getKey() + " → " + entry.getValue());
                }
            } else {
                System.out.println("   ℹ️  Aucun client trouvé");
                System.out.println("   Essaie 'arp -a' manuellement:");
                p = Runtime.getRuntime().exec(new String[]{"arp", "-a"});
                reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    System.out.println("   " + line);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}