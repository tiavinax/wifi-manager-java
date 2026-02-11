package com.wifimanager.enforcer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée du Module 2 (Enforcer)
 */
public class EnforcerMain {
    private static final Logger logger = LoggerFactory.getLogger(EnforcerMain.class);
    
    public static void main(String[] args) {
        try {
            logger.info("========================================");
            logger.info("Démarrage du Module 2 - Enforcer");
            logger.info("========================================");
            
            // Configuration par défaut
            boolean simulationMode = true; // Mode simulation par défaut
            String networkInterface = "wlo1";
            int port = 8082;
            
            // Parser les arguments
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--simulation":
                    case "-s":
                        simulationMode = true;
                        break;
                    case "--real":
                    case "-r":
                        simulationMode = false;
                        break;
                    case "--interface":
                    case "-i":
                        if (i + 1 < args.length) {
                            networkInterface = args[++i];
                        }
                        break;
                    case "--port":
                    case "-p":
                        if (i + 1 < args.length) {
                            try {
                                port = Integer.parseInt(args[++i]);
                            } catch (NumberFormatException e) {
                                logger.warn("Port invalide, utilisation de la valeur par défaut: {}", port);
                            }
                        }
                        break;
                    case "--help":
                    case "-h":
                        printUsage();
                        return;
                }
            }
            
            logger.info("Configuration:");
            logger.info("  Mode: {}", simulationMode ? "SIMULATION" : "RÉEL");
            logger.info("  Interface réseau: {}", networkInterface);
            logger.info("  Port API: {}", port);
            
            // Initialiser les composants
            logger.info("Initialisation des composants...");
            
            QuotaManager quotaManager = new QuotaManager();
            logger.info("✅ QuotaManager initialisé");
            
            TrafficController trafficController = new TrafficController(simulationMode, networkInterface);
            logger.info("✅ TrafficController initialisé (mode: {})", 
                simulationMode ? "SIMULATION" : "RÉEL");
            
            DisconnectionService disconnectionService = new DisconnectionService(
                quotaManager, trafficController);
            logger.info("✅ DisconnectionService initialisé");
            
            // Démarrer la surveillance automatique
            disconnectionService.startMonitoring();
            logger.info("✅ Surveillance automatique démarrée");
            
            // Démarrer le serveur API
            EnforcerApiServer apiServer = new EnforcerApiServer(
                quotaManager, trafficController, disconnectionService, port);
            apiServer.start();
            
            // Thread pour mettre à jour le temps consommé (1 minute par minute)
            startTimeUpdateThread(quotaManager);
            
            logger.info("✅ Module 2 complètement initialisé et opérationnel!");
            logger.info("");
            logger.info("📋 Endpoints API disponibles sur http://localhost:{}", port);
            logger.info("   curl http://localhost:{}/health", port);
            logger.info("   curl -X POST http://localhost:{}/quota/set \\", port);
            logger.info("     -H \"Content-Type: application/json\" \\");
            logger.info("     -d '{\"mac\":\"AA:BB:CC:DD:EE:FF\",\"timeMinutes\":5,\"dataMB\":100}'");
            logger.info("");
            logger.info("🔄 Le module fonctionne en arrière-plan:");
            logger.info("   - Surveillance des quotas toutes les 30 secondes");
            logger.info("   - Déconnexion automatique si quota dépassé");
            logger.info("   - Sauvegarde automatique des quotas");
            
            // Garder le programme en vie
            Thread.sleep(Long.MAX_VALUE);
            
        } catch (Exception e) {
            logger.error("Erreur fatale dans EnforcerMain: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Démarrer un thread qui met à jour le temps consommé
     */
    private static void startTimeUpdateThread(QuotaManager quotaManager) {
        Thread timeUpdateThread = new Thread(() -> {
            logger.info("Démarrage thread de mise à jour du temps (intervalle: 60s)");
            
            while (true) {
                try {
                    Thread.sleep(60000); // 60 secondes
                    quotaManager.updateAllTimeQuotas();
                    logger.debug("Temps mis à jour pour tous les quotas actifs");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Erreur dans la mise à jour du temps: {}", e.getMessage());
                }
            }
        });
        
        timeUpdateThread.setName("TimeUpdateThread");
        timeUpdateThread.setDaemon(true);
        timeUpdateThread.start();
    }
    
    /**
     * Afficher l'utilisation
     */
    private static void printUsage() {
        System.out.println("Usage: java EnforcerMain [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -s, --simulation          Mode simulation (défaut)");
        System.out.println("  -r, --real               Mode réel (nécessite sudo)");
        System.out.println("  -i, --interface <iface>  Interface réseau (défaut: wlo1)");
        System.out.println("  -p, --port <port>        Port API (défaut: 8082)");
        System.out.println("  -h, --help               Affiche cette aide");
        System.out.println();
        System.out.println("Exemples:");
        System.out.println("  java EnforcerMain                          # Mode simulation");
        System.out.println("  java EnforcerMain --real --port 8082       # Mode réel");
        System.out.println("  java EnforcerMain -i eth0 -p 9090         # Interface spécifique");
    }
}