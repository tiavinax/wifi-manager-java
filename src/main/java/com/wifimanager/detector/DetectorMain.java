package com.wifimanager.detector;

import com.wifimanager.shared.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée du module de détection
 * Démarre le service de découverte et le serveur API
 */
public class DetectorMain {
    private static final Logger logger = LoggerFactory.getLogger(DetectorMain.class);

    public static void main(String[] args) {
        try {
            logger.info("========================================");
            logger.info("Démarrage du Module de Détection");
            logger.info("========================================");
            
            // Parser les arguments en ligne de commande
            String networkInterface = Constants.DEFAULT_INTERFACE;
            String subnet = Constants.DEFAULT_SUBNET;
            int port = Constants.DETECTOR_PORT;
            
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--interface":
                    case "-i":
                        if (i + 1 < args.length) {
                            networkInterface = args[++i];
                        }
                        break;
                    case "--subnet":
                    case "-s":
                        if (i + 1 < args.length) {
                            subnet = args[++i];
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
            logger.info("  Interface: {}", networkInterface);
            logger.info("  Subnet: {}", subnet);
            logger.info("  Port API: {}", port);
            
            // Créer le service de découverte
            ClientDiscoveryService discoveryService = new ClientDiscoveryService(networkInterface, subnet);
            
            // Lancer une découverte initiale
            logger.info("Effectuation de la découverte initiale...");
            discoveryService.discoverClients();
            logger.info("Découverte initiale terminée: {} clients trouvés", 
                discoveryService.getTotalClientCount());
            
            // Démarrer le serveur API
            DetectorApiServer apiServer = new DetectorApiServer(discoveryService, port);
            apiServer.start();
            
            // Boucle periodique de découverte
            startDiscoveryLoop(discoveryService);
            
        } catch (Exception e) {
            logger.error("Erreur fatale: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Démarre une boucle de découverte périodique
     */
    private static void startDiscoveryLoop(ClientDiscoveryService discoveryService) {
        Thread discoveryThread = new Thread(() -> {
            long interval = Constants.ARP_SCAN_INTERVAL;
            logger.info("Démarrage de la boucle de découverte (intervalle: {}ms)", interval);
            
            while (true) {
                try {
                    Thread.sleep(interval);
                    discoveryService.discoverClients();
                    logger.debug("Redécouverte effectuée: {} clients actifs", 
                        discoveryService.getActiveClientCount());
                } catch (InterruptedException e) {
                    logger.info("Boucle de découverte interrompue");
                    break;
                } catch (Exception e) {
                    logger.error("Erreur dans la boucle de découverte: {}", e.getMessage(), e);
                }
            }
        });
        
        discoveryThread.setName("DiscoveryThread");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    /**
     * Affiche l'utilisation du programme
     */
    private static void printUsage() {
        System.out.println("Usage: java DetectorMain [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -i, --interface <interface>  Interface réseau (défaut: " + Constants.DEFAULT_INTERFACE + ")");
        System.out.println("  -s, --subnet <subnet>        Sous-réseau (défaut: " + Constants.DEFAULT_SUBNET + ")");
        System.out.println("  -p, --port <port>           Port API (défaut: " + Constants.DETECTOR_PORT + ")");
        System.out.println("  -h, --help                  Affiche cette aide");
    }
}
