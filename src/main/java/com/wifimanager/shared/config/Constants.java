package com.wifimanager.shared.config;

/**
 * Constantes réseau et configuration partagées
 */
public class Constants {
    // ===== CONFIGURATION RÉSEAU =====
    public static final String DEFAULT_INTERFACE = "wlo1";
    public static final String DEFAULT_SUBNET = "192.168.1.0/24";
    
    // ===== PORTS DES MODULES =====
    public static final int DETECTOR_PORT = 8081;
    public static final int ENFORCER_PORT = 8082;
    public static final int INSPECTOR_PORT = 8083;
    public static final int DASHBOARD_PORT = 8080;
    
    // ===== API ENDPOINTS =====
    public static final String API_DETECTOR_BASE = "http://localhost:8081/api";
    public static final String API_ENFORCER_BASE = "http://localhost:8082/api";
    public static final String API_INSPECTOR_BASE = "http://localhost:8083/api";
    
    // ===== CONFIGURATION DHCP =====
    // public static final String DHCP_LEASES_FILE = "/var/lib/dhcp/dhcpd.leases";
    public static final String DHCP_LEASES_FILE = "/var/lib/misc/dnsmasq.leases";
    public static final long DHCP_MONITOR_INTERVAL = 5000; // 5 secondes
    
    // ===== SCAN ARP =====
    public static final long ARP_SCAN_INTERVAL = 30000; // 30 secondes
    public static final int ARP_PING_TIMEOUT = 1000; // 1 seconde
    public static final String ARP_BROADCAST_ADDR = "255.255.255.255";
    
    // ===== CLIENT TIMEOUT =====
    public static final long CLIENT_TIMEOUT = 60000; // 60seconde
    
    // ===== BASE DE DONNÉES =====
    public static final String DB_FILE = "data/wifimanager.db";
    public static final String DB_DRIVER = "org.sqlite.JDBC";
    public static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    
    // ===== LOGS =====
    public static final String LOG_FILE = "logs/app.log";
    
    // ===== FORMATS =====
    public static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})";
    public static final String IP_ADDRESS_REGEX = 
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    
    // ===== MESSAGES =====
    public static final String MSG_CLIENT_CONNECTED = "Client connecté";
    public static final String MSG_CLIENT_DISCONNECTED = "Client déconnecté";
    public static final String MSG_ERROR = "Erreur: ";

    // ===== AUTRES ======
    public static final String APP_NAME = "WiFi Manager";
    public static final String VERSION = "1.0.0";
    
    // Empêcher l'instanciation
    private Constants() {
        throw new AssertionError("Impossible d'instancier Constants");
    }
}
