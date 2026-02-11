package com.wifimanager.enforcer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de déconnexion automatique basé sur les quotas
 */
public class DisconnectionService {
    private static final Logger logger = LoggerFactory.getLogger(DisconnectionService.class);
    
    private final QuotaManager quotaManager;
    private final TrafficController trafficController;
    private final Map<String, DisconnectionEvent> disconnectionHistory;
    private boolean monitoringActive = false;
    private Thread monitoringThread;
    
    public DisconnectionService(QuotaManager quotaManager, TrafficController trafficController) {
        this.quotaManager = quotaManager;
        this.trafficController = trafficController;
        this.disconnectionHistory = new ConcurrentHashMap<>();
        logger.info("DisconnectionService initialisé");
    }
    
    /**
     * Démarrer la surveillance automatique
     */
    public synchronized void startMonitoring() {
        if (monitoringActive) {
            logger.warn("Surveillance déjà active");
            return;
        }
        
        monitoringActive = true;
        monitoringThread = new Thread(this::monitoringLoop);
        monitoringThread.setName("DisconnectionMonitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
        
        logger.info("Surveillance des déconnexions démarrée");
    }
    
    /**
     * Arrêter la surveillance
     */
    public synchronized void stopMonitoring() {
        monitoringActive = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
            monitoringThread = null;
        }
        logger.info("Surveillance des déconnexions arrêtée");
    }
    
    /**
     * Boucle principale de surveillance
     */
    private void monitoringLoop() {
        logger.info("Démarrage boucle de surveillance (intervalle: 30s)");
        
        while (monitoringActive) {
            try {
                Thread.sleep(30000); // 30 secondes
                checkAndDisconnect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Erreur dans la boucle de surveillance: {}", e.getMessage());
            }
        }
        
        logger.info("Boucle de surveillance terminée");
    }
    
    /**
     * Vérifie tous les quotas et déconnecte si nécessaire
     */
    public void checkAndDisconnect() {
        logger.debug("Vérification des quotas...");
        
        List<com.wifimanager.shared.model.Quota> activeQuotas = quotaManager.getActiveQuotas();
        int disconnected = 0;
        
        for (com.wifimanager.shared.model.Quota quota : activeQuotas) {
            if (quota.isExceeded()) {
                String macAddress = quota.getMacAddress();
                
                // Déterminer la raison
                String reason = "";
                if (!quota.hasTimeRemaining() && !quota.hasDataRemaining()) {
                    reason = "QUOTA_TIME_AND_DATA";
                } else if (!quota.hasTimeRemaining()) {
                    reason = "QUOTA_TIME";
                } else {
                    reason = "QUOTA_DATA";
                }
                
                // Déconnecter le client
                disconnectClient(macAddress, reason);
                disconnected++;
                
                logger.warn("Client déconnecté (quota dépassé): {} - {}", macAddress, reason);
            }
        }
        
        if (disconnected > 0) {
            logger.info("{} clients déconnectés pour quota dépassé", disconnected);
        } else {
            logger.debug("Aucun client à déconnecter");
        }
    }
    
    /**
     * Déconnecter manuellement un client
     */
    public void disconnectClient(String macAddress, String reason) {
        macAddress = macAddress.toUpperCase();
        
        try {
            // Récupérer l'IP depuis le module de détection (simulé pour l'instant)
            String ipAddress = getClientIp(macAddress);
            
            // Bloquer le trafic
            trafficController.blockClient(macAddress, ipAddress);
            
            // Désactiver le quota
            quotaManager.deactivateQuota(macAddress);
            
            // Enregistrer l'événement
            DisconnectionEvent event = new DisconnectionEvent(macAddress, ipAddress, reason);
            disconnectionHistory.put(macAddress + "_" + System.currentTimeMillis(), event);
            
            logger.info("Client déconnecté manuellement: {} - {}", macAddress, reason);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la déconnexion de {}: {}", macAddress, e.getMessage());
        }
    }
    
    /**
     * Reconnecter un client
     */
    public void reconnectClient(String macAddress) {
        macAddress = macAddress.toUpperCase();
        
        try {
            String ipAddress = getClientIp(macAddress);
            
            // Débloquer le trafic
            trafficController.unblockClient(macAddress, ipAddress);
            
            // Réactiver le quota
            quotaManager.reactivateQuota(macAddress);
            
            logger.info("Client reconnecté: {}", macAddress);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la reconnexion de {}: {}", macAddress, e.getMessage());
        }
    }
    
    /**
     * Récupérer l'historique des déconnexions
     */
    public List<DisconnectionEvent> getDisconnectionHistory() {
        return new ArrayList<>(disconnectionHistory.values());
    }
    
    /**
     * Récupérer les déconnexions récentes (dernières 24h)
     */
    public List<DisconnectionEvent> getRecentDisconnections() {
        List<DisconnectionEvent> recent = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        for (DisconnectionEvent event : disconnectionHistory.values()) {
            if (event.getTimestamp().isAfter(cutoff)) {
                recent.add(event);
            }
        }
        
        // Trier par date (plus récent d'abord)
        recent.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        
        return recent;
    }
    
    /**
     * Simuler la récupération d'IP (à remplacer par appel au Module 1)
     */
    private String getClientIp(String macAddress) {
        // Pour l'instant, simulation simple
        // Dans la version finale, appeler le Module 1
        return "192.168.0." + (100 + Math.abs(macAddress.hashCode() % 100));
    }
    
    // Classe interne pour les événements de déconnexion
    public static class DisconnectionEvent {
        private final String macAddress;
        private final String ipAddress;
        private final String reason;
        private final LocalDateTime timestamp;
        
        public DisconnectionEvent(String macAddress, String ipAddress, String reason) {
            this.macAddress = macAddress;
            this.ipAddress = ipAddress;
            this.reason = reason;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getMacAddress() { return macAddress; }
        public String getIpAddress() { return ipAddress; }
        public String getReason() { return reason; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("Disconnection{%s, %s, %s, %s}", 
                macAddress, ipAddress, reason, timestamp);
        }
    }
    
    // Getters
    public boolean isMonitoringActive() { return monitoringActive; }
}