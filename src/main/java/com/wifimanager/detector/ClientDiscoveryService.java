package com.wifimanager.detector;

import com.wifimanager.shared.config.Constants;
import com.wifimanager.shared.model.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service principal pour la découverte des clients sur le réseau
 */
public class ClientDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(ClientDiscoveryService.class);
    
    private final ArpScanner arpScanner;
    private final DhcpMonitor dhcpMonitor;
    private final String networkInterface;
    private final String subnet;
    
    // Cache pour les données des clients
    private final Map<String, Client> clientsCache;

    public ClientDiscoveryService() {
        this(Constants.DEFAULT_INTERFACE, Constants.DEFAULT_SUBNET);
    }

    public ClientDiscoveryService(String networkInterface, String subnet) {
        this.networkInterface = networkInterface;
        this.subnet = subnet;
        this.clientsCache = new HashMap<>();
        
        // Initialiser les composants
        this.arpScanner = new ArpScanner(networkInterface, subnet);
        
        // Déterminer le fichier DHCP en fonction de l'interface
        String dhcpFile = determineDhcpLeaseFile();
        this.dhcpMonitor = new DhcpMonitor(dhcpFile, arpScanner);
        
        logger.info("Service de découverte initialisé");
        logger.info("Interface réseau: {}", networkInterface);
        logger.info("Sous-réseau: {}", subnet);
        logger.info("Fichier DHCP: {}", dhcpFile);
    }

    /**
     * Détermine le fichier DHCP utilisé
     */
    private String determineDhcpLeaseFile() {
        String[] possibleFiles = {
            "/var/lib/dhcp/dhcpd.leases",      // dhcpd (ISC DHCP)
            "/var/lib/dhcpd/dhcpd.leases",     // dhcpd alternative
            "/var/db/dhcpd.leases",            // BSD
            "/var/lib/misc/dnsmasq.leases",    // dnsmasq
            "/tmp/dhcp.leases"                 // Test
        };

        for (String file : possibleFiles) {
            java.io.File f = new java.io.File(file);
            if (f.exists()) {
                logger.info("Fichier DHCP trouvé: {}", file);
                return file;
            }
        }
        
        logger.warn("Aucun fichier DHCP trouvé, utilisation de: {}", Constants.DHCP_LEASES_FILE);
        return Constants.DHCP_LEASES_FILE;
    }

    /**
     * Effectue une découverte complète des clients
     */
    public void discoverClients() {
        try {
            // Scanner l'ARP
            Map<String, String> arpResults = arpScanner.scan();
            
            // Récupérer les baux DHCP
            Map<String, String> dhcpLeases = dhcpMonitor.getActiveLeases();
            
            // Fusionner les résultats
            Set<String> allMacs = new HashSet<>();
            allMacs.addAll(arpResults.keySet());
            allMacs.addAll(dhcpLeases.keySet());
            
            for (String mac : allMacs) {
                String ip = arpResults.getOrDefault(mac, dhcpLeases.get(mac));
                
                if (ip != null) {
                    if (!clientsCache.containsKey(mac)) {
                        // Nouveau client
                        Client client = new Client(mac, ip);
                        clientsCache.put(mac, client);
                        logger.info("Nouveau client découvert: {} ({})", mac, ip);
                    } else {
                        // Mettre à jour le client existant
                        Client client = clientsCache.get(mac);
                        client.setIpAddress(ip);
                        client.setActive(true);
                        client.setLastSeen(LocalDateTime.now());
                    }
                }
            }
            
            // Marquer les clients inactifs
            markInactiveClients();
            
            logger.debug("Découverte complète: {} clients", clientsCache.size());
            
        } catch (Exception e) {
            logger.error("Erreur lors de la découverte des clients: {}", e.getMessage(), e);
        }
    }

    /**
     * Marque les clients qui ne répondent plus comme inactifs
     */
    private void markInactiveClients() {
        LocalDateTime timeout = LocalDateTime.now().minusSeconds(
            Constants.CLIENT_TIMEOUT / 1000
        );
        
        for (Client client : clientsCache.values()) {
            if (client.isActive() && client.getLastSeen().isBefore(timeout)) {
                client.setActive(false);
                logger.debug("Client marqué comme inactif: {}", client.getMacAddress());
            }
        }
    }

    /**
     * Obtient tous les clients
     */
    public List<Client> getAllClients() {
        discoverClients();
        return new ArrayList<>(clientsCache.values());
    }

    /**
     * Obtient les clients actifs
     */
    public List<Client> getActiveClients() {
        discoverClients();
        return clientsCache.values().stream()
            .filter(Client::isActive)
            .collect(Collectors.toList());
    }

    /**
     * Obtient un client par adresse MAC
     */
    public Client getClientByMac(String mac) {
        discoverClients();
        return clientsCache.get(mac.toUpperCase());
    }

    /**
     * Obtient un client par adresse IP
     */
    public Client getClientByIp(String ip) {
        discoverClients();
        return clientsCache.values().stream()
            .filter(c -> ip.equals(c.getIpAddress()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Met à jour le trafic d'un client
     */
    public void updateClientTraffic(String mac, long downloadBytes, long uploadBytes) {
        Client client = clientsCache.get(mac.toUpperCase());
        if (client != null) {
            client.setBytesDownloaded(client.getBytesDownloaded() + downloadBytes);
            client.setBytesUploaded(client.getBytesUploaded() + uploadBytes);
            client.setLastSeen(LocalDateTime.now());
            logger.debug("Trafic mis à jour pour {}: +{} down, +{} up", mac, downloadBytes, uploadBytes);
        }
    }

    /**
     * Marque un client comme déconnecté
     */
    public void markClientDisconnected(String mac) {
        Client client = clientsCache.get(mac.toUpperCase());
        if (client != null) {
            client.setActive(false);
            client.setLastSeen(LocalDateTime.now());
            logger.info("Client marqué déconnecté: {}", mac);
        }
    }

    /**
     * Compte total de clients
     */
    public int getTotalClientCount() {
        discoverClients();
        return clientsCache.size();
    }

    /**
     * Compte des clients actifs
     */
    public int getActiveClientCount() {
        discoverClients();
        return (int) clientsCache.values().stream()
            .filter(Client::isActive)
            .count();
    }

    // Getters
    public String getNetworkInterface() {
        return networkInterface;
    }

    public String getSubnet() {
        return subnet;
    }

    public Map<String, Client> getClientsCache() {
        return clientsCache;
    }
}
