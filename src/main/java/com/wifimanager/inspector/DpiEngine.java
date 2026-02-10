package com.wifimanager.inspector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Moteur de Deep Packet Inspection (DPI)
 * Analyse les paquets capturés pour détecter les services utilisés
 * (YouTube, Netflix, Streaming, Gaming, etc.)
 */
public class DpiEngine {

    // Cache des services détectés par client
    private final Map<String, Set<String>> clientServices = new ConcurrentHashMap<>();
    
    // Patterns de détection pour différents services
    private final Map<String, List<Pattern>> servicePatterns = new HashMap<>();
    
    // Ports standards par service
    private final Map<String, Set<Integer>> servicePorts = new HashMap<>();

    /**
     * Constructeur - Initialise les patterns de détection
     */
    public DpiEngine() {
        initializeServicePatterns();
        initializeServicePorts();
    }

    /**
     * Initialiser les patterns de détection pour chaque service
     */
    private void initializeServicePatterns() {
        // YouTube
        servicePatterns.put("YouTube", Arrays.asList(
            Pattern.compile("youtube\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("googlevideo\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ytimg\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/watch\\?v=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("youtube-nocookie\\.com", Pattern.CASE_INSENSITIVE)
        ));

        // Netflix
        servicePatterns.put("Netflix", Arrays.asList(
            Pattern.compile("netflix\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("nflxvideo\\.net", Pattern.CASE_INSENSITIVE),
            Pattern.compile("nflximg\\.net", Pattern.CASE_INSENSITIVE),
            Pattern.compile("nflxext\\.com", Pattern.CASE_INSENSITIVE)
        ));

        // Amazon Prime Video
        servicePatterns.put("Amazon Prime", Arrays.asList(
            Pattern.compile("primevideo\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("amazon.*video", Pattern.CASE_INSENSITIVE),
            Pattern.compile("amazonvideo\\.com", Pattern.CASE_INSENSITIVE)
        ));

        // Spotify
        servicePatterns.put("Spotify", Arrays.asList(
            Pattern.compile("spotify\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("scdn\\.co", Pattern.CASE_INSENSITIVE),
            Pattern.compile("spotifycdn\\.com", Pattern.CASE_INSENSITIVE)
        ));

        // Facebook
        servicePatterns.put("Facebook", Arrays.asList(
            Pattern.compile("facebook\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fbcdn\\.net", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fb\\.com", Pattern.CASE_INSENSITIVE)
        ));

        // Instagram
        servicePatterns.put("Instagram", Arrays.asList(
            Pattern.compile("instagram\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("cdninstagram\\.com", Pattern.CASE_INSENSITIVE)
        ));

        // TikTok
        servicePatterns.put("TikTok", Arrays.asList(
            Pattern.compile("tiktok\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("tiktokcdn\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("musical\\.ly", Pattern.CASE_INSENSITIVE)
        ));

        // Twitch
        servicePatterns.put("Twitch", Arrays.asList(
            Pattern.compile("twitch\\.tv", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ttvnw\\.net", Pattern.CASE_INSENSITIVE)
        ));

        // Gaming (générique)
        servicePatterns.put("Gaming", Arrays.asList(
            Pattern.compile("steam.*\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("epicgames\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("riot.*\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("battle\\.net", Pattern.CASE_INSENSITIVE)
        ));

        // VPN detection
        servicePatterns.put("VPN", Arrays.asList(
            Pattern.compile("openvpn", Pattern.CASE_INSENSITIVE),
            Pattern.compile("nordvpn", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expressvpn", Pattern.CASE_INSENSITIVE),
            Pattern.compile("wireguard", Pattern.CASE_INSENSITIVE)
        ));
    }

    /**
     * Initialiser les ports standards par service
     */
    private void initializeServicePorts() {
        servicePorts.put("HTTP", new HashSet<>(Arrays.asList(80, 8080, 8000)));
        servicePorts.put("HTTPS", new HashSet<>(Arrays.asList(443, 8443)));
        servicePorts.put("SMTP", new HashSet<>(Arrays.asList(25, 587, 465)));
        servicePorts.put("FTP", new HashSet<>(Arrays.asList(20, 21)));
        servicePorts.put("SSH", new HashSet<>(Arrays.asList(22)));
        servicePorts.put("DNS", new HashSet<>(Arrays.asList(53)));
        servicePorts.put("DHCP", new HashSet<>(Arrays.asList(67, 68)));
        servicePorts.put("Gaming", new HashSet<>(Arrays.asList(27015, 3074, 3478, 3479, 3480)));
    }

    /**
     * Analyser un paquet et détecter les services
     */
    public List<String> analyzePacket(Map<String, Object> packet) {
        List<String> detectedServices = new ArrayList<>();
        
        if (packet == null) {
            return detectedServices;
        }

        String macAddress = (String) packet.get("srcMac");
        String payload = (String) packet.get("payload");
        Integer dstPort = (Integer) packet.get("dstPort");

        // Analyser le payload pour détecter les patterns
        if (payload != null && !payload.isEmpty()) {
            for (Map.Entry<String, List<Pattern>> entry : servicePatterns.entrySet()) {
                String service = entry.getKey();
                List<Pattern> patterns = entry.getValue();
                
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(payload);
                    if (matcher.find()) {
                        detectedServices.add(service);
                        
                        // Mettre à jour le cache par client
                        if (macAddress != null) {
                            clientServices.computeIfAbsent(macAddress, k -> new HashSet<>())
                                .add(service);
                        }
                        break;
                    }
                }
            }
        }

        // Analyser les ports pour identification supplémentaire
        if (dstPort != null) {
            for (Map.Entry<String, Set<Integer>> entry : servicePorts.entrySet()) {
                String service = entry.getKey();
                Set<Integer> ports = entry.getValue();
                
                if (ports.contains(dstPort) && !detectedServices.contains(service)) {
                    detectedServices.add(service);
                }
            }
        }

        // Analyse heuristique basée sur la taille des paquets et les patterns de trafic
        analyzeTrafficPattern(packet, detectedServices);

        return detectedServices;
    }

    /**
     * Analyse heuristique des patterns de trafic
     */
    private void analyzeTrafficPattern(Map<String, Object> packet, List<String> services) {
        Integer size = (Integer) packet.get("size");
        String protocol = (String) packet.get("protocol");
        Integer dstPort = (Integer) packet.get("dstPort");

        if (size == null || protocol == null) {
            return;
        }

        // Détection de streaming vidéo (paquets larges, TCP, ports HTTPS)
        if ("TCP".equals(protocol) && size > 1200 && dstPort != null && dstPort == 443) {
            if (!services.contains("Video Streaming")) {
                services.add("Video Streaming");
            }
        }

        // Détection de VoIP (petits paquets UDP réguliers)
        if ("UDP".equals(protocol) && size > 100 && size < 300) {
            if (dstPort != null && (dstPort >= 5060 && dstPort <= 5090)) {
                services.add("VoIP");
            }
        }

        // Détection de P2P (ports élevés, trafic bidirectionnel)
        if (dstPort != null && dstPort > 6881 && dstPort < 6999) {
            services.add("P2P/Torrent");
        }
    }

    /**
     * Obtenir tous les services détectés pour un client
     */
    public List<String> detectServices(String macAddress) {
        Set<String> services = clientServices.get(macAddress);
        return services != null ? new ArrayList<>(services) : new ArrayList<>();
    }

    /**
     * Obtenir les statistiques d'utilisation des services
     */
    public Map<String, Integer> getServiceStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        for (Set<String> services : clientServices.values()) {
            for (String service : services) {
                stats.put(service, stats.getOrDefault(service, 0) + 1);
            }
        }
        
        return stats;
    }

    /**
     * Obtenir le nombre de clients utilisant un service spécifique
     */
    public int getClientCountForService(String service) {
        int count = 0;
        
        for (Set<String> services : clientServices.values()) {
            if (services.contains(service)) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * Réinitialiser le cache pour un client
     */
    public void clearClientCache(String macAddress) {
        clientServices.remove(macAddress);
    }

    /**
     * Réinitialiser tout le cache
     */
    public void clearAllCache() {
        clientServices.clear();
    }

    /**
     * Obtenir tous les clients utilisant un service spécifique
     */
    public List<String> getClientsUsingService(String service) {
        List<String> clients = new ArrayList<>();
        
        for (Map.Entry<String, Set<String>> entry : clientServices.entrySet()) {
            if (entry.getValue().contains(service)) {
                clients.add(entry.getKey());
            }
        }
        
        return clients;
    }

    /**
     * Vérifier si un client utilise un service spécifique
     */
    public boolean isClientUsingService(String macAddress, String service) {
        Set<String> services = clientServices.get(macAddress);
        return services != null && services.contains(service);
    }

    /**
     * Obtenir un rapport complet de l'utilisation des services
     */
    public Map<String, Object> getUsageReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("totalClients", clientServices.size());
        report.put("serviceStatistics", getServiceStatistics());
        report.put("timestamp", java.time.LocalDateTime.now().toString());
        
        // Top services
        Map<String, Integer> stats = getServiceStatistics();
        List<Map.Entry<String, Integer>> sortedStats = new ArrayList<>(stats.entrySet());
        sortedStats.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        List<String> topServices = new ArrayList<>();
        for (int i = 0; i < Math.min(5, sortedStats.size()); i++) {
            topServices.add(sortedStats.get(i).getKey());
        }
        report.put("topServices", topServices);
        
        return report;
    }
}
