package com.wifimanager.detector;

import com.wifimanager.shared.model.Client;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

/**
 * Serveur HTTP simple (sans Spark) pour exposer l'API de détection
 */
public class DetectorApiServer {
    private final ClientDiscoveryService discoveryService;
    private final int port;
    private HttpServer server;

    public DetectorApiServer(ClientDiscoveryService discoveryService, int port) {
        this.discoveryService = discoveryService;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Définir les routes
        server.createContext("/api/clients", this::handleGetClients);
        server.createContext("/api/clients/active", this::handleGetActiveClients);
        server.createContext("/api/stats", this::handleGetStats);
        server.createContext("/api/health", this::handleHealthCheck);

        server.setExecutor(null); // Utilise le thread par défaut
        server.start();

        System.out.println("✅ Serveur API démarré sur le port " + port);
        System.out.println("   Endpoints disponibles:");
        System.out.println("   - GET /api/clients");
        System.out.println("   - GET /api/clients/active");
        System.out.println("   - GET /api/stats");
        System.out.println("   - GET /api/health");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Serveur API arrêté");
        }
    }

    private void handleGetClients(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        // ✅ AJOUTE CES HEADERS CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        List<Client> clients = discoveryService.getAllClients();
        String response = formatClientsToJson(clients);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // private void handleOptions(HttpExchange exchange) throws IOException {
    //     exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    //     exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    //     exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    //     exchange.sendResponseHeaders(204, -1); // No content
    // }

    private void handleGetActiveClients(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        List<Client> clients = discoveryService.getActiveClients();
        String response = formatClientsToJson(clients);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private void handleGetStats(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", discoveryService.getTotalClientCount());
        stats.put("activeClients", discoveryService.getActiveClientCount());
        stats.put("networkInterface", discoveryService.getNetworkInterface());
        stats.put("subnet", discoveryService.getSubnet());

        String response = formatStatsToJson(stats);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private void handleHealthCheck(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        String response = "{\"status\": \"OK\", \"service\": \"detector\"}";

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String response = "{\"error\": \"" + message + "\", \"code\": " + code + "}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // Méthodes simples pour formater en JSON sans Jackson
    private String formatClientsToJson(List<Client> clients) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            sb.append(formatClientToJson(client));
            if (i < clients.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatClientToJson(Client client) {
        return String.format(
                "{\"macAddress\":\"%s\",\"ipAddress\":\"%s\",\"hostname\":\"%s\"," +
                        "\"active\":%s,\"bytesDownloaded\":%d,\"bytesUploaded\":%d," +
                        "\"totalBytes\":%d,\"firstSeen\":\"%s\",\"lastSeen\":\"%s\"}",
                client.getMacAddress(),
                client.getIpAddress(),
                client.getHostname() != null ? client.getHostname() : "",
                client.isActive(),
                client.getBytesDownloaded(),
                client.getBytesUploaded(),
                client.getTotalBytes(),
                client.getFirstSeen(),
                client.getLastSeen());
    }

    private String formatStatsToJson(Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
            if (++i < stats.size()) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}