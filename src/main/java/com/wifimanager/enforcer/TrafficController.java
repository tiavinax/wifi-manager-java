package com.wifimanager.enforcer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* CONTRÔLEUR DE TRAFIC RÉSEAU - Interface avec iptables et tc (Traffic Control) */
public class TrafficController {
    
    //  CONFIGURATION
    private final boolean simulationMode;           // true = simulation, false = exécution réelle
    private static final String NETWORK_INTERFACE = "wlan0";  // Interface réseau WiFi par défaut
    
    //  TRACKING
    // Map<MAC, IP> des clients actuellement bloqués
    private final Map<String, String> blockedClients = new ConcurrentHashMap<>();
    
    // Map<MAC, KBPS> des clients avec limitation de bande passante
    private final Map<String, Integer> bandwidthLimits = new ConcurrentHashMap<>();
    
    //  STATISTIQUES
    private int totalBlockOperations = 0;
    private int totalUnblockOperations = 0;
    private int totalBandwidthLimits = 0;
    private int failedOperations = 0;
    
    //  CONSTRUCTEUR
    
    public TrafficController(boolean simulationMode) {
        this.simulationMode = simulationMode;
        
        if (simulationMode) {
            System.out.println("[TrafficController] ⚠️ MODE SIMULATION activé - Les commandes ne seront PAS exécutées");
        } else {
            System.out.println("[TrafficController] 🔴 MODE RÉEL activé - Les commandes seront exécutées avec sudo");
            System.out.println("[TrafficController] ⚠️ ATTENTION: Nécessite les droits sudo !");
        }
    }
    
    //  BLOCAGE CLIENT 
    
    public boolean blockClient(String macAddress, String ipAddress) {
        // Validation
        if (macAddress == null || macAddress.trim().isEmpty()) {
            System.err.println("[TrafficController] ❌ MAC address vide, blocage impossible");
            return false;
        }
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            System.err.println("[TrafficController] ❌ IP address vide, blocage impossible");
            return false;
        }
        
        macAddress = macAddress.trim().toUpperCase();
        ipAddress = ipAddress.trim();
        
        // Vérifier si déjà bloqué
        if (blockedClients.containsKey(macAddress)) {
            System.out.println("[TrafficController] ⚠️ Client " + macAddress + " déjà bloqué");
            return true;
        }
        
        System.out.println("\n[TrafficController] 🔒 BLOCAGE du client " + macAddress + " (IP: " + ipAddress + ")");
        
        // Commande 1: Bloquer par MAC (trafic sortant du client)
        String cmd1 = String.format("sudo iptables -A FORWARD -m mac --mac-source %s -j DROP", macAddress);
        
        // Commande 2: Bloquer par IP (trafic entrant vers le client)
        String cmd2 = String.format("sudo iptables -A FORWARD -d %s -j DROP", ipAddress);
        
        boolean success = true;
        
        // Exécuter ou simuler
        if (simulationMode) {
            System.out.println("  [SIMULATION] " + cmd1);
            System.out.println("  [SIMULATION] " + cmd2);
            System.out.println("  [SIMULATION] ✅ Client bloqué (simulation)");
        } else {
            // Exécution réelle
            success = executeCommand(cmd1) && executeCommand(cmd2);
            
            if (success) {
                System.out.println("  [RÉEL] ✅ Client bloqué avec iptables");
            } else {
                System.err.println("  [RÉEL] ❌ Échec du blocage iptables");
                failedOperations++;
                return false;
            }
        }
        
        // Enregistrer dans la map
        blockedClients.put(macAddress, ipAddress);
        totalBlockOperations++;
        
        return success;
    }
    
    /* Débloquer un client (supprimer les règles iptables).*/
    public boolean unblockClient(String macAddress, String ipAddress) {
        if (macAddress == null || ipAddress == null) return false;
        
        macAddress = macAddress.trim().toUpperCase();
        ipAddress = ipAddress.trim();
        
        // Vérifier si vraiment bloqué
        if (!blockedClients.containsKey(macAddress)) {
            System.out.println("[TrafficController] ⚠️ Client " + macAddress + " n'est pas bloqué");
            return true;
        }
        
        System.out.println("\n[TrafficController] 🔓 DÉBLOCAGE du client " + macAddress + " (IP: " + ipAddress + ")");
        
        // Commande 1: Débloquer par MAC
        String cmd1 = String.format("sudo iptables -D FORWARD -m mac --mac-source %s -j DROP", macAddress);
        
        // Commande 2: Débloquer par IP
        String cmd2 = String.format("sudo iptables -D FORWARD -d %s -j DROP", ipAddress);
        
        boolean success = true;
        
        if (simulationMode) {
            System.out.println("  [SIMULATION] " + cmd1);
            System.out.println("  [SIMULATION] " + cmd2);
            System.out.println("  [SIMULATION] ✅ Client débloqué (simulation)");
        } else {
            success = executeCommand(cmd1) && executeCommand(cmd2);
            
            if (success) {
                System.out.println("  [RÉEL] ✅ Client débloqué avec iptables");
            } else {
                System.err.println("  [RÉEL] ❌ Échec du déblocage iptables");
                failedOperations++;
                return false;
            }
        }
        
        // Retirer de la map
        blockedClients.remove(macAddress);
        totalUnblockOperations++;
        
        return success;
    }
    
    // LIMITATION BANDE PASSANTE 
    
    public boolean limitBandwidth(String macAddress, String ipAddress, int kbps) {
        if (macAddress == null || ipAddress == null || kbps <= 0) {
            System.err.println("[TrafficController] ❌ Paramètres invalides pour limitation bande passante");
            return false;
        }
        
        macAddress = macAddress.trim().toUpperCase();
        ipAddress = ipAddress.trim();
        
        System.out.println("\n[TrafficController] 🚦 LIMITATION BANDE PASSANTE: " + macAddress + 
                           " (IP: " + ipAddress + ") → " + kbps + " kbps");
        
        // Commande simplifiée pour la démonstration
        // En production, il faudrait des commandes plus complexes avec tc classes
        String cmd = String.format(
            "sudo tc qdisc add dev %s root tbf rate %dkbit burst 32kbit latency 400ms",
            NETWORK_INTERFACE, kbps
        );
        
        boolean success = true;
        
        if (simulationMode) {
            System.out.println("  [SIMULATION] " + cmd);
            System.out.println("  [SIMULATION] ✅ Bande passante limitée à " + kbps + " kbps (simulation)");
        } else {
            success = executeCommand(cmd);
            
            if (success) {
                System.out.println("  [RÉEL] ✅ Bande passante limitée avec tc");
            } else {
                System.err.println("  [RÉEL] ❌ Échec de la limitation tc");
                failedOperations++;
                return false;
            }
        }
        
        // Enregistrer la limitation
        bandwidthLimits.put(macAddress, kbps);
        totalBandwidthLimits++;
        
        return success;
    }
    
    /* Supprimer la limitation de bande passante d'un client.*/
    public boolean removeBandwidthLimit(String macAddress, String ipAddress) {
        if (macAddress == null || ipAddress == null) return false;
        
        macAddress = macAddress.trim().toUpperCase();
        ipAddress = ipAddress.trim();
        
        if (!bandwidthLimits.containsKey(macAddress)) {
            System.out.println("[TrafficController] ⚠️ Aucune limitation active pour " + macAddress);
            return true;
        }
        
        System.out.println("\n[TrafficController] 🚀 SUPPRESSION LIMITATION: " + macAddress + " (IP: " + ipAddress + ")");
        
        String cmd = String.format("sudo tc qdisc del dev %s root", NETWORK_INTERFACE);
        
        boolean success = true;
        
        if (simulationMode) {
            System.out.println("  [SIMULATION] " + cmd);
            System.out.println("  [SIMULATION] ✅ Limitation supprimée (simulation)");
        } else {
            success = executeCommand(cmd);
            
            if (success) {
                System.out.println("  [RÉEL] ✅ Limitation supprimée avec tc");
            } else {
                System.err.println("  [RÉEL] ❌ Échec de suppression tc");
                failedOperations++;
                return false;
            }
        }
        
        bandwidthLimits.remove(macAddress);
        
        return success;
    }
    
    //EXÉCUTION COMMANDES SYSTÈME (iptables/tc).
    private boolean executeCommand(String command) {
        try {
            System.out.println("  [EXEC] " + command);
            
            // Exécuter la commande
            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            
            // Lire stdout
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            String line;
            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();
            
            while ((line = stdOut.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = stdErr.readLine()) != null) {
                errors.append(line).append("\n");
            }
            
            // Attendre la fin
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                if (output.length() > 0) {
                    System.out.println("  [OUTPUT] " + output.toString().trim());
                }
                return true;
            } else {
                System.err.println("  [ERROR] Code retour: " + exitCode);
                if (errors.length() > 0) {
                    System.err.println("  [STDERR] " + errors.toString().trim());
                }
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("  [EXCEPTION] " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /* Vérifier si un client est actuellement bloqué.*/
    public boolean isBlocked(String macAddress) {
        return blockedClients.containsKey(macAddress.toUpperCase());
    }
    
    /* Obtenir la limitation de bande passante d'un client.*/
    public int getBandwidthLimit(String macAddress) {
        return bandwidthLimits.getOrDefault(macAddress.toUpperCase(), -1);
    }
    
    /*Obtenir la liste de tous les clients bloqués.*/
    public List<String> getBlockedClients() {
        return new ArrayList<>(blockedClients.keySet());
    }
    
    /*Statistiques du contrôleur.*/
    public void printStats() {
        System.out.println("\n========== STATISTIQUES TRAFFICCONTROLLER ==========");
        System.out.println("Mode: " + (simulationMode ? "SIMULATION" : "RÉEL"));
        System.out.println("Interface réseau: " + NETWORK_INTERFACE);
        System.out.println("Clients bloqués actuellement: " + blockedClients.size());
        System.out.println("Limitations bande passante actives: " + bandwidthLimits.size());
        System.out.println("Total opérations blocage: " + totalBlockOperations);
        System.out.println("Total opérations déblocage: " + totalUnblockOperations);
        System.out.println("Total limitations bande passante: " + totalBandwidthLimits);
        System.out.println("Opérations échouées: " + failedOperations);
        System.out.println("===================================================\n");
    }
    
    //  MAIN DE TEST 

    public static void main(String[] args) {
        System.out.println("========== TEST TRAFFICCONTROLLER ==========\n");
        
        // IMPORTANT: Mode simulation pour ne pas nécessiter sudo
        TrafficController controller = new TrafficController(true);
        
        // Test 1: Bloquer un client
        System.out.println("\n--- Test 1: Blocage client ---");
        boolean blocked = controller.blockClient("AA:BB:CC:DD:EE:FF", "192.168.1.105");
        System.out.println("Résultat blocage: " + (blocked ? "✅ Succès" : "❌ Échec"));
        
        // Test 2: Vérifier si bloqué
        System.out.println("\n--- Test 2: Vérification statut ---");
        System.out.println("Client bloqué? " + controller.isBlocked("AA:BB:CC:DD:EE:FF"));
        
        // Test 3: Tenter de bloquer à nouveau
        System.out.println("\n--- Test 3: Double blocage ---");
        controller.blockClient("AA:BB:CC:DD:EE:FF", "192.168.1.105");
        
        // Test 4: Limiter bande passante
        System.out.println("\n--- Test 4: Limitation bande passante ---");
        controller.limitBandwidth("AA:BB:CC:DD:EE:FF", "192.168.1.105", 512);
        System.out.println("Limitation actuelle: " + controller.getBandwidthLimit("AA:BB:CC:DD:EE:FF") + " kbps");
        
        // Test 5: Bloquer un deuxième client
        System.out.println("\n--- Test 5: Bloquer client 2 ---");
        controller.blockClient("11:22:33:44:55:66", "192.168.1.106");
        controller.limitBandwidth("11:22:33:44:55:66", "192.168.1.106", 1024);
        
        // Test 6: Liste des clients bloqués
        System.out.println("\n--- Test 6: Liste clients bloqués ---");
        System.out.println("Clients bloqués: " + controller.getBlockedClients());
        
        // Test 7: Débloquer premier client
        System.out.println("\n--- Test 7: Déblocage client 1 ---");
        controller.unblockClient("AA:BB:CC:DD:EE:FF", "192.168.1.105");
        System.out.println("Client encore bloqué? " + controller.isBlocked("AA:BB:CC:DD:EE:FF"));
        
        // Test 8: Supprimer limitation
        System.out.println("\n--- Test 8: Suppression limitation ---");
        controller.removeBandwidthLimit("11:22:33:44:55:66", "192.168.1.106");
        
        // Test 9: Statistiques
        System.out.println("\n--- Test 9: Statistiques ---");
        controller.printStats();
        
        System.out.println("\n========== FIN DES TESTS ==========");
    }
}
