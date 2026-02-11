package com.wifimanager.enforcer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Contrôleur de trafic réseau via iptables/tc
 */
public class TrafficController {
    private static final Logger logger = LoggerFactory.getLogger(TrafficController.class);
    
    private final boolean simulationMode;
    private final String networkInterface;
    
    public TrafficController(boolean simulationMode) {
        this(simulationMode, "wlo1"); // Interface par défaut
    }
    
    public TrafficController(boolean simulationMode, String networkInterface) {
        this.simulationMode = simulationMode;
        this.networkInterface = networkInterface;
        logger.info("TrafficController initialisé (mode: {}, interface: {})",
            simulationMode ? "SIMULATION" : "RÉEL", networkInterface);
    }
    
    /**
     * Bloque complètement un client
     */
    public void blockClient(String macAddress, String ipAddress) {
        macAddress = macAddress.toUpperCase();
        
        if (simulationMode) {
            logger.info("[SIMULATION] Blocage client {} ({})", macAddress, ipAddress);
            logger.info("  Commande: sudo iptables -A FORWARD -m mac --mac-source {} -j DROP", 
                macAddress);
        } else {
            try {
                // Bloquer par adresse MAC
                executeCommand("sudo", "iptables", "-A", "FORWARD", 
                    "-m", "mac", "--mac-source", macAddress, "-j", "DROP");
                
                // Également bloquer par IP (au cas où)
                executeCommand("sudo", "iptables", "-A", "FORWARD", 
                    "-s", ipAddress, "-j", "DROP");
                
                logger.info("Client bloqué: {} ({})", macAddress, ipAddress);
                
            } catch (Exception e) {
                logger.error("Erreur lors du blocage de {}: {}", macAddress, e.getMessage());
            }
        }
    }
    
    /**
     * Débloque un client
     */
    public void unblockClient(String macAddress, String ipAddress) {
        macAddress = macAddress.toUpperCase();
        
        if (simulationMode) {
            logger.info("[SIMULATION] Déblocage client {} ({})", macAddress, ipAddress);
            logger.info("  Commande: sudo iptables -D FORWARD -m mac --mac-source {} -j DROP", 
                macAddress);
        } else {
            try {
                // Retirer la règle MAC
                executeCommand("sudo", "iptables", "-D", "FORWARD", 
                    "-m", "mac", "--mac-source", macAddress, "-j", "DROP");
                
                // Retirer la règle IP
                executeCommand("sudo", "iptables", "-D", "FORWARD", 
                    "-s", ipAddress, "-j", "DROP");
                
                logger.info("Client débloqué: {} ({})", macAddress, ipAddress);
                
            } catch (Exception e) {
                logger.error("Erreur lors du déblocage de {}: {}", macAddress, e.getMessage());
            }
        }
    }
    
    /**
     * Limite la bande passante d'un client
     */
    public void limitBandwidth(String macAddress, String ipAddress, int kbps) {
        macAddress = macAddress.toUpperCase();
        
        if (simulationMode) {
            logger.info("[SIMULATION] Limitation bande passante {}: {} kbps", 
                macAddress, kbps);
            logger.info("  Commande: sudo tc qdisc add dev {} root handle 1: htb", 
                networkInterface);
            logger.info("  Commande: sudo tc class add dev {} parent 1: classid 1:{} htb rate {}kbit", 
                networkInterface, macAddress.replace(":", "").substring(0, 4), kbps);
        } else {
            try {
                // Vérifier si tc est déjà configuré
                Process check = Runtime.getRuntime().exec(new String[]{"tc", "qdisc", "show", "dev", networkInterface});
                if (check.waitFor() != 0) {
                    // Configurer tc pour l'interface
                    executeCommand("sudo", "tc", "qdisc", "add", "dev", networkInterface, 
                        "root", "handle", "1:", "htb");
                }
                
                // Créer une classe pour ce client
                String classId = "1:" + macAddress.replace(":", "").substring(0, 4);
                executeCommand("sudo", "tc", "class", "add", "dev", networkInterface, 
                    "parent", "1:", "classid", classId, "htb", "rate", kbps + "kbit");
                
                // Filtrer par adresse MAC
                executeCommand("sudo", "tc", "filter", "add", "dev", networkInterface, 
                    "protocol", "ip", "parent", "1:", "prio", "1", 
                    "handle", macAddress.replace(":", "").substring(6) + ":",
                    "fw", "flowid", classId);
                
                logger.info("Bande passante limitée à {} kbps pour {}", kbps, macAddress);
                
            } catch (Exception e) {
                logger.error("Erreur limitation bande passante pour {}: {}", 
                    macAddress, e.getMessage());
            }
        }
    }
    
    /**
     * Supprime la limitation de bande passante
     */
    public void removeBandwidthLimit(String macAddress) {
        macAddress = macAddress.toUpperCase();
        
        if (simulationMode) {
            logger.info("[SIMULATION] Suppression limitation bande passante pour {}", 
                macAddress);
        } else {
            try {
                String classId = "1:" + macAddress.replace(":", "").substring(0, 4);
                
                // Supprimer le filtre
                executeCommand("sudo", "tc", "filter", "del", "dev", networkInterface, 
                    "parent", "1:", "handle", "800::800", "prio", "1", "protocol", "ip");
                
                // Supprimer la classe
                executeCommand("sudo", "tc", "class", "del", "dev", networkInterface, 
                    "parent", "1:", "classid", classId);
                
                logger.info("Limitation bande passante supprimée pour {}", macAddress);
                
            } catch (Exception e) {
                logger.error("Erreur suppression limitation pour {}: {}", 
                    macAddress, e.getMessage());
            }
        }
    }
    
    /**
     * Affiche les règles iptables actuelles
     */
    public void showIptablesRules() {
        if (simulationMode) {
            logger.info("[SIMULATION] Affichage règles iptables");
            logger.info("  sudo iptables -L -n -v");
        } else {
            try {
                executeCommand("sudo", "iptables", "-L", "-n", "-v");
            } catch (Exception e) {
                logger.error("Erreur affichage règles iptables: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Affiche la configuration tc
     */
    public void showTrafficControl() {
        if (simulationMode) {
            logger.info("[SIMULATION] Affichage configuration tc");
            logger.info("  sudo tc qdisc show dev {}", networkInterface);
            logger.info("  sudo tc class show dev {}", networkInterface);
        } else {
            try {
                executeCommand("sudo", "tc", "qdisc", "show", "dev", networkInterface);
                executeCommand("sudo", "tc", "class", "show", "dev", networkInterface);
            } catch (Exception e) {
                logger.error("Erreur affichage configuration tc: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Réinitialise toutes les règles
     */
    public void resetAllRules() {
        if (simulationMode) {
            logger.info("[SIMULATION] Réinitialisation complète des règles");
            logger.info("  sudo iptables -F");
            logger.info("  sudo tc qdisc del dev {} root", networkInterface);
        } else {
            try {
                // Réinitialiser iptables
                executeCommand("sudo", "iptables", "-F");
                executeCommand("sudo", "iptables", "-X");
                
                // Supprimer la configuration tc
                executeCommand("sudo", "tc", "qdisc", "del", "dev", networkInterface, "root");
                
                logger.info("Toutes les règles ont été réinitialisées");
                
            } catch (Exception e) {
                logger.error("Erreur réinitialisation règles: {}", e.getMessage());
            }
        }
    }
    
    // Méthode utilitaire pour exécuter des commandes
    private void executeCommand(String... command) throws Exception {
        logger.debug("Exécution: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Lire la sortie
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("  {}", line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Commande échouée avec code: " + exitCode);
        }
    }
    
    // Getters
    public boolean isSimulationMode() { return simulationMode; }
    public String getNetworkInterface() { return networkInterface; }
}