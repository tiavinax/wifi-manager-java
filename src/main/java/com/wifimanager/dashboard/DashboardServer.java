package com.wifimanager.dashboard;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DashboardServer {

    private static final int DETECTOR_PORT = 8081;
    private static final int ENFORCER_PORT = 8082;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ========== PAGES HTML ==========
        server.createContext("/", new PageHandler("layout.html"));
        server.createContext("/index", new PageHandler("index.html"));
        server.createContext("/clients", new PageHandler("clients.html"));
        server.createContext("/quotas", new PageHandler("quotas.html"));
        server.createContext("/stats", new PageHandler("stats.html"));

        // ========== FICHIERS STATIQUES ==========
        server.createContext("/css", new StaticHandler("src/main/resources/static/css"));
        server.createContext("/js", new StaticHandler("src/main/resources/static/js"));
        server.createContext("/fonts", new StaticHandler("src/main/resources/static/fonts"));
        server.createContext("/bootstrap-icons", new StaticHandler("src/main/resources/static/bootstrap-icons"));

        // ========== PROXY API VERS MODULE 1 (DETECTOR) ==========
        // ✅ CORRECTION: Mappe /api/detector/ → http://localhost:8081/
        server.createContext("/api/detector/", new ProxyHandler("http://localhost:" + DETECTOR_PORT));

        // ========== PROXY API VERS MODULE 2 (ENFORCER) ==========
        // ✅ CORRECTION: Mappe /api/enforcer/ → http://localhost:8082/
        server.createContext("/api/enforcer/", new ProxyHandler("http://localhost:" + ENFORCER_PORT));

        server.setExecutor(null);
        server.start();

        System.out.println("\n" +
                "╔════════════════════════════════════════════════════════════╗\n" +
                "║         WiFi Manager - Dashboard (Module 4)              ║\n" +
                "║                    🚀 SERVEUR HTTP                       ║\n" +
                "╠════════════════════════════════════════════════════════════╣\n" +
                "║  📍 http://localhost:8080                                 ║\n" +
                "║                                                            ║\n" +
                "║  📋 Pages :                                               ║\n" +
                "║  • 🏠 Accueil    : http://localhost:8080/               ║\n" +
                "║  • 👥 Clients    : http://localhost:8080/clients        ║\n" +
                "║  • ⚙️ Quotas     : http://localhost:8080/quotas         ║\n" +
                "║                                                            ║\n" +
                "║  🔌 Proxy APIs :                                          ║\n" +
                "║  • Module 1 : /api/detector/* → http://localhost:8081  ║\n" +
                "║  • Module 2 : /api/enforcer/* → http://localhost:8082  ║\n" +
                "║                                                            ║\n" +
                "║  📡 Exemples :                                            ║\n" +
                "║  • GET  /api/detector/api/clients                       ║\n" +
                "║  • GET  /api/enforcer/quota/                            ║\n" +
                "║  • POST /api/enforcer/quota/set                         ║\n" +
                "║  • POST /api/enforcer/disconnect/AA:BB:CC:DD:EE:FF      ║\n" +
                "║                                                            ║\n" +
                "║  🔴 Arrêter: Ctrl+C                                       ║\n" +
                "╚════════════════════════════════════════════════════════════╝\n");
    }

    // ========== HANDLER PAGES HTML ==========
    static class PageHandler implements HttpHandler {
        private final String pageName;

        public PageHandler(String pageName) {
            this.pageName = pageName;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String filePath = "src/main/resources/templates/" + pageName;
            File file = new File(filePath);

            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

                // Injecter le bon fichier JS
                if (pageName.equals("clients.html")) {
                    content = content.replace("<!-- PAGE_SCRIPT -->",
                            "<script src='/js/clients.js'></script>");
                } else if (pageName.equals("quotas.html")) {
                    content = content.replace("<!-- PAGE_SCRIPT -->",
                            "<script src='/js/quotas.js'></script>");
                } else {
                    content = content.replace("<!-- PAGE_SCRIPT -->", "");
                }

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                send404(exchange, pageName);
            }
        }
    }

    // ========== HANDLER FICHIERS STATIQUES ==========
    static class StaticHandler implements HttpHandler {
        private final String basePath;

        public StaticHandler(String basePath) {
            this.basePath = basePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            File file = new File(basePath, filename);

            if (file.exists()) {
                String contentType = getContentType(filename);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody();
                        FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(os);
                }
            } else {
                send404(exchange, path);
            }
        }

        private String getContentType(String filename) {
            if (filename.endsWith(".css"))
                return "text/css; charset=utf-8";
            if (filename.endsWith(".js"))
                return "application/javascript; charset=utf-8";
            if (filename.endsWith(".woff"))
                return "font/woff";
            if (filename.endsWith(".woff2"))
                return "font/woff2";
            if (filename.endsWith(".ttf"))
                return "font/ttf";
            if (filename.endsWith(".svg"))
                return "image/svg+xml";
            return "text/plain";
        }
    }

    // ========== HANDLER PROXY API - CORRIGÉ ! ==========
    static class ProxyHandler implements HttpHandler {
        private final String targetBaseUrl;

        public ProxyHandler(String targetBaseUrl) {
            this.targetBaseUrl = targetBaseUrl;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();

                // ✅ CORRECTION: Déterminer le chemin cible selon le module
                String targetUrl;

                if (path.startsWith("/api/detector/")) {
                    // Module 1: /api/detector/api/clients → http://localhost:8081/api/clients
                    // /api/detector/api/health → http://localhost:8081/api/health
                    String remainingPath = path.substring("/api/detector".length()); // Enlever /api/detector
                    targetUrl = targetBaseUrl + remainingPath;

                } else if (path.startsWith("/api/enforcer/")) {
                    // Module 2: /api/enforcer/health → http://localhost:8082/health
                    // /api/enforcer/quota/ → http://localhost:8082/quota/
                    String remainingPath = path.substring("/api/enforcer".length()); // Enlever /api/enforcer
                    targetUrl = targetBaseUrl + remainingPath;

                } else {
                    send404(exchange, path);
                    return;
                }

                System.out.println("🔄 PROXY: " + method + " " + path + " → " + targetUrl);

                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // Headers CORS
                conn.setRequestProperty("Origin", "http://localhost:8080");

                // Copier les headers
                exchange.getRequestHeaders().forEach((key, values) -> {
                    if (!key.equalsIgnoreCase("Host") && !key.equalsIgnoreCase("Origin")) {
                        values.forEach(value -> conn.setRequestProperty(key, value));
                    }
                });

                // Body pour POST
                if (method.equals("POST") || method.equals("PUT")) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream();
                            InputStream is = exchange.getRequestBody()) {
                        is.transferTo(os);
                    }
                }

                // Lire la réponse
                int responseCode = conn.getResponseCode();
                InputStream responseStream = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();

                // Headers CORS pour la réponse
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

                // Copier les headers de réponse
                conn.getHeaderFields().forEach((key, values) -> {
                    if (key != null && !key.equalsIgnoreCase("Transfer-Encoding")) {
                        values.forEach(value -> exchange.getResponseHeaders().add(key, value));
                    }
                });

                // Envoyer la réponse
                exchange.sendResponseHeaders(responseCode, 0);
                try (OutputStream os = exchange.getResponseBody();
                        InputStream is = responseStream) {
                    is.transferTo(os);
                }

                System.out.println("   ✅ Réponse: " + responseCode);

            } catch (Exception e) {
                System.err.println("   ❌ Erreur proxy: " + e.getMessage());
                e.printStackTrace();

                String error = "{\"error\":\"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(502, error.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        }
    }

    private static void send404(HttpExchange exchange, String path) throws IOException {
        String response = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                "<title>404 - Page non trouvée</title>" +
                "<link href='/css/bootstrap.min.css' rel='stylesheet'>" +
                "<link href='/bootstrap-icons/font/bootstrap-icons.css' rel='stylesheet'>" +
                "</head><body class='bg-light'>" +
                "<div class='container mt-5 text-center'>" +
                "<div class='display-1 text-muted mb-4'>404</div>" +
                "<h2 class='mb-4'>Page non trouvée</h2>" +
                "<p class='lead mb-4'>Le fichier <code>" + path + "</code> n'existe pas.</p>" +
                "<a href='/' class='btn btn-primary'>Retour à l'accueil</a>" +
                "</div></body></html>";

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}