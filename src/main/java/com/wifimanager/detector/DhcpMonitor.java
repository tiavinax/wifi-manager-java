package com.wifimanager.detector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Moniteur DHCP pour détecter les nouvelles connexions via les logs DHCP
 */
public class DhcpMonitor {
    private static final Logger logger = LoggerFactory.getLogger(DhcpMonitor.class);
    private static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})";
    private static final String IP_ADDRESS_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private final String leaseFile;
    private final ArpScanner arpScanner;
    private final Pattern macPattern;
    private final Pattern ipPattern;
    private long lastFilePosition = 0;
    private LocalDateTime lastCheck;

    public DhcpMonitor(String leaseFile, ArpScanner arpScanner) {
        this.leaseFile = leaseFile;
        this.arpScanner = arpScanner;
        this.macPattern = Pattern.compile(MAC_ADDRESS_REGEX);
        this.ipPattern = Pattern.compile(IP_ADDRESS_REGEX);
        this.lastCheck = LocalDateTime.now();
        logger.info("DHCP Monitor initialisé, fichier: {}", leaseFile);
    }

    public ArpScanner getArpScanner() {
        return arpScanner;
    }

    /**
     * Récupère les entrées DHCP depuis le dernier appel
     */
    public Map<String, String> getNewLeases() {
        Map<String, String> result = new HashMap<>();
        File file = new File(leaseFile);

        if (!file.exists()) {
            logger.warn("Fichier DHCP non trouvé: {}", leaseFile);
            return result;
        }

        try (RandomAccessFile raFile = new RandomAccessFile(file, "r")) {
            // Aller à la dernière position lue
            raFile.seek(lastFilePosition);

            String line;
            while ((line = raFile.readLine()) != null) {
                parseLeaseEntry(line, result);
            }

            // Mettre à jour la position
            lastFilePosition = raFile.getFilePointer();
            lastCheck = LocalDateTime.now();

        } catch (IOException e) {
            logger.error("Erreur lors de la lecture du fichier DHCP: {}", e.getMessage(), e);
        }

        logger.debug("Récupérées {} nouvelles entrées DHCP", result.size());
        return result;
    }

    /**
     * Récupère tous les baux DHCP actifs
     */
    public Map<String, String> getActiveLeases() {
        Map<String, String> result = new HashMap<>();
        File file = new File(leaseFile);

        if (!file.exists()) {
            logger.warn("Fichier DHCP non trouvé: {}", leaseFile);
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLeaseEntry(line, result);
            }
        } catch (IOException e) {
            logger.error("Erreur lors de la lecture du fichier DHCP: {}", e.getMessage(), e);
        }

        logger.debug("Trouvés {} baux DHCP actifs", result.size());
        return result;
    }

    /**
     * Parse une entrée du fichier de baux DHCP
     * Format typique: 192.168.1.100,AA:BB:CC:DD:EE:FF,hostname,client-id
     */
    private void parseLeaseEntry(String line, Map<String, String> result) {
        if (line == null || line.trim().isEmpty() || line.startsWith("#")) {
            return;
        }

        // Format ISC DHCP
        if (line.contains("hardware ethernet") || line.contains("client-hostname")) {
            Matcher macMatcher = macPattern.matcher(line);
            if (macMatcher.find()) {
                String mac = macMatcher.group();
                // Chercher l'IP juste avant
                String[] tokens = line.split("[\\s,]+");
                for (int i = 0; i < tokens.length - 1; i++) {
                    if (isValidIpAddress(tokens[i])) {
                        result.put(mac.toUpperCase(), tokens[i]);
                        break;
                    }
                }
            }
        }
        // Format dnsmasq (simple CSV)
        else if (line.contains(",")) {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                String ip = parts[0].trim();
                String mac = parts[1].trim();

                if (isValidIpAddress(ip) && isValidMacAddress(mac)) {
                    result.put(mac.toUpperCase(), ip);
                }
            }
        }
    }

    /**
     * Valide une adresse MAC
     */
    private boolean isValidMacAddress(String mac) {
        return macPattern.matcher(mac).matches();
    }

    /**
     * Valide une adresse IP
     */
    private boolean isValidIpAddress(String ip) {
        return ipPattern.matcher(ip).matches();
    }

    // Getters
    public String getLeaseFile() {
        return leaseFile;
    }

    public LocalDateTime getLastCheck() {
        return lastCheck;
    }
}
