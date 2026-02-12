package com.wifimanager.enforcer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
// import java.net.NetworkInterface;
import java.util.*;

/**
 * Contrôleur de trafic réseau via iptables/tc
 * MODE RÉEL avec protection anti-blocage
 */
public class TrafficController {
    private static final Logger logger = LoggerFactory.getLogger(TrafficController.class);
    
    private final boolean simulationMode;
    private final String networkInterface;
    private final String myMacAddress;
    private BlockedClientManager blockedClientManager;
    
    public TrafficController(boolean simulationMode) {
        this(simulationMode, getDefaultInterface());
    }
    
    public TrafficController(boolean simulationMode, String networkInterface) {
        this.simulationMode = simulationMode;
        this.networkInterface = networkInterface;
        this.myMacAddress = getMyMacAddress();
        this.blockedClientManager = new BlockedClientManager();
        
        if (simulationMode) {
            logger.warn("⚠️ Mode SIMULATION - Les commandes ne sont PAS exécutées");
        } else {
            logger.info("🔥 MODE RÉEL ACTIVÉ - Les clients seront VRAIMENT bloqués");
            logger.info("🛡️ Ma MAC: {} (Protection active)", myMacAddress);
            
            // Restaurer les blocages au démarrage
            restoreBlockedClients();
        }
        
        logger.info("📡 Interface réseau: {}", networkInterface);
    }
    
    /**
     * Récupérer l'interface réseau par défaut
     */
    private static String getDefaultInterface() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"ip", "-br", "addr", "show"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("UP") && line.contains("192.168.")) {
                    String[] parts = line.split("\\s+");
                    return parts[0];
                }
            }
        } catch (Exception e) {
            logger.error("Erreur détection interface: {}", e.getMessage());
        }
        return "wlo1"; // Fallback
    }
    
    /**
     * Récupérer l'adresse MAC de l'interface
     */
    private String getMyMacAddress() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                "cat", "/sys/class/net/" + networkInterface + "/address"
            });
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String mac = reader.readLine();
            return mac != null ? mac.trim().toUpperCase() : "UNKNOWN";
        } catch (Exception e) {
            logger.error("Erreur récupération MAC: {}", e.getMessage());
            return "UNKNOWN";
        }
    }
    
    /**
     * Restaurer les blocages au démarrage
     */
    private void restoreBlockedClients() {
        List<BlockedClient> blocked = blockedClientManager.getAllBlockedClients();
        if (blocked.isEmpty()) {
            logger.info("📂 Aucun client à restaurer");
            return;
        }
        
        logger.info("🔄 Restauration de {} clients bloqués...", blocked.size());
        int success = 0;
        
        for (BlockedClient client : blocked) {
            try {
                blockClient(client.getMacAddress(), client.getIpAddress(), 
                    "RESTORE:" + client.getReason());
                success++;
            } catch (Exception e) {
                logger.error("❌ Échec restauration {}: {}", 
                    client.getMacAddress(), e.getMessage());
            }
        }
        
        logger.info("✅ {}/{} clients restaurés", success, blocked.size());
    }
    
    /**
     * 🛡️ PROTECTION: Vérifier si on essaie de se bloquer soi-même
     */
    private boolean isSelf(String macAddress) {
        boolean self = macAddress.toUpperCase().equals(myMacAddress);
        if (self) {
            logger.error("🚫 TENTATIVE DE BLOCAGE DE SOI-MÊME - INTERDIT !");
            logger.error("   MAC: {}", macAddress);
            logger.error("   C'est votre propre carte réseau !");
        }
        return self;
    }
    
    /**
     * Bloque complètement un client
     */
    public boolean blockClient(String macAddress, String ipAddress, String reason) {
        macAddress = macAddress.toUpperCase();
        
        // 🛡️ PROTECTION: Ne pas se bloquer soi-même
        if (isSelf(macAddress)) {
            return false;
        }
        
        if (simulationMode) {
            logger.info("[SIMULATION] Blocage client {} ({}) - {}", 
                macAddress, ipAddress, reason);
            logger.info("  Commande: sudo iptables -A FORWARD -m mac --mac-source {} -j DROP", 
                macAddress);
            return true;
        }
        
        // ========== MODE RÉEL ==========
        try {
            logger.info("🔨 BLOCAGE RÉEL: {} ({}) - {}", macAddress, ipAddress, reason);
            
            // 1. Bloquer par adresse MAC
            boolean macBlocked = executeCommandWithCheck(
                "sudo", "iptables", "-A", "FORWARD", 
                "-m", "mac", "--mac-source", macAddress, "-j", "DROP"
            );
            
            // 2. Bloquer par IP (au cas où)
            boolean ipBlocked = executeCommandWithCheck(
                "sudo", "iptables", "-A", "FORWARD", 
                "-s", ipAddress, "-j", "DROP"
            );
            
            if (macBlocked || ipBlocked) {
                // Ajouter à la liste des bloqués
                blockedClientManager.blockClient(macAddress, ipAddress, reason);
                
                logger.info("✅ Client BLOQUÉ: {} ({})", macAddress, ipAddress);
                
                // Vérification
                verifyBlock(macAddress);
                return true;
            } else {
                logger.error("❌ Échec blocage: {} ({})", macAddress, ipAddress);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ Erreur blocage {}: {}", macAddress, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Débloque un client
     */
    public boolean unblockClient(String macAddress, String ipAddress) {
        macAddress = macAddress.toUpperCase();
        
        if (simulationMode) {
            logger.info("[SIMULATION] Déblocage client {} ({})", macAddress, ipAddress);
            return true;
        }
        
        try {
            logger.info("🔓 DÉBLOCAGE: {} ({})", macAddress, ipAddress);
            
            // 1. Supprimer la règle MAC
            executeCommandWithCheck(
                "sudo", "iptables", "-D", "FORWARD", 
                "-m", "mac", "--mac-source", macAddress, "-j", "DROP"
            );
            
            // 2. Supprimer la règle IP
            executeCommandWithCheck(
                "sudo", "iptables", "-D", "FORWARD", 
                "-s", ipAddress, "-j", "DROP"
            );
            
            // Retirer de la liste des bloqués
            blockedClientManager.unblockClient(macAddress);
            
            logger.info("✅ Client DÉBLOQUÉ: {} ({})", macAddress, ipAddress);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Erreur déblocage {}: {}", macAddress, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Vérifier qu'un client est bien bloqué
     */
    private void verifyBlock(String macAddress) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                "sudo", "iptables", "-L", "FORWARD", "-n", "-v"
            });
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            
            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains(macAddress.replace(":", "").toLowerCase()) || 
                    line.contains(macAddress.replace(":", "").toUpperCase())) {
                    logger.info("✅ Vérification: Règle présente dans iptables");
                    logger.debug("   {}", line.trim());
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                logger.warn("⚠️ Vérification: Règle non trouvée dans iptables");
            }
            
        } catch (Exception e) {
            logger.error("❌ Erreur vérification blocage: {}", e.getMessage());
        }
    }
    
    /**
     * Exécute une commande et vérifie le code de retour
     */
    private boolean executeCommandWithCheck(String... command) throws Exception {
        logger.debug("⚡ Exécution: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Lire la sortie en temps réel
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("   {}", line);
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode == 0) {
            logger.debug("   ✅ Succès (code: {})", exitCode);
            return true;
        } else {
            logger.error("   ❌ Échec (code: {})", exitCode);
            return false;
        }
    }
    
    /**
     * Vérifier si un client est bloqué
     */
    public boolean isClientBlocked(String macAddress) {
        return blockedClientManager.isBlocked(macAddress);
    }
    
    /**
     * Récupérer la liste des clients bloqués
     */
    public List<BlockedClient> getBlockedClients() {
        return blockedClientManager.getAllBlockedClients();
    }
    
    /**
     * Récupérer le BlockedClientManager
     */
    public BlockedClientManager getBlockedClientManager() {
        return blockedClientManager;
    }
    
    // ========== MÉTHODES EXISTANTES À CONSERVER ==========
    
    public void limitBandwidth(String macAddress, String ipAddress, int kbps) {
        // Code inchangé...
        if (simulationMode) {
            logger.info("[SIMULATION] Limitation bande passante {}: {} kbps", macAddress, kbps);
        } else {
            // ... votre code existant
        }
    }
    
    public void removeBandwidthLimit(String macAddress) {
        // Code inchangé...
    }
    
    public void showIptablesRules() {
        if (simulationMode) {
            logger.info("[SIMULATION] sudo iptables -L -n -v");
        } else {
            try {
                executeCommandWithCheck("sudo", "iptables", "-L", "-n", "-v");
            } catch (Exception e) {
                logger.error("Erreur affichage iptables: {}", e.getMessage());
            }
        }
    }
    
    public void resetAllRules() {
        if (simulationMode) {
            logger.info("[SIMULATION] Réinitialisation complète");
        } else {
            try {
                executeCommandWithCheck("sudo", "iptables", "-F", "FORWARD");
                blockedClientManager = new BlockedClientManager(); // Reset
                logger.info("✅ Toutes les règles FORWARD réinitialisées");
            } catch (Exception e) {
                logger.error("❌ Erreur réinitialisation: {}", e.getMessage());
            }
        }
    }
    
    public boolean isSimulationMode() { return simulationMode; }
    public String getNetworkInterface() { return networkInterface; }
    // public String getMyMacAddress() { return myMacAddress; }
}