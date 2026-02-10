package com.wifimanager.inspector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Module 3 - Analyse et Inspection du trafic réseau
 * Port: 8083
 * Fonctionnalités:
 * - Deep Packet Inspection (DPI)
 * - Détection services (YouTube, Netflix, etc.)
 * - Captive Portal
 */
@SpringBootApplication
@RestController
@RequestMapping("/api/inspector")
@EnableScheduling
public class InspectorMain {

    private final PacketSniffer packetSniffer;
    private final DpiEngine dpiEngine;
    private final CaptivePortal captivePortal;
    
    private final Map<String, Map<String, Object>> trafficStats = new ConcurrentHashMap<>();
    private volatile boolean isRunning = false;

    public InspectorMain() {
        this.packetSniffer = new PacketSniffer();
        this.dpiEngine = new DpiEngine();
        this.captivePortal = new CaptivePortal();
    }

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("    Module 3 - Inspector (Analyse DPI)");
        System.out.println("    Port: 8083");
        System.out.println("    Auteur: Larissa");
        System.out.println("==============================================");
        
        System.setProperty("server.port", "8083");
        SpringApplication.run(InspectorMain.class, args);
    }

    /**
     * Démarrer l'inspection du trafic
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startInspection() {
        if (isRunning) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", "error", "message", "Inspection déjà en cours"));
        }

        try {
            packetSniffer.start();
            isRunning = true;
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Inspection démarrée",
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Arrêter l'inspection du trafic
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopInspection() {
        if (!isRunning) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", "error", "message", "Inspection déjà arrêtée"));
        }

        try {
            packetSniffer.stop();
            isRunning = false;
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Inspection arrêtée",
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Obtenir les statistiques de trafic par client
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTrafficStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("clients", trafficStats);
        response.put("totalClients", trafficStats.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir les détails d'un client spécifique
     */
    @GetMapping("/stats/{macAddress}")
    public ResponseEntity<Map<String, Object>> getClientStats(@PathVariable String macAddress) {
        Map<String, Object> clientStats = trafficStats.get(macAddress);
        
        if (clientStats == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Client non trouvé"));
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "macAddress", macAddress,
            "stats", clientStats
        ));
    }

    /**
     * Détecter les services utilisés par un client
     */
    @GetMapping("/detect/{macAddress}")
    public ResponseEntity<Map<String, Object>> detectServices(@PathVariable String macAddress) {
        Map<String, Object> clientStats = trafficStats.get(macAddress);
        
        if (clientStats == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", "Client non trouvé"));
        }
        
        List<String> detectedServices = dpiEngine.detectServices(macAddress);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "macAddress", macAddress,
            "services", detectedServices,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Vérifier l'état du module
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "module", "Inspector",
            "port", 8083,
            "running", isRunning,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Rediriger un client vers le portail captif
     */
    @PostMapping("/redirect/{macAddress}")
    public ResponseEntity<Map<String, Object>> redirectToCaptivePortal(@PathVariable String macAddress) {
        try {
            String portalUrl = captivePortal.getPortalUrl(macAddress);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "macAddress", macAddress,
                "portalUrl", portalUrl,
                "message", "Client redirigé vers le portail captif"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Traitement périodique des paquets capturés (toutes les 10 secondes)
     */
    @Scheduled(fixedRate = 10000)
    public void processPackets() {
        if (!isRunning) {
            return;
        }

        try {
            // Récupérer les paquets capturés
            List<Map<String, Object>> packets = packetSniffer.getRecentPackets();
            
            // Analyser les paquets avec le DPI Engine
            for (Map<String, Object> packet : packets) {
                String macAddress = (String) packet.get("srcMac");
                if (macAddress == null) continue;
                
                // Mettre à jour les statistiques
                trafficStats.putIfAbsent(macAddress, new HashMap<>());
                Map<String, Object> stats = trafficStats.get(macAddress);
                
                // Incrémenter les compteurs
                stats.put("lastSeen", LocalDateTime.now().toString());
                stats.put("packetCount", (Integer) stats.getOrDefault("packetCount", 0) + 1);
                stats.put("bytesTransferred", 
                    (Long) stats.getOrDefault("bytesTransferred", 0L) + 
                    (Integer) packet.getOrDefault("size", 0));
                
                // Détecter les services
                List<String> services = dpiEngine.analyzePacket(packet);
                stats.put("detectedServices", services);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement des paquets: " + e.getMessage());
        }
    }

    /**
     * Nettoyer les anciennes statistiques (toutes les heures)
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupOldStats() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        trafficStats.entrySet().removeIf(entry -> {
            Map<String, Object> stats = entry.getValue();
            String lastSeenStr = (String) stats.get("lastSeen");
            if (lastSeenStr == null) return true;
            
            try {
                LocalDateTime lastSeen = LocalDateTime.parse(lastSeenStr, formatter);
                return lastSeen.isBefore(threshold);
            } catch (Exception e) {
                return true;
            }
        });
    }
}
