package com.wifimanager.enforcer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des clients bloqués
 * Version 100% sans Jackson - Parsing JSON manuel
 */
public class BlockedClientManager {
    private static final Logger logger = LoggerFactory.getLogger(BlockedClientManager.class);
    
    private final Map<String, BlockedClient> blockedClients;
    private final String storageFile;
    private final DateTimeFormatter dateFormatter;
    
    public BlockedClientManager() {
        this("data/blocked_clients.json");
    }
    
    public BlockedClientManager(String storageFile) {
        this.blockedClients = new ConcurrentHashMap<>();
        this.storageFile = storageFile;
        this.dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        // Créer le dossier data s'il n'existe pas
        new File("data").mkdirs();
        
        // Charger les blocages existants
        loadBlockedClients();
        
        logger.info("📁 BlockedClientManager initialisé - Fichier: {}", storageFile);
    }
    
    /**
     * Ajouter un client à la liste des bloqués
     */
    public void blockClient(String macAddress, String ipAddress, String reason) {
        BlockedClient client = new BlockedClient(
            macAddress.toUpperCase(),
            ipAddress,
            reason,
            LocalDateTime.now()
        );
        
        blockedClients.put(macAddress.toUpperCase(), client);
        saveBlockedClients();
        
        logger.info("🚫 Client bloqué: {} - {} ({})", macAddress, ipAddress, reason);
    }
    
    /**
     * Retirer un client de la liste des bloqués
     */
    public boolean unblockClient(String macAddress) {
        BlockedClient removed = blockedClients.remove(macAddress.toUpperCase());
        if (removed != null) {
            saveBlockedClients();
            logger.info("✅ Client débloqué: {} - {}", macAddress, removed.getReason());
            return true;
        }
        return false;
    }
    
    /**
     * Vérifier si un client est bloqué
     */
    public boolean isBlocked(String macAddress) {
        return blockedClients.containsKey(macAddress.toUpperCase());
    }
    
    /**
     * Récupérer un client bloqué
     */
    public BlockedClient getBlockedClient(String macAddress) {
        return blockedClients.get(macAddress.toUpperCase());
    }
    
    /**
     * Récupérer tous les clients bloqués
     */
    public List<BlockedClient> getAllBlockedClients() {
        return new ArrayList<>(blockedClients.values());
    }
    
    /**
     * Nombre de clients bloqués
     */
    public int getBlockedCount() {
        return blockedClients.size();
    }
    
    /**
     * Sauvegarder la liste des bloqués en JSON (manuel)
     */
    private void saveBlockedClients() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(storageFile))) {
            writer.println("[");
            
            List<BlockedClient> clients = new ArrayList<>(blockedClients.values());
            for (int i = 0; i < clients.size(); i++) {
                BlockedClient client = clients.get(i);
                writer.print("  ");
                writer.print(client.toJson());
                if (i < clients.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }
            
            writer.println("]");
            logger.debug("💾 {} clients bloqués sauvegardés", blockedClients.size());
            
        } catch (IOException e) {
            logger.error("❌ Erreur sauvegarde clients bloqués: {}", e.getMessage());
        }
    }
    
    /**
     * Charger la liste des bloqués depuis JSON (manuel)
     */
    private void loadBlockedClients() {
        File file = new File(storageFile);
        if (!file.exists()) {
            logger.info("📂 Aucun fichier de blocage trouvé, démarrage avec liste vide");
            return;
        }
        
        try {
            String content = readFileAsString(file);
            blockedClients.clear();
            
            // Enlever les crochets et espaces
            content = content.trim();
            if (content.startsWith("[")) content = content.substring(1);
            if (content.endsWith("]")) content = content.substring(0, content.length() - 1);
            
            // Séparer les objets JSON
            if (content.trim().isEmpty()) {
                return;
            }
            
            // Découper les objets JSON en préservant les structures internes
            List<String> jsonObjects = new ArrayList<>();
            int braceCount = 0;
            int lastIndex = 0;
            
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;
                
                if (braceCount == 0 && (c == '}' || (i == content.length() - 1))) {
                    String obj = content.substring(lastIndex, i + 1).trim();
                    if (!obj.isEmpty() && obj.startsWith("{")) {
                        jsonObjects.add(obj);
                    }
                    lastIndex = i + 1;
                }
            }
            
            // Parser chaque objet JSON
            for (String json : jsonObjects) {
                try {
                    BlockedClient client = parseBlockedClient(json);
                    if (client != null && client.getMacAddress() != null) {
                        blockedClients.put(client.getMacAddress(), client);
                    }
                } catch (Exception e) {
                    logger.debug("Erreur parsing objet: {}", e.getMessage());
                }
            }
            
            logger.info("📂 {} clients bloqués chargés depuis {}", 
                blockedClients.size(), storageFile);
            
        } catch (Exception e) {
            logger.error("❌ Erreur chargement clients bloqués: {}", e.getMessage());
        }
    }
    
    /**
     * Parser un objet JSON en BlockedClient
     */
    private BlockedClient parseBlockedClient(String json) {
        try {
            String mac = extractJsonValue(json, "macAddress");
            String ip = extractJsonValue(json, "ipAddress");
            String reason = extractJsonValue(json, "reason");
            String since = extractJsonValue(json, "blockedSince");
            
            LocalDateTime blockedSince = since != null ? 
                LocalDateTime.parse(since, dateFormatter) : 
                LocalDateTime.now();
            
            return new BlockedClient(mac, ip, reason, blockedSince);
            
        } catch (Exception e) {
            logger.debug("Erreur parsing client: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extraire une valeur d'un JSON simple
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        
        start += pattern.length();
        if (start >= json.length()) return null;
        
        char firstChar = json.charAt(start);
        
        if (firstChar == '"') {
            // Valeur string
            start++;
            int end = json.indexOf('"', start);
            if (end != -1) {
                return json.substring(start, end);
            }
        } else {
            // Valeur nombre
            int end = json.indexOf(',', start);
            if (end == -1) end = json.indexOf('}', start);
            if (end != -1) {
                return json.substring(start, end).trim();
            }
        }
        return null;
    }
    
    /**
     * Lire un fichier en String
     */
    private String readFileAsString(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }
    
    /**
     * Nettoyer les blocages expirés
     */
    public void cleanExpiredBlockages(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int removed = 0;
        
        Iterator<BlockedClient> iterator = blockedClients.values().iterator();
        while (iterator.hasNext()) {
            BlockedClient client = iterator.next();
            if (client.getBlockedSince().isBefore(cutoff)) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            saveBlockedClients();
            logger.info("🧹 {} blocages expirés nettoyés (>{})", removed, days);
        }
    }
}

/**
 * Classe représentant un client bloqué
 */
class BlockedClient {
    private String macAddress;
    private String ipAddress;
    private String reason;
    private LocalDateTime blockedSince;
    private LocalDateTime lastSeen;
    private int blockCount;
    
    public BlockedClient() {}
    
    public BlockedClient(String macAddress, String ipAddress, String reason, LocalDateTime blockedSince) {
        this.macAddress = macAddress.toUpperCase();
        this.ipAddress = ipAddress;
        this.reason = reason;
        this.blockedSince = blockedSince;
        this.lastSeen = blockedSince;
        this.blockCount = 1;
    }
    
    // Getters et Setters
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress.toUpperCase(); }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public LocalDateTime getBlockedSince() { return blockedSince; }
    public void setBlockedSince(LocalDateTime blockedSince) { this.blockedSince = blockedSince; }
    
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    
    public int getBlockCount() { return blockCount; }
    public void setBlockCount(int blockCount) { this.blockCount = blockCount; }
    
    public String getFormattedBlockedSince() {
        if (blockedSince == null) return "-";
        return blockedSince.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    
    public String getDuration() {
        if (blockedSince == null) return "-";
        java.time.Duration duration = java.time.Duration.between(blockedSince, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%dh%02d", hours, minutes);
    }
    
    /**
     * Convertir en JSON (manuel)
     */
    public String toJson() {
        return String.format(
            "{\"macAddress\":\"%s\",\"ipAddress\":\"%s\",\"reason\":\"%s\",\"blockedSince\":\"%s\"}",
            macAddress, 
            ipAddress, 
            reason != null ? reason : "", 
            blockedSince != null ? blockedSince.toString() : LocalDateTime.now().toString()
        );
    }
    
    @Override
    public String toString() {
        return String.format("BlockedClient{mac=%s, ip=%s, reason=%s, since=%s}",
            macAddress, ipAddress, reason, blockedSince);
    }
}