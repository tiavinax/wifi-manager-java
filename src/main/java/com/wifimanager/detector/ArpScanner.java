package com.wifimanager.detector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scanner ARP pour découvrir les clients sur le réseau
 */
public class ArpScanner {
    private static final Logger logger = LoggerFactory.getLogger(ArpScanner.class);
    private static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})";
    private static final String IP_ADDRESS_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private final String networkInterface;
    private final String subnet;
    private final Pattern macPattern;
    private final Pattern ipPattern;

    public ArpScanner(String networkInterface, String subnet) {
        this.networkInterface = networkInterface;
        this.subnet = subnet;
        this.macPattern = Pattern.compile(MAC_ADDRESS_REGEX);
        this.ipPattern = Pattern.compile(IP_ADDRESS_REGEX);
        logger.info("ARP Scanner initialisé pour interface: {} subnet: {}", networkInterface, subnet);
    }

    /**
     * Effectue un scan ARP sur le réseau
     * 
     * @return Map des addresses MAC trouvées avec leurs IPs
     */

    /**
     * Effectue un scan ARP sur le réseau pour trouver TOUS les clients connectés
     * 
     * @return Map des adresses MAC trouvées avec leurs IPs
     */
    public Map<String, String> scan() {
        Map<String, String> result = new HashMap<>();

        try {
            logger.info("🔍 DÉBUT DU SCAN RÉSEAU SUR {}", networkInterface);

            // ===========================================
            // MÉTHODE 1: arp-scan AVEC sudo (LA MEILLEURE)
            // ===========================================
            logger.info("📡 Méthode 1: arp-scan avec sudo...");
            try {
                String[] cmd = { "sudo", "arp-scan", "--interface=" + networkInterface,
                        "--localnet", "--quiet" };

                Process p = Runtime.getRuntime().exec(cmd);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            String ip = parts[0];
                            String mac = parts[1];

                            // Ignorer les lignes d'en-tête et de statistiques
                            if (isValidIpAddress(ip) && isValidMacAddress(mac)) {
                                result.put(mac.toUpperCase(), ip);
                                logger.info("   ✅ arp-scan: {} → {}", mac, ip);
                            }
                        }
                    }
                }

                p.waitFor();
                logger.info("   ✅ arp-scan terminé: {} clients trouvés", result.size());

            } catch (Exception e) {
                logger.warn("   ⚠️ arp-scan échoué: {}", e.getMessage());
            }

            // ===========================================
            // MÉTHODE 2: ip neigh show (fallback)
            // ===========================================
            if (result.isEmpty()) {
                logger.info("📡 Méthode 2: ip neigh show...");
                try {
                    String[] cmd = { "ip", "neigh", "show", "dev", networkInterface };
                    Process p = Runtime.getRuntime().exec(cmd);

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(p.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.trim().split("\\s+");
                            String ip = null;
                            String mac = null;

                            // Extraire IP
                            for (String part : parts) {
                                if (isValidIpAddress(part)) {
                                    ip = part;
                                    break;
                                }
                            }

                            // Extraire MAC
                            for (String part : parts) {
                                if (isValidMacAddress(part)) {
                                    mac = part.replaceAll("[()]", "");
                                    break;
                                }
                            }

                            if (ip != null && mac != null) {
                                result.putIfAbsent(mac.toUpperCase(), ip);
                                logger.info("   ✅ ip neigh: {} → {}", mac, ip);
                            }
                        }
                    }
                    p.waitFor();

                } catch (Exception e) {
                    logger.warn("   ⚠️ ip neigh échoué: {}", e.getMessage());
                }
            }

            // ===========================================
            // MÉTHODE 3: arp -a (dernier recours)
            // ===========================================
            if (result.isEmpty()) {
                logger.info("📡 Méthode 3: arp -a...");
                try {
                    String[] cmd = { "arp", "-a" };
                    Process p = Runtime.getRuntime().exec(cmd);

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(p.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains(networkInterface)) {
                                // Extraire IP entre parenthèses
                                int startParen = line.indexOf('(');
                                int endParen = line.indexOf(')');
                                if (startParen != -1 && endParen != -1) {
                                    String ip = line.substring(startParen + 1, endParen);

                                    // Extraire MAC après "à " ou "at "
                                    int atIndex = line.indexOf("à ", endParen);
                                    if (atIndex == -1)
                                        atIndex = line.indexOf("at ", endParen);

                                    if (atIndex != -1) {
                                        String afterAt = line
                                                .substring(atIndex + (line.charAt(atIndex + 1) == 't' ? 3 : 2));
                                        String[] parts = afterAt.trim().split("\\s+");
                                        if (parts.length > 0) {
                                            String mac = parts[0].replaceAll("[\\[\\]]", "");

                                            if (isValidIpAddress(ip) && isValidMacAddress(mac)) {
                                                result.putIfAbsent(mac.toUpperCase(), ip);
                                                logger.info("   ✅ arp -a: {} → {}", mac, ip);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    p.waitFor();

                } catch (Exception e) {
                    logger.warn("   ⚠️ arp -a échoué: {}", e.getMessage());
                }
            }

            // ===========================================
            // RÉSULTAT FINAL
            // ===========================================
            logger.info("📊 SCAN TERMINÉ: {} clients trouvés sur {}",
                    result.size(), networkInterface);

            if (!result.isEmpty()) {
                logger.info("📋 LISTE DES CLIENTS:");
                result.forEach((mac, ip) -> {
                    logger.info("   • {} → {}", mac, ip);
                });
            } else {
                logger.warn("⚠️ AUCUN client trouvé!");
                logger.warn("   Vérifiez que:");
                logger.warn("   - Vous êtes connecté au WiFi (interface {})", networkInterface);
                logger.warn("   - sudo arp-scan fonctionne: 'sudo arp-scan --localnet'");
                logger.warn("   - Le réseau est bien {} (votre IP: {})", subnet, getMyIp());
            }

        } catch (Exception e) {
            logger.error("❌ Erreur fatale lors du scan: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Récupère l'IP de l'interface
     */
    private String getMyIp() {
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "ip", "-4", "addr", "show", networkInterface });
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("inet ")) {
                        String[] parts = line.trim().split("\\s+");
                        String ipWithMask = parts[1];
                        return ipWithMask.split("/")[0];
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer
        }
        return "inconnue";
    }

    private Map<String, String> scanWithIpNeigh() {
        Map<String, String> result = new HashMap<>();
        try {
            // AVEC ta config sudoers, tu peux utiliser 'ip' sans sudo
            String[] cmd = { "ip", "neigh", "show", "dev", networkInterface };
            logger.debug("Exécution: {}", String.join(" ", cmd));

            Process p = Runtime.getRuntime().exec(cmd);

            // Lire le résultat
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("Ligne ip neigh: {}", line);

                    // Plusieurs formats possibles:
                    // 1. 192.168.0.1 dev wlo1 lladdr d8:42:f7:2a:20:4f REACHABLE
                    // 2. 192.168.0.1 lladdr d8:42:f7:2a:20:4f REACHABLE

                    String[] parts = line.trim().split("\\s+");
                    String ip = null;
                    String mac = null;

                    // Chercher l'IP (premier token qui ressemble à une IPv4)
                    for (String part : parts) {
                        if (isValidIpAddress(part)) {
                            ip = part;
                            break;
                        }
                    }

                    // Chercher le MAC (token avec : ou -)
                    for (String part : parts) {
                        if (isValidMacAddress(part)) {
                            mac = part;
                            break;
                        }
                    }

                    if (ip != null && mac != null) {
                        mac = mac.replaceAll("[()]", "").toUpperCase();
                        result.put(mac, ip);
                        logger.info("✅ Client trouvé: {} → {}", mac, ip);
                    }
                }
            }

            // Vérifier les erreurs
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()))) {
                String errorLine;
                boolean hasError = false;
                while ((errorLine = errorReader.readLine()) != null) {
                    logger.error("Erreur ip neigh: {}", errorLine);
                    hasError = true;
                }
                if (hasError) {
                    logger.warn("Problème avec 'ip neigh'. Vérifiez les droits.");
                }
            }

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                logger.warn("Commande 'ip neigh' a échoué avec code: {}", exitCode);
            }

        } catch (Exception e) {
            logger.error("Exception dans scanWithIpNeigh: {}", e.getMessage(), e);
        }

        logger.debug("scanWithIpNeigh trouvé {} clients", result.size());
        return result;
    }

    /**
     * Détecte le type d'appareil par son adresse MAC
     */
    public String detectDeviceType(String macAddress) {
        macAddress = macAddress.toUpperCase();

        // Détection par préfixe OUI
        if (macAddress.startsWith("D8:42:F7"))
            return "Routeur (TP-Link?)";
        if (macAddress.startsWith("7C:5C:F8"))
            return "PC Portable (HP)";
        if (macAddress.startsWith("00:11:22"))
            return "Equipement réseau";
        if (macAddress.startsWith("AA:BB:CC"))
            return "Appareil de test";

        // Détection par plage
        String firstOctet = macAddress.substring(0, 2);
        if (firstOctet.equals("00") || firstOctet.equals("08") || firstOctet.equals("0C")) {
            return "Equipement réseau";
        }
        if (firstOctet.equals("34") || firstOctet.equals("98") || firstOctet.equals("AC")) {
            return "Smartphone/Tablette";
        }
        if (firstOctet.equals("54") || firstOctet.equals("60") || firstOctet.equals("B8")) {
            return "PC/Ordinateur";
        }

        return "Inconnu";
    }

    // private Map<String, String> scanWithIpNeigh() {
    // Map<String, String> result = new HashMap<>();
    // try {
    // String[] cmd = { "ip", "neigh", "show", "dev", networkInterface };
    // Process p = Runtime.getRuntime().exec(cmd);

    // try (BufferedReader reader = new BufferedReader(
    // new InputStreamReader(p.getInputStream()))) {
    // String line;
    // while ((line = reader.readLine()) != null) {
    // // Format: 192.168.0.1 dev wlo1 lladdr d8:42:f7:2a:20:4f REACHABLE
    // String[] parts = line.split("\\s+");
    // if (parts.length >= 5) {
    // String ip = parts[0];
    // String mac = parts[4];

    // // Nettoyer le MAC (enlever les parenthèses si présentes)
    // mac = mac.replaceAll("[()]", "");

    // if (isValidIpAddress(ip) && isValidMacAddress(mac)) {
    // result.put(mac.toUpperCase(), ip);
    // logger.debug("Trouvé via ip neigh: {} -> {}", mac, ip);
    // }
    // }
    // }
    // }

    // p.waitFor();

    // } catch (Exception e) {
    // logger.warn("Erreur avec ip neigh: {}", e.getMessage());
    // }
    // return result;
    // }

    /**
     * Scan avec arp-scan (nécessite sudo mais c'est LE PLUS FIABLE)
     */
    private Map<String, String> scanWithArpScan() {
        Map<String, String> result = new HashMap<>();

        try {
            // Vérifier si arp-scan est installé
            Process check = Runtime.getRuntime().exec("which arp-scan");
            if (check.waitFor() != 0) {
                logger.warn("⚠️ arp-scan n'est pas installé. Installez-le avec: sudo apt-get install arp-scan");
                return result;
            }

            // ✅ AVEC sudo - fonctionne grâce à ta configuration sudoers
            String[] cmd = { "sudo", "arp-scan", "--interface=" + networkInterface,
                    "--localnet", "--quiet" };

            logger.debug("Exécution: {}", String.join(" ", cmd));
            Process p = Runtime.getRuntime().exec(cmd);

            // Lire la sortie
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Format: 192.168.43.71 80:a5:89:d5:8c:89 (Unknown)
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        String ip = parts[0];
                        String mac = parts[1];

                        // Ignorer les lignes de statistiques
                        if (isValidIpAddress(ip) && isValidMacAddress(mac)) {
                            result.put(mac.toUpperCase(), ip);
                            logger.debug("   ✅ arp-scan: {} → {}", mac, ip);
                        }
                    }
                }
            }

            // Lire les erreurs éventuelles
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    if (errorLine.contains("Permission denied")) {
                        logger.error("❌ Permission denied! Vérifie la config sudoers");
                    } else {
                        logger.warn("⚠️ arp-scan: {}", errorLine);
                    }
                }
            }

            p.waitFor();
            logger.info("   ✅ arp-scan terminé: {} clients trouvés", result.size());

        } catch (Exception e) {
            logger.error("❌ Erreur arp-scan: {}", e.getMessage());
        }

        return result;
    }

    
    // public Map<String, String> scan() {
    // Map<String, String> result = new HashMap<>();
    // try {
    // // Utiliser 'arp-scan' pour scanner le réseau
    // // Alternative : utiliser 'nmap' ou 'arping'
    // String[] cmd;

    // // Vérifier le système et utiliser la bonne commande
    // String os = System.getProperty("os.name").toLowerCase();
    // if (os.contains("linux")) {
    // cmd = new String[]{"arp-scan", "-l"};
    // } else if (os.contains("mac")) {
    // cmd = new String[]{"arp", "-a"};
    // } else {
    // logger.warn("Système d'exploitation non supporté pour ARP scanning");
    // return result;
    // }

    // ProcessBuilder pb = new ProcessBuilder(cmd);
    // pb.redirectErrorStream(true);
    // Process process = pb.start();

    // try (BufferedReader reader = new BufferedReader(
    // new InputStreamReader(process.getInputStream()))) {
    // String line;
    // while ((line = reader.readLine()) != null) {
    // // Parser la ligne pour extraire MAC et IP
    // String[] parts = line.split("\\s+");
    // if (parts.length >= 2) {
    // String ip = parts[0];
    // String mac = parts[1];

    // // Valider IP et MAC
    // if (isValidIpAddress(ip) && isValidMacAddress(mac)) {
    // result.put(mac.toUpperCase(), ip);
    // }
    // }
    // }
    // }

    // process.waitFor();
    // logger.debug("Scan ARP trouvé {} clients", result.size());

    // } catch (Exception e) {
    // logger.error("Erreur lors du scan ARP: {}", e.getMessage(), e);
    // }

    // return result;
    // }

    /**
     * Effectue un ping ARP sur une adresse IP spécifique
     */
    public boolean pingArp(String ipAddress) {
        try {
            String[] cmd = { "arping", "-c", "1", "-w", "1", ipAddress };
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            logger.debug("Erreur lors du ping ARP: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Récupère la table ARP système
     */
    public Map<String, String> getSystemArpTable() {
        Map<String, String> result = new HashMap<>();
        try {
            String[] cmd = { "arp", "-a" };
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher macMatcher = macPattern.matcher(line);
                    String[] parts = line.split("\\s+");

                    if (macMatcher.find() && parts.length >= 1) {
                        String mac = macMatcher.group();
                        String ip = parts[0].replaceAll("[()]", "");

                        if (isValidIpAddress(ip)) {
                            result.put(mac.toUpperCase(), ip);
                        }
                    }
                }
            }

            process.waitFor();

        } catch (Exception e) {
            logger.error("Erreur lors de la lecture de la table ARP: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Valide une adresse MAC
     */
    public boolean isValidMacAddress(String mac) {
        return macPattern.matcher(mac).matches();
    }

    /**
     * Valide une adresse IP
     */
    public boolean isValidIpAddress(String ip) {
        return ipPattern.matcher(ip).matches();
    }

    // Getters
    public String getNetworkInterface() {
        return networkInterface;
    }

    public String getSubnet() {
        return subnet;
    }
}
