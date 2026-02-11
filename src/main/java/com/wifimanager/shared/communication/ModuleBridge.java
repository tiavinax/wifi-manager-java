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
                macAddress.toUpperCase(), timeMinutes, dataMB
            );
            
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
        if (start == -1) return "";
        
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
            if (end == -1) end = json.indexOf('}', start);
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
                macAddress, timeUsed, timeMinutes, dataUsed, dataMB, isExceeded
            );
        }
    }
}