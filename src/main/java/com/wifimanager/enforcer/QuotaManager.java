package com.wifimanager.enforcer;

import com.wifimanager.shared.model.Quota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire central des quotas temps/données
 */
public class QuotaManager {
    private static final Logger logger = LoggerFactory.getLogger(QuotaManager.class);
    private static final String QUOTA_FILE = "data/quotas.json";

    private final Map<String, Quota> quotas; // MAC → Quota
    private boolean autoSave = true;
    private long lastSaveTime = 0;

    public QuotaManager() {
        this.quotas = new ConcurrentHashMap<>();
        ensureDataDirectory();
        loadFromFile();
        startAutoSaveThread();
        logger.info("QuotaManager initialisé. {} quotas chargés", quotas.size());
    }

    /**
     * Définit un nouveau quota pour un client
     */
    public synchronized Quota setQuota(String macAddress, int timeMinutes, int dataMB) {
        macAddress = macAddress.toUpperCase();

        Quota quota = new Quota(macAddress, timeMinutes, dataMB);
        quotas.put(macAddress, quota);

        logger.info("Nouveau quota défini: {} → {} min, {} MB",
                macAddress, timeMinutes, dataMB);

        if (autoSave) {
            saveToFile(true);
        }

        return quota;
    }

    /**
     * Récupère le quota d'un client
     */
    public Quota getQuota(String macAddress) {
        return quotas.get(macAddress.toUpperCase());
    }

    /**
     * Consomme du temps pour un client
     */
    public synchronized boolean consumeTime(String macAddress, int minutes) {
        Quota quota = getQuota(macAddress);
        if (quota == null || !quota.isActive()) {
            return false;
        }

        quota.consumeTime(minutes);

        if (quota.isExceeded()) {
            logger.warn("Quota temps dépassé pour {}: {} > {} minutes",
                    macAddress, quota.getTimeUsedMinutes(), quota.getTimeLimitMinutes());
        }

        if (autoSave) {
            saveToFile(true);
        }

        return true;
    }

    public synchronized boolean consumeData(String macAddress, int megabytes) {
        Quota quota = getQuota(macAddress);
        if (quota == null || !quota.isActive()) {
            return false;
        }

        quota.consumeData(megabytes);

        if (quota.isExceeded()) {
            logger.warn("Quota données dépassé pour {}: {} > {} MB",
                    macAddress, quota.getDataUsedMB(), quota.getDataLimitMB());
        }

        if (autoSave) {
            saveToFile(true);
        }

        return true;
    }

    /**
     * Vérifie si un client a dépassé son quota
     */
    public boolean isExceeded(String macAddress) {
        Quota quota = getQuota(macAddress);
        return quota != null && quota.isExceeded();
    }

    /**
     * Désactive un quota (sans le supprimer)
     */
    public synchronized void deactivateQuota(String macAddress) {
        Quota quota = getQuota(macAddress);
        if (quota != null) {
            quota.setActive(false);
            logger.info("Quota désactivé pour {}", macAddress);

            if (autoSave) {
                saveToFile(true);
            }
        }
    }

    /**
     * Réactive un quota
     */
    public synchronized void reactivateQuota(String macAddress) {
        Quota quota = getQuota(macAddress);
        if (quota != null) {
            quota.setActive(true);
            logger.info("Quota réactivé pour {}", macAddress);

            if (autoSave) {
                saveToFile(true);
            }
        }
    }

    /**
     * Supprime un quota
     */
    public synchronized void removeQuota(String macAddress) {
        quotas.remove(macAddress.toUpperCase());
        logger.info("Quota supprimé pour {}", macAddress);

        if (autoSave) {
            saveToFile(true);
        }
    }

    /**
     * Récupère tous les quotas
     */
    public List<Quota> getAllQuotas() {
        return new ArrayList<>(quotas.values());
    }

    /**
     * Récupère les quotas actifs
     */
    public List<Quota> getActiveQuotas() {
        List<Quota> active = new ArrayList<>();
        for (Quota quota : quotas.values()) {
            if (quota.isActive()) {
                active.add(quota);
            }
        }
        return active;
    }

    /**
     * Met à jour périodiquement le temps pour tous les quotas actifs
     */
    public synchronized void updateAllTimeQuotas() {
        int updated = 0;
        for (Quota quota : quotas.values()) {
            if (quota.isActive()) {
                quota.consumeTime(1); // 1 minute consommée
                updated++;
            }
        }

        if (updated > 0 && autoSave) {
            saveToFile(true);
        }

        logger.debug("Temps mis à jour pour {} quotas", updated);
    }

    // === Persistence ===

    private void ensureDataDirectory() {
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            logger.error("Impossible de créer le dossier data: {}", e.getMessage());
        }
    }

    public synchronized void saveToFile(boolean force) {
        try {
            // Éviter de sauvegarder trop souvent SAUF si force=true
            long now = System.currentTimeMillis();
            if (!force && now - lastSaveTime < 2000) { // 2 secondes minimum
                return;
            }

            List<Map<String, Object>> quotaList = new ArrayList<>();
            for (Quota quota : quotas.values()) {
                Map<String, Object> quotaMap = new HashMap<>();
                quotaMap.put("macAddress", quota.getMacAddress());
                quotaMap.put("timeLimitMinutes", quota.getTimeLimitMinutes());
                quotaMap.put("dataLimitMB", quota.getDataLimitMB());
                quotaMap.put("timeUsedMinutes", quota.getTimeUsedMinutes());
                quotaMap.put("dataUsedMB", quota.getDataUsedMB());
                quotaMap.put("startTime", quota.getStartTime().toString());
                quotaMap.put("lastUpdate", quota.getLastUpdate().toString());
                quotaMap.put("isActive", quota.isActive());
                quotaMap.put("quotaId", quota.getQuotaId());
                quotaList.add(quotaMap);
            }

            // Conversion JSON simple
            String json = toSimpleJson(quotaList);
            Files.write(Paths.get(QUOTA_FILE), json.getBytes());
            lastSaveTime = now;

            logger.debug("Quotas sauvegardés dans {}", QUOTA_FILE);

        } catch (Exception e) {
            logger.error("Erreur lors de la sauvegarde des quotas: {}", e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            Path filePath = Paths.get(QUOTA_FILE);
            if (!Files.exists(filePath)) {
                logger.info("Fichier quotas non trouvé: {}", QUOTA_FILE);
                return;
            }

            String json = new String(Files.readAllBytes(filePath));
            List<Map<String, Object>> quotaList = parseSimpleJson(json);

            quotas.clear();
            for (Map<String, Object> quotaMap : quotaList) {
                String mac = (String) quotaMap.get("macAddress");
                int timeLimit = ((Number) quotaMap.get("timeLimitMinutes")).intValue();
                int dataLimit = ((Number) quotaMap.get("dataLimitMB")).intValue();

                Quota quota = new Quota(mac, timeLimit, dataLimit);
                quota.setTimeUsedMinutes(((Number) quotaMap.get("timeUsedMinutes")).intValue());
                quota.setDataUsedMB(((Number) quotaMap.get("dataUsedMB")).intValue());
                quota.setActive((Boolean) quotaMap.get("isActive"));

                quotas.put(mac.toUpperCase(), quota);
            }

            logger.info("{} quotas chargés depuis {}", quotas.size(), QUOTA_FILE);

        } catch (Exception e) {
            logger.error("Erreur lors du chargement des quotas: {}", e.getMessage());
        }
    }

    // Thread de sauvegarde automatique
    private void startAutoSaveThread() {
        Thread saveThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // 30 secondes
                    if (autoSave) {
                        saveToFile(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        saveThread.setDaemon(true);
        saveThread.setName("QuotaAutoSave");
        saveThread.start();
    }

    // Méthodes JSON simples (sans dépendance externe)
    private String toSimpleJson(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            sb.append("  {\n");
            int j = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": ");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else if (value instanceof Boolean) {
                    sb.append(value);
                } else {
                    sb.append(value);
                }
                if (++j < map.size()) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  }");
            if (i < list.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    // private String toSimpleJson(List<Map<String, Object>> list) {
    // StringBuilder sb = new StringBuilder("[");
    // for (int i = 0; i < list.size(); i++) {
    // Map<String, Object> map = list.get(i);
    // sb.append("{");
    // int j = 0;
    // for (Map.Entry<String, Object> entry : map.entrySet()) {
    // sb.append("\"").append(entry.getKey()).append("\":");
    // Object value = entry.getValue();
    // if (value instanceof String) {
    // sb.append("\"").append(value).append("\"");
    // } else if (value instanceof Boolean) {
    // sb.append(value);
    // } else {
    // sb.append(value);
    // }
    // if (++j < map.size()) {
    // sb.append(",");
    // }
    // }
    // sb.append("}");
    // if (i < list.size() - 1) {
    // sb.append(",");
    // }
    // }
    // sb.append("]");
    // return sb.toString();
    // }

    // @SuppressWarnings("unchecked")

    private List<Map<String, Object>> parseSimpleJson(String json) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            json = json.trim();
            if (!json.startsWith("[") || !json.endsWith("]")) {
                logger.warn("JSON invalide: ne commence/termine pas par []");
                return result;
            }

            // Vider si JSON vide
            if (json.equals("[]")) {
                return result;
            }

            // Parsing plus robuste
            String content = json.substring(1, json.length() - 1).trim();
            if (content.isEmpty()) {
                return result;
            }

            // Séparer les objets (prend en compte les objets imbriqués)
            List<String> objects = new ArrayList<>();
            int braceCount = 0;
            StringBuilder current = new StringBuilder();

            for (char c : content.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
                current.append(c);

                if (c == '}' && braceCount == 0) {
                    objects.add(current.toString());
                    current = new StringBuilder();
                }
            }

            for (String obj : objects) {
                Map<String, Object> map = parseJsonObject(obj);
                if (!map.isEmpty()) {
                    result.add(map);
                }
            }

        } catch (Exception e) {
            logger.error("Erreur parsing JSON: {}", e.getMessage());
            logger.debug("JSON problématique: {}", json);
        }
        return result;
    }

    private Map<String, Object> parseJsonObject(String jsonObj) {
        Map<String, Object> map = new HashMap<>();
        try {
            // Nettoyer l'objet
            jsonObj = jsonObj.trim();
            if (jsonObj.startsWith("{")) {
                jsonObj = jsonObj.substring(1);
            }
            if (jsonObj.endsWith("}")) {
                jsonObj = jsonObj.substring(0, jsonObj.length() - 1);
            }
            jsonObj = jsonObj.trim();

            if (jsonObj.isEmpty()) {
                return map;
            }

            // Séparer les paires clé-valeur
            List<String> pairs = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inString = false;

            for (char c : jsonObj.toCharArray()) {
                if (c == '"') {
                    inString = !inString;
                }
                if (c == ',' && !inString) {
                    pairs.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) {
                pairs.add(current.toString().trim());
            }

            // Parser chaque paire
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim();

                    // Nettoyer la valeur
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                        map.put(key, value);
                    } else if (value.equals("true") || value.equals("false")) {
                        map.put(key, Boolean.parseBoolean(value));
                    } else if (value.matches("-?\\d+")) {
                        map.put(key, Integer.parseInt(value));
                    } else if (value.matches("-?\\d+\\.\\d+")) {
                        map.put(key, Double.parseDouble(value));
                    } else {
                        map.put(key, value);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Erreur parsing objet JSON: {}", e.getMessage());
        }
        return map;
    }

    // private List<Map<String, Object>> parseSimpleJson(String json) {
    // List<Map<String, Object>> result = new ArrayList<>();
    // try {
    // json = json.trim();
    // if (!json.startsWith("[") || !json.endsWith("]")) {
    // return result;
    // }

    // // Parsing très basique - pour développement seulement
    // // Dans un vrai projet, utiliser Jackson ou Gson
    // String content = json.substring(1, json.length() - 1).trim();
    // if (content.isEmpty()) {
    // return result;
    // }

    // // Séparer les objets
    // String[] objects = content.split("\\},\\s*\\{");
    // for (String obj : objects) {
    // obj = obj.replace("{", "").replace("}", "").trim();
    // Map<String, Object> map = new HashMap<>();

    // String[] pairs = obj.split(",\\s*");
    // for (String pair : pairs) {
    // String[] keyValue = pair.split(":\\s*");
    // if (keyValue.length == 2) {
    // String key = keyValue[0].replace("\"", "").trim();
    // String value = keyValue[1].replace("\"", "").trim();

    // // Convertir selon le type
    // if (value.equals("true") || value.equals("false")) {
    // map.put(key, Boolean.parseBoolean(value));
    // } else if (value.matches("\\d+")) {
    // map.put(key, Integer.parseInt(value));
    // } else {
    // map.put(key, value);
    // }
    // }
    // }
    // result.add(map);
    // }
    // } catch (Exception e) {
    // logger.error("Erreur parsing JSON: {}", e.getMessage());
    // }
    // return result;
    // }

    // Getters/Setters
    public Map<String, Quota> getQuotas() {
        return Collections.unmodifiableMap(quotas);
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }
}