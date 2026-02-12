package com.wifimanager.enforcer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class EnforcerMain {
    private static final Logger logger = LoggerFactory.getLogger(EnforcerMain.class);
    
    public static void main(String[] args) {
        // Parser les arguments
        boolean simulationMode = true; // Par défaut
        String networkInterface = "wlo1";
        int port = 8082;
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--real":
                case "-r":
                    simulationMode = false;
                    break;
                case "--interface":
                case "-i":
                    if (i + 1 < args.length) networkInterface = args[++i];
                    break;
                case "--port":
                case "-p":
                    if (i + 1 < args.length) port = Integer.parseInt(args[++i]);
                    break;
                case "--simulation":
                case "-s":
                    simulationMode = true;
                    break;
            }
        }
        
        // Initialisation
        logger.info("🚀 Démarrage du Module 2 - Enforcer");
        logger.info("📡 Interface réseau: {}", networkInterface);
        logger.info("🔌 Port API: {}", port);
        
        QuotaManager quotaManager = new QuotaManager();
        TrafficController trafficController = new TrafficController(simulationMode, networkInterface);
        DisconnectionService disconnectionService = new DisconnectionService(quotaManager, trafficController);
        EnforcerApiServer apiServer = new EnforcerApiServer(quotaManager, trafficController, disconnectionService, port);
        
        try {
            apiServer.start();
            
            // ✅ DÉMARRER LE THREAD DE MISE À JOUR DU TEMPS
            startTimeUpdateThread(quotaManager);
            
            // ✅ DÉMARRER LA SURVEILLANCE DES DÉCONNEXIONS
            disconnectionService.startMonitoring();
            
            if (simulationMode) {
                logger.warn("⚠️ Mode SIMULATION - Les clients ne seront PAS vraiment bloqués");
            } else {
                logger.info("🔥 MODE RÉEL ACTIVÉ - Les clients seront VRAIMENT déconnectés !");
            }
            
            // Boucle principale
            while (true) {
                Thread.sleep(1000);
            }
            
        } catch (IOException e) {
            logger.error("❌ Erreur démarrage serveur API: {}", e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            logger.info("Arrêt du module Enforcer");
            System.exit(0);
        }
    }
    
    /**
     * Thread de mise à jour du temps toutes les 60 secondes
     */
    private static void startTimeUpdateThread(QuotaManager quotaManager) {
        Thread timeThread = new Thread(() -> {
            logger.info("⏱️ Démarrage thread de mise à jour du temps (intervalle: 60s)");
            
            while (true) {
                try {
                    Thread.sleep(60000); // 60 secondes
                    quotaManager.updateAllTimeQuotas();
                    logger.debug("Temps mis à jour pour tous les quotas actifs");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Erreur mise à jour temps: {}", e.getMessage());
                }
            }
        });
        
        timeThread.setName("TimeUpdateThread");
        timeThread.setDaemon(true);
        timeThread.start();
    }
}