package com.wifimanager.dashboard;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
// import java.nio.file.*;

public class SimpleHttpServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Handler principal
        server.createContext("/", new RootHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("\n" +
            "╔════════════════════════════════════════════════════════════╗\n" +
            "║         WiFi Manager - Dashboard (Module 4)              ║\n" +
            "║                    🎨 DESIGN PREMIUM                     ║\n" +
            "╠════════════════════════════════════════════════════════════╣\n" +
            "║  📍 http://localhost:8080                                 ║\n" +
            "║                                                            ║\n" +
            "║  📋 Pages disponibles :                                   ║\n" +
            "║  • 🏠 Accueil    : http://localhost:8080/                ║\n" +
            "║  • 👥 Clients    : http://localhost:8080/clients         ║\n" +
            "║  • ⚙️ Quotas     : http://localhost:8080/quotas          ║\n" +
            "║                                                            ║\n" +
            "║  ✅ Bootstrap: Chargé                                     ║\n" +
            "║  ✅ Icons: Chargé                                         ║\n" +
            "║  ✅ Dashboard CSS: Chargé                                 ║\n" +
            "║                                                            ║\n" +
            "║  🔴 Arrêter: Ctrl+C                                       ║\n" +
            "╚════════════════════════════════════════════════════════════╝\n");
    }
    
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // ROUTES PRINCIPALES
            if (path.equals("/")) {
                serveFile(exchange, "src/main/resources/templates/layout.html", "text/html; charset=utf-8");
                return;
            }
            
            if (path.equals("/index") || path.equals("/dashboard")) {
                serveFile(exchange, "src/main/resources/templates/index.html", "text/html; charset=utf-8");
                return;
            }
            
            if (path.equals("/clients")) {
                serveFile(exchange, "src/main/resources/templates/clients.html", "text/html; charset=utf-8");
                return;
            }
            
            if (path.equals("/quotas")) {
                serveFile(exchange, "src/main/resources/templates/quotas.html", "text/html; charset=utf-8");
                return;
            }
            
            // FICHIERS STATIQUES
            if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/fonts/")) {
                serveStaticFile(exchange, path);
                return;
            }
            
            // 404
            send404(exchange, path);
        }
        
        private void serveFile(HttpExchange exchange, String filePath, String contentType) throws IOException {
            File file = new File(filePath);
            if (file.exists()) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody();
                     FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(os);
                }
            } else {
                send404(exchange, filePath);
            }
        }
        
        private void serveStaticFile(HttpExchange exchange, String path) throws IOException {
            // Enlever le premier '/'
            String relativePath = path.substring(1);
            File file = new File("src/main/resources/static/" + relativePath);
            
            if (file.exists()) {
                String contentType = getContentType(file.getName());
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
            if (filename.endsWith(".css")) return "text/css; charset=utf-8";
            if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (filename.endsWith(".woff")) return "font/woff";
            if (filename.endsWith(".woff2")) return "font/woff2";
            if (filename.endsWith(".ttf")) return "font/ttf";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            return "text/plain";
        }
        
        private void send404(HttpExchange exchange, String path) throws IOException {
            String response = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                            "<title>404 - Page non trouvée</title>" +
                            "<link href='/css/bootstrap.min.css' rel='stylesheet'>" +
                            "<link href='/css/bootstrap-icons.css' rel='stylesheet'>" +
                            "</head><body class='bg-light'>" +
                            "<div class='container mt-5 text-center'>" +
                            "<div class='display-1 text-muted mb-4'>404</div>" +
                            "<h2 class='mb-4'>Page non trouvée</h2>" +
                            "<p class='lead mb-4'>Le fichier <code>" + path + "</code> n'existe pas.</p>" +
                            "<a href='/' class='btn btn-primary'>Retour à l'accueil</a>" +
                            "</div></body></html>";
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            byte[] bytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(404, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}