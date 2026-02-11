package com.wifimanager.enforcer;

import com.sun.net.httpserver.HttpServer;
// import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serveur HTTP pour l'API du Module 2 (Enforcer)
 */
public class EnforcerApiServer {
    private static final Logger logger = LoggerFactory.getLogger(EnforcerApiServer.class);

    private final QuotaManager quotaManager;
    private final TrafficController trafficController;
    private final DisconnectionService disconnectionService;
    private final int port;
    private HttpServer server;

    public EnforcerApiServer(QuotaManager quotaManager, TrafficController trafficController,
            DisconnectionService disconnectionService, int port) {
        this.quotaManager = quotaManager;
        this.trafficController = trafficController;
        this.disconnectionService = disconnectionService;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Définir les routes
        server.createContext("/health", this::handleHealth);
        server.createContext("/quota/set", this::handleSetQuota);
        server.createContext("/quota/", this::handleGetQuota);
        server.createContext("/quota/consume", this::handleConsumeQuota);
        server.createContext("/disconnect/", this::handleDisconnect);
        server.createContext("/reconnect/", this::handleReconnect);
        server.createContext("/stats", this::handleStats);
        server.createContext("/rules/show", this::handleShowRules);
        server.createContext("/rules/reset", this::handleResetRules);

        server.setExecutor(null);
        server.start();

        logger.info("✅ Serveur API Module 2 démarré sur le port {}", port);
        logger.info("   Endpoints disponibles sur http://localhost:{}", port);
        logger.info("   - GET  /health                 → Vérifier état du module");
        logger.info("   - POST /quota/set              → Définir un quota");
        logger.info("   - GET  /quota/{mac}            → Consulter un quota");
        logger.info("   - POST /quota/consume          → Simuler consommation");
        logger.info("   - POST /disconnect/{mac}       → Déconnecter un client");
        logger.info("   - POST /reconnect/{mac}        → Reconnecter un client");
        logger.info("   - GET  /stats                  → Statistiques");
        logger.info("   - GET  /rules/show             → Afficher règles réseau");
        logger.info("   - POST /rules/reset            → Réinitialiser règles");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Serveur API Module 2 arrêté");
        }
    }

    // === Handlers ===

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("module", "enforcer");
        response.put("port", port);
        response.put("monitoring", disconnectionService.isMonitoringActive());

        sendJsonResponse(exchange, 200, response);
    }

    private void handleSetQuota(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        try {
            Map<String, Object> request = parseJsonRequest(exchange);
            logger.info("Requête quota reçue: {}", request);

            String macAddress = (String) request.get("mac");

            // Gestion robuste des nombres
            int timeMinutes = 0;
            int dataMB = 0;

            Object timeObj = request.get("timeMinutes");
            if (timeObj instanceof Number) {
                timeMinutes = ((Number) timeObj).intValue();
            } else if (timeObj instanceof String) {
                timeMinutes = Integer.parseInt((String) timeObj);
            }

            Object dataObj = request.get("dataMB");
            if (dataObj instanceof Number) {
                dataMB = ((Number) dataObj).intValue();
            } else if (dataObj instanceof String) {
                dataMB = Integer.parseInt((String) dataObj);
            }

            if (macAddress == null || macAddress.isEmpty()) {
                sendError(exchange, 400, "Adresse MAC requise");
                return;
            }

            if (timeMinutes <= 0 || dataMB <= 0) {
                sendError(exchange, 400,
                        "Les quotas doivent être > 0 (timeMinutes=" + timeMinutes + ", dataMB=" + dataMB + ")");
                return;
            }

            // Nettoyer l'adresse MAC
            macAddress = macAddress.toUpperCase().trim();

            com.wifimanager.shared.model.Quota quota = quotaManager.setQuota(macAddress, timeMinutes, dataMB);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Quota défini avec succès");
            response.put("macAddress", quota.getMacAddress());
            response.put("timeMinutes", quota.getTimeLimitMinutes());
            response.put("dataMB", quota.getDataLimitMB());
            response.put("quotaId", quota.getQuotaId());

            logger.info("Quota défini: {} - {}min, {}MB", macAddress, timeMinutes, dataMB);

            sendJsonResponse(exchange, 200, response);

        } catch (NumberFormatException e) {
            logger.error("Erreur de format numérique: {}", e.getMessage());
            sendError(exchange, 400, "Format de nombre invalide: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur lors du set quota: {}", e.getMessage(), e);
            sendError(exchange, 400, "Requête invalide: " + e.getMessage());
        }
    }

    private void handleGetQuota(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String macAddress = path.substring("/quota/".length());

        if (macAddress.isEmpty()) {
            // Retourner tous les quotas
            List<com.wifimanager.shared.model.Quota> allQuotas = quotaManager.getAllQuotas();

            Map<String, Object> response = new HashMap<>();
            response.put("count", allQuotas.size());
            response.put("quotas", allQuotas);

            sendJsonResponse(exchange, 200, response);
            return;
        }

        com.wifimanager.shared.model.Quota quota = quotaManager.getQuota(macAddress);

        if (quota == null) {
            sendError(exchange, 404, "Quota non trouvé pour " + macAddress);
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("macAddress", quota.getMacAddress());
        response.put("timeLimitMinutes", quota.getTimeLimitMinutes());
        response.put("dataLimitMB", quota.getDataLimitMB());
        response.put("timeUsedMinutes", quota.getTimeUsedMinutes());
        response.put("dataUsedMB", quota.getDataUsedMB());
        response.put("timeRemainingMinutes", quota.getTimeRemainingMinutes());
        response.put("dataRemainingMB", quota.getDataRemainingMB());
        response.put("isActive", quota.isActive());
        response.put("isExceeded", quota.isExceeded());
        response.put("startTime", quota.getStartTime().toString());

        sendJsonResponse(exchange, 200, response);
    }

    private void handleConsumeQuota(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        try {
            Map<String, Object> request = parseJsonRequest(exchange);
            String macAddress = (String) request.get("mac");
            int timeMinutes = ((Number) request.getOrDefault("timeMinutes", 0)).intValue();
            int dataMB = ((Number) request.getOrDefault("dataMB", 0)).intValue();

            if (macAddress == null) {
                sendError(exchange, 400, "Adresse MAC requise");
                return;
            }

            boolean timeConsumed = timeMinutes > 0 ? quotaManager.consumeTime(macAddress, timeMinutes) : false;
            boolean dataConsumed = dataMB > 0 ? quotaManager.consumeData(macAddress, dataMB) : false;

            Map<String, Object> response = new HashMap<>();
            response.put("success", timeConsumed || dataConsumed);
            response.put("timeConsumed", timeConsumed);
            response.put("dataConsumed", dataConsumed);
            response.put("isExceeded", quotaManager.isExceeded(macAddress));

            sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            sendError(exchange, 400, "Requête invalide: " + e.getMessage());
        }
    }

    private void handleDisconnect(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String macAddress = path.substring("/disconnect/".length());

        if (macAddress.isEmpty()) {
            sendError(exchange, 400, "Adresse MAC requise");
            return;
        }

        String reason = "MANUAL_DISCONNECT";
        disconnectionService.disconnectClient(macAddress, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Client déconnecté: " + macAddress);

        sendJsonResponse(exchange, 200, response);
    }

    private void handleReconnect(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String macAddress = path.substring("/reconnect/".length());

        if (macAddress.isEmpty()) {
            sendError(exchange, 400, "Adresse MAC requise");
            return;
        }

        disconnectionService.reconnectClient(macAddress);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Client reconnecté: " + macAddress);

        sendJsonResponse(exchange, 200, response);
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuotas", quotaManager.getAllQuotas().size());
        stats.put("activeQuotas", quotaManager.getActiveQuotas().size());
        stats.put("monitoringActive", disconnectionService.isMonitoringActive());
        stats.put("recentDisconnections",
                disconnectionService.getRecentDisconnections().size());
        stats.put("trafficControllerMode",
                trafficController.isSimulationMode() ? "SIMULATION" : "REAL");

        sendJsonResponse(exchange, 200, stats);
    }

    private void handleShowRules(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("simulationMode", trafficController.isSimulationMode());

        // Dans un vrai système, on récupérerait les règles actuelles
        // Pour l'instant, on simule
        if (trafficController.isSimulationMode()) {
            response.put("iptablesRules", "[SIMULATION] iptables -L -n -v");
            response.put("tcRules", "[SIMULATION] tc qdisc show");
        } else {
            response.put("iptablesRules", "Exécution réelle sur le système");
            response.put("tcRules", "Exécution réelle sur le système");
        }

        sendJsonResponse(exchange, 200, response);
    }

    private void handleResetRules(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        trafficController.resetAllRules();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Règles réseau réinitialisées");

        sendJsonResponse(exchange, 200, response);
    }

    // === Utilitaires ===

    private void sendJsonResponse(HttpExchange exchange, int code, Object data)
            throws IOException {

        // ✅ AJOUTE CES HEADERS CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        String response = toSimpleJson(data);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ✅ AJOUTE CE HANDLER POUR OPTIONS
    
    // private void handleOptions(HttpExchange exchange) throws IOException {
    //     exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    //     exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
    //     exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    //     exchange.sendResponseHeaders(204, -1);
    // }

    private void sendError(HttpExchange exchange, int code, String message)
            throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("code", code);
        error.put("message", message);

        sendJsonResponse(exchange, code, error);
    }

    // @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonRequest(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            String json = body.toString().trim();
            logger.debug("JSON reçu: {}", json);

            if (!json.startsWith("{") || !json.endsWith("}")) {
                throw new IOException("JSON invalide");
            }

            String content = json.substring(1, json.length() - 1).trim();
            Map<String, Object> result = new HashMap<>();

            if (!content.isEmpty()) {
                // Parse plus robuste qui gère les espaces et les guillemets
                String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim();

                        // Nettoyer la valeur
                        value = value.replaceAll("^\"|\"$", "");

                        // Conversion intelligente
                        if (value.matches("^\\d+$")) {
                            result.put(key, Integer.parseInt(value));
                        } else if (value.matches("^\\d+\\.\\d+$")) {
                            result.put(key, Double.parseDouble(value));
                        } else if (value.equals("true") || value.equals("false")) {
                            result.put(key, Boolean.parseBoolean(value));
                        } else {
                            result.put(key, value);
                        }

                        logger.debug("  {} -> {} ({})", key, value, result.get(key).getClass().getSimpleName());
                    }
                }
            }

            return result;
        }
    }

    private String toSimpleJson(Object obj) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(toSimpleJson(entry.getValue()));
                if (++i < map.size()) {
                    sb.append(",");
                }
            }
            sb.append("}");
            return sb.toString();
        } else if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(toSimpleJson(list.get(i)));
                if (i < list.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        } else if (obj instanceof String) {
            return "\"" + obj + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else {
            return "\"" + obj.toString() + "\"";
        }
    }
}