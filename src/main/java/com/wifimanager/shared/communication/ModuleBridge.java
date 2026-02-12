package com.wifimanager.shared.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Pont de communication entre les modules
 * Version compatible avec les APIs réelles des Module 1 et Module 2
 */
public class ModuleBridge {
    private static final Logger logger = LoggerFactory.getLogger(ModuleBridge.class);

    // Ports des modules
    public static final int DETECTOR_PORT = 8081;
    public static final int ENFORCER_PORT = 8082;

    /**
     * Récupérer la liste des clients depuis le Module 1
     * 
     * @return Liste des clients en format JSON ou tableau vide
     */
    public static String getClientsFromDetector() {
        try {
            URL url = new URL("http://localhost:" + DETECTOR_PORT + "/api/clients");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    logger.debug("Clients récupérés du Module 1: {} caractères", response.length());
                    return response.toString();
                }
            } else {
                logger.error("Module 1 répond avec code: {}", responseCode);
                return "[]";
            }
        } catch (Exception e) {
            logger.warn("Impossible de contacter Module 1: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * Définir un quota via le Module 2
     * 
     * @return QuotaResponse contenant les détails du quota créé
     */
    public static QuotaResponse setQuotaViaEnforcer(String macAddress, int timeMinutes, int dataMB) {
        QuotaResponse response = new QuotaResponse();
        response.success = false;

        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/quota/set");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Format exact attendu par le Module 2
            String jsonInput = String.format(
                    "{\"mac\":\"%s\",\"timeMinutes\":%d,\"dataMB\":%d}",
                    macAddress.toUpperCase(), timeMinutes, dataMB);

            logger.debug("Envoi requête quota à Module 2: {}", jsonInput);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            logger.debug("Module 2 répond avec code: {}", responseCode);

            if (responseCode == 200) {
                // Lire la réponse JSON
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    String jsonResponse = sb.toString();
                    logger.debug("Réponse Module 2: {}", jsonResponse);

                    // Parser la réponse
                    response.success = true;
                    response.message = extractJsonValue(jsonResponse, "message");
                    response.quotaId = extractJsonValue(jsonResponse, "quotaId");
                    response.macAddress = extractJsonValue(jsonResponse, "macAddress");

                    try {
                        response.timeMinutes = Integer.parseInt(
                                extractJsonValue(jsonResponse, "timeMinutes"));
                        response.dataMB = Integer.parseInt(
                                extractJsonValue(jsonResponse, "dataMB"));
                    } catch (Exception e) {
                        // Ignorer si les champs ne sont pas présents
                    }
                }
            } else {
                // Lire l'erreur
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    response.message = "Erreur " + responseCode + ": " + sb.toString();
                }
                logger.error("Erreur Module 2: {}", response.message);
            }

        } catch (Exception e) {
            logger.error("Erreur définition quota: {}", e.getMessage());
            response.message = "Exception: " + e.getMessage();
        }

        return response;
    }

    /**
     * Déconnecter un client via le Module 2
     * 
     * @return true si déconnexion réussie
     */
    public static boolean disconnectClientViaEnforcer(String macAddress) {
        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/disconnect/" +
                    macAddress.toUpperCase());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            boolean success = (responseCode == 200);

            if (success) {
                logger.info("Client déconnecté via Module 2: {}", macAddress);

                // Lire la réponse pour confirmation
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    logger.debug("Réponse déconnexion: {}", sb.toString());
                }
            } else {
                logger.warn("Échec déconnexion {}: code {}", macAddress, responseCode);
            }

            return success;

        } catch (Exception e) {
            logger.error("Erreur déconnexion client: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Reconnecter un client via le Module 2
     */
    public static boolean reconnectClientViaEnforcer(String macAddress) {
        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/reconnect/" +
                    macAddress.toUpperCase());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);

            int responseCode = conn.getResponseCode();
            return responseCode == 200;

        } catch (Exception e) {
            logger.error("Erreur reconnexion client: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Récupérer les statistiques du Module 2
     */
    public static Map<String, Object> getEnforcerStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/stats");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    String json = sb.toString();
                    stats.put("totalQuotas", extractJsonInt(json, "totalQuotas"));
                    stats.put("activeQuotas", extractJsonInt(json, "activeQuotas"));
                    stats.put("monitoringActive", extractJsonBoolean(json, "monitoringActive"));
                    stats.put("recentDisconnections", extractJsonInt(json, "recentDisconnections"));
                    stats.put("trafficControllerMode", extractJsonValue(json, "trafficControllerMode"));
                }
            }

        } catch (Exception e) {
            logger.error("Erreur récupération stats: {}", e.getMessage());
        }

        return stats;
    }

    /**
     * Récupérer un quota spécifique
     */
    public static QuotaResponse getQuotaFromEnforcer(String macAddress) {
        QuotaResponse response = new QuotaResponse();
        response.success = false;

        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/quota/" +
                    macAddress.toUpperCase());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    String json = sb.toString();
                    response.success = true;
                    response.macAddress = extractJsonValue(json, "macAddress");
                    response.timeMinutes = extractJsonInt(json, "timeLimitMinutes");
                    response.dataMB = extractJsonInt(json, "dataLimitMB");
                    response.timeUsed = extractJsonInt(json, "timeUsedMinutes");
                    response.dataUsed = extractJsonInt(json, "dataUsedMB");
                    response.timeRemaining = extractJsonInt(json, "timeRemainingMinutes");
                    response.dataRemaining = extractJsonInt(json, "dataRemainingMB");
                    response.isActive = extractJsonBoolean(json, "isActive");
                    response.isExceeded = extractJsonBoolean(json, "isExceeded");
                }
            }

        } catch (Exception e) {
            logger.error("Erreur récupération quota: {}", e.getMessage());
        }

        return response;
    }

    /**
     * Vérifier si un module est en ligne
     */
    public static boolean isModuleOnline(int port) {
        try {
            String healthEndpoint;
            if (port == DETECTOR_PORT) {
                healthEndpoint = "/api/health";
            } else if (port == ENFORCER_PORT) {
                healthEndpoint = "/health";
            } else {
                return false;
            }

            URL url = new URL("http://localhost:" + port + healthEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            return responseCode == 200;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Vérifier l'état de tous les modules
     */
    public static void checkAllModules() {
        System.out.println("🔍 Vérification des modules:");
        System.out.println("   Module 1 (Détection) - Port " + DETECTOR_PORT + ": " +
                (isModuleOnline(DETECTOR_PORT) ? "✅ En ligne" : "❌ Hors ligne"));
        System.out.println("   Module 2 (Enforcer) - Port " + ENFORCER_PORT + ": " +
                (isModuleOnline(ENFORCER_PORT) ? "✅ En ligne" : "❌ Hors ligne"));
    }

    // === Utilitaires de parsing JSON ===

    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1)
            return "";

        start += pattern.length();
        char firstChar = json.charAt(start);

        if (firstChar == '"') {
            // Valeur string
            start++;
            int end = json.indexOf('"', start);
            return end > start ? json.substring(start, end) : "";
        } else {
            // Valeur nombre ou booléen
            int end = json.indexOf(',', start);
            if (end == -1)
                end = json.indexOf('}', start);
            return end > start ? json.substring(start, end).trim() : "";
        }
    }

    private static int extractJsonInt(String json, String key) {
        try {
            return Integer.parseInt(extractJsonValue(json, key));
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean extractJsonBoolean(String json, String key) {
        try {
            return Boolean.parseBoolean(extractJsonValue(json, key));
        } catch (Exception e) {
            return false;
        }
    }

    // ===========================================
    // NOUVELLES MÉTHODES POUR CLIENTS BLOQUÉS
    // ===========================================

    /**
     * Récupérer la liste des clients bloqués depuis le Module 2
     * 
     * @return BlockedClientsResponse contenant la liste des clients bloqués
     */
    public static BlockedClientsResponse getBlockedClientsFromEnforcer() {
        BlockedClientsResponse response = new BlockedClientsResponse();
        response.success = false;
        response.blocked = new ArrayList<>();

        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/blocked");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    String json = sb.toString();
                    response.success = true;
                    response.count = extractJsonInt(json, "count");

                    // Extraire la liste des clients bloqués
                    String blockedArray = extractJsonArray(json, "blocked");
                    if (!blockedArray.isEmpty()) {
                        response.blocked = parseBlockedClients(blockedArray);
                    }

                    logger.debug("{} clients bloqués récupérés du Module 2", response.count);
                }
            } else {
                logger.warn("Module 2 répond avec code {} pour /blocked", responseCode);
            }

        } catch (Exception e) {
            logger.error("Erreur récupération clients bloqués: {}", e.getMessage());
        }

        return response;
    }

    /**
     * Vérifier si un client est bloqué
     * 
     * @return BlockedCheckResponse contenant le statut de blocage
     */
    public static BlockedCheckResponse isClientBlocked(String macAddress) {
        BlockedCheckResponse response = new BlockedCheckResponse();
        response.success = false;
        response.isBlocked = false;
        response.macAddress = macAddress.toUpperCase();

        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/blocked/" +
                    macAddress.toUpperCase());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    String json = sb.toString();
                    response.success = true;
                    response.isBlocked = extractJsonBoolean(json, "isBlocked");

                    // Extraire les détails si disponibles
                    String detailsJson = extractJsonObject(json, "details");
                    if (!detailsJson.isEmpty()) {
                        response.details = parseBlockedClient(detailsJson);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Erreur vérification blocage {}: {}", macAddress, e.getMessage());
        }

        return response;
    }

    /**
     * Débloquer un client via le Module 2
     * 
     * @return true si déblocage réussi
     */
    public static boolean unblockClientViaEnforcer(String macAddress) {
        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/unblock/" +
                    macAddress.toUpperCase());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            boolean success = (responseCode == 200);

            if (success) {
                logger.info("✅ Client débloqué via Module 2: {}", macAddress);

                // Lire la réponse pour confirmation
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    logger.debug("Réponse déblocage: {}", sb.toString());
                }
            } else {
                logger.warn("⚠️ Échec déblocage {}: code {}", macAddress, responseCode);
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ Erreur déblocage client {}: {}", macAddress, e.getMessage());
            return false;
        }
    }

    /**
     * Nettoyer les blocages expirés
     * 
     * @return true si nettoyage réussi
     */
    public static boolean cleanExpiredBlockages() {
        try {
            URL url = new URL("http://localhost:" + ENFORCER_PORT + "/blocked/clean");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);

            int responseCode = conn.getResponseCode();
            boolean success = (responseCode == 200);

            if (success) {
                logger.info("🧹 Blocages expirés nettoyés");
            }

            return success;

        } catch (Exception e) {
            logger.error("❌ Erreur nettoyage blocages: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Récupérer le nombre de clients bloqués
     */
    public static int getBlockedCount() {
        BlockedClientsResponse response = getBlockedClientsFromEnforcer();
        return response.success ? response.count : 0;
    }

    // ===========================================
    // UTILITAIRES DE PARSING JSON (AJOUTS)
    // ===========================================

    /**
     * Extraire un tableau JSON
     */
    private static String extractJsonArray(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1)
            return "[]";

        start += pattern.length();
        if (start >= json.length())
            return "[]";

        char firstChar = json.charAt(start);
        if (firstChar != '[')
            return "[]";

        int braceCount = 1;
        int end = start + 1;

        while (end < json.length() && braceCount > 0) {
            char c = json.charAt(end);
            if (c == '[')
                braceCount++;
            if (c == ']')
                braceCount--;
            end++;
        }

        return json.substring(start, end);
    }

    /**
     * Extraire un objet JSON
     */
    private static String extractJsonObject(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1)
            return "";

        start += pattern.length();
        if (start >= json.length())
            return "";

        char firstChar = json.charAt(start);
        if (firstChar != '{')
            return "";

        int braceCount = 1;
        int end = start + 1;

        while (end < json.length() && braceCount > 0) {
            char c = json.charAt(end);
            if (c == '{')
                braceCount++;
            if (c == '}')
                braceCount--;
            end++;
        }

        return json.substring(start, end);
    }

    /**
     * Parser une liste de clients bloqués depuis un tableau JSON
     */
    private static List<BlockedClient> parseBlockedClients(String jsonArray) {
        List<BlockedClient> clients = new ArrayList<>();

        if (jsonArray.isEmpty() || jsonArray.equals("[]")) {
            return clients;
        }

        // Enlever les crochets
        String content = jsonArray.substring(1, jsonArray.length() - 1).trim();
        if (content.isEmpty()) {
            return clients;
        }

        // Séparer les objets JSON
        List<String> jsonObjects = new ArrayList<>();
        int braceCount = 0;
        int lastIndex = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{')
                braceCount++;
            if (c == '}')
                braceCount--;

            if (braceCount == 0 && c == '}') {
                String obj = content.substring(lastIndex, i + 1).trim();
                if (!obj.isEmpty()) {
                    jsonObjects.add(obj);
                }
                lastIndex = i + 1;
            }
        }

        // Parser chaque objet
        for (String jsonObj : jsonObjects) {
            try {
                BlockedClient client = parseBlockedClient(jsonObj);
                if (client != null) {
                    clients.add(client);
                }
            } catch (Exception e) {
                logger.debug("Erreur parsing client bloqué: {}", e.getMessage());
            }
        }

        return clients;
    }

    /**
     * Parser un client bloqué depuis un objet JSON
     */
    private static BlockedClient parseBlockedClient(String json) {
        if (json.isEmpty() || json.equals("{}")) {
            return null;
        }

        BlockedClient client = new BlockedClient();
        client.macAddress = extractJsonValue(json, "macAddress");
        client.ipAddress = extractJsonValue(json, "ipAddress");
        client.reason = extractJsonValue(json, "reason");
        client.blockedSince = extractJsonValue(json, "blockedSince");

        try {
            client.timeRemaining = extractJsonInt(json, "timeRemaining");
        } catch (Exception e) {
            client.timeRemaining = 0;
        }

        return client;
    }

    // ===========================================
    // NOUVELLES CLASSES DE RÉPONSE
    // ===========================================

    /**
     * Réponse pour la liste des clients bloqués
     */
    public static class BlockedClientsResponse {
        public boolean success;
        public int count;
        public List<BlockedClient> blocked;

        public BlockedClientsResponse() {
            this.success = false;
            this.count = 0;
            this.blocked = new ArrayList<>();
        }
    }

    /**
     * Réponse pour la vérification de blocage
     */
    public static class BlockedCheckResponse {
        public boolean success;
        public boolean isBlocked;
        public String macAddress;
        public BlockedClient details;

        public BlockedCheckResponse() {
            this.success = false;
            this.isBlocked = false;
            this.macAddress = "";
            this.details = null;
        }
    }

    /**
     * Classe représentant un client bloqué
     */
    public static class BlockedClient {
        public String macAddress;
        public String ipAddress;
        public String reason;
        public String blockedSince;
        public int timeRemaining;

        public BlockedClient() {
            this.macAddress = "";
            this.ipAddress = "";
            this.reason = "";
            this.blockedSince = "";
            this.timeRemaining = 0;
        }

        public String getFormattedBlockedSince() {
            if (blockedSince == null || blockedSince.isEmpty())
                return "-";
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(blockedSince);
                return dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (Exception e) {
                return blockedSince;
            }
        }

        public String getDuration() {
            if (blockedSince == null || blockedSince.isEmpty())
                return "-";
            try {
                java.time.LocalDateTime since = java.time.LocalDateTime.parse(blockedSince);
                java.time.Duration duration = java.time.Duration.between(since, java.time.LocalDateTime.now());
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                return String.format("%dh%02d", hours, minutes);
            } catch (Exception e) {
                return "-";
            }
        }

        @Override
        public String toString() {
            return String.format("BlockedClient{mac=%s, ip=%s, reason=%s, since=%s}",
                    macAddress, ipAddress, reason, blockedSince);
        }
    }

    // === Classes de réponse ===

    public static class QuotaResponse {
        public boolean success;
        public String message;
        public String quotaId;
        public String macAddress;
        public int timeMinutes;
        public int dataMB;
        public int timeUsed;
        public int dataUsed;
        public int timeRemaining;
        public int dataRemaining;
        public boolean isActive;
        public boolean isExceeded;

        public QuotaResponse() {
            this.success = false;
            this.message = "";
            this.quotaId = "";
            this.macAddress = "";
        }

        @Override
        public String toString() {
            return String.format(
                    "Quota{mac=%s, time=%d/%d, data=%d/%d, exceeded=%s}",
                    macAddress, timeUsed, timeMinutes, dataUsed, dataMB, isExceeded);
        }
    }
}