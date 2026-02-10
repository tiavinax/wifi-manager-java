package com.wifimanager.inspector;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;

/**
 * Capture des paquets réseau en temps réel
 * Utilise JNetPcap (jpcap) pour la capture bas niveau
 * 
 * Note: Cette implémentation nécessite la bibliothèque jpcap.jar
 * et les droits d'accès aux interfaces réseau (CAP_NET_RAW)
 */
public class PacketSniffer {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Queue<Map<String, Object>> packetQueue = new ConcurrentLinkedQueue<>();
    private Thread snifferThread;
    private static final int MAX_QUEUE_SIZE = 10000;

    // Interface réseau à surveiller (par défaut: eth0 ou wlan0)
    private String networkInterface = "eth0";

    /**
     * Constructeur par défaut
     */
    public PacketSniffer() {
        // Détecter automatiquement l'interface réseau
        this.networkInterface = detectNetworkInterface();
    }

    /**
     * Constructeur avec interface spécifique
     */
    public PacketSniffer(String networkInterface) {
        this.networkInterface = networkInterface;
    }

    /**
     * Démarrer la capture de paquets
     */
    public void start() throws Exception {
        if (running.get()) {
            throw new IllegalStateException("PacketSniffer est déjà en cours d'exécution");
        }

        System.out.println("Démarrage du PacketSniffer sur l'interface: " + networkInterface);
        
        running.set(true);
        snifferThread = new Thread(this::captureLoop, "PacketSniffer-Thread");
        snifferThread.setDaemon(true);
        snifferThread.start();
        
        System.out.println("PacketSniffer démarré avec succès");
    }

    /**
     * Arrêter la capture de paquets
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        System.out.println("Arrêt du PacketSniffer...");
        running.set(false);
        
        if (snifferThread != null) {
            try {
                snifferThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        packetQueue.clear();
        System.out.println("PacketSniffer arrêté");
    }

    /**
     * Boucle principale de capture
     * 
     * Note: Dans une implémentation réelle avec jpcap:
     * - Ouvrir l'interface réseau avec Pcap.openLive()
     * - Configurer un filtre BPF si nécessaire
     * - Capturer les paquets avec loop() ou nextPacket()
     * - Parser les headers Ethernet/IP/TCP/UDP
     */
    private void captureLoop() {
        System.out.println("Boucle de capture démarrée");
        
        try {
            // SIMULATION: Dans une vraie implémentation, on utiliserait jpcap ici
            // Pcap pcap = Pcap.openLive(networkInterface, snaplen, promiscuous, timeout, errbuf);
            
            while (running.get()) {
                try {
                    // Simulation de capture de paquets
                    // Dans la vraie version: pcap.loop(count, handler, user)
                    simulatePacketCapture();
                    
                    Thread.sleep(100); // Intervalle de capture
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur dans la boucle de capture: " + e.getMessage());
            running.set(false);
        }
    }

    /**
     * Simulation de capture de paquets (pour démonstration)
     * À remplacer par du vrai code jpcap en production
     */
    private void simulatePacketCapture() {
        // Simuler quelques paquets aléatoires
        if (Math.random() > 0.7) { // 30% de chance de capturer un paquet
            Map<String, Object> packet = new HashMap<>();
            
            // Simuler des adresses MAC et IP
            String[] macs = {"00:11:22:33:44:55", "AA:BB:CC:DD:EE:FF", "12:34:56:78:90:AB"};
            String[] ips = {"192.168.1.100", "192.168.1.101", "192.168.1.102"};
            String[] protocols = {"TCP", "UDP", "ICMP"};
            
            packet.put("timestamp", LocalDateTime.now().toString());
            packet.put("srcMac", macs[(int)(Math.random() * macs.length)]);
            packet.put("dstMac", "00:00:00:00:00:01");
            packet.put("srcIp", ips[(int)(Math.random() * ips.length)]);
            packet.put("dstIp", "8.8.8.8");
            packet.put("protocol", protocols[(int)(Math.random() * protocols.length)]);
            packet.put("size", (int)(Math.random() * 1500) + 64);
            packet.put("srcPort", (int)(Math.random() * 65535));
            packet.put("dstPort", 443);
            
            // Ajouter quelques données de payload (simulées)
            packet.put("payload", generateSimulatedPayload());
            
            addPacket(packet);
        }
    }

    /**
     * Générer un payload simulé pour les tests
     */
    private String generateSimulatedPayload() {
        String[] patterns = {
            "GET /watch?v=dQw4w9WgXcQ HTTP/1.1\r\nHost: www.youtube.com",
            "GET /browse HTTP/1.1\r\nHost: www.netflix.com",
            "GET / HTTP/1.1\r\nHost: www.google.com",
            "POST /api/data HTTP/1.1\r\nHost: example.com"
        };
        
        return patterns[(int)(Math.random() * patterns.length)];
    }

    /**
     * Ajouter un paquet capturé à la queue
     */
    private void addPacket(Map<String, Object> packet) {
        if (packetQueue.size() >= MAX_QUEUE_SIZE) {
            packetQueue.poll(); // Retirer le plus ancien
        }
        packetQueue.offer(packet);
    }

    /**
     * Récupérer les paquets récents et vider la queue
     */
    public List<Map<String, Object>> getRecentPackets() {
        List<Map<String, Object>> packets = new ArrayList<>();
        
        Map<String, Object> packet;
        while ((packet = packetQueue.poll()) != null) {
            packets.add(packet);
        }
        
        return packets;
    }

    /**
     * Obtenir le nombre de paquets en attente
     */
    public int getQueueSize() {
        return packetQueue.size();
    }

    /**
     * Vérifier si le sniffer est en cours d'exécution
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Détecter automatiquement l'interface réseau active
     */
    private String detectNetworkInterface() {
        try {
            // Essayer de détecter l'interface réseau
            // Dans une vraie implémentation: utiliser NetworkInterface.getNetworkInterfaces()
            String[] commonInterfaces = {"eth0", "wlan0", "en0", "wlp2s0"};
            
            for (String iface : commonInterfaces) {
                // Vérifier si l'interface existe (simplifié)
                if (checkInterfaceExists(iface)) {
                    return iface;
                }
            }
            
            return "eth0"; // Fallback
        } catch (Exception e) {
            return "eth0";
        }
    }

    /**
     * Vérifier si une interface réseau existe
     */
    private boolean checkInterfaceExists(String iface) {
        try {
            // Simulation - dans une vraie implémentation:
            // NetworkInterface.getByName(iface) != null
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Définir l'interface réseau à surveiller
     */
    public void setNetworkInterface(String networkInterface) {
        if (running.get()) {
            throw new IllegalStateException("Impossible de changer l'interface pendant l'exécution");
        }
        this.networkInterface = networkInterface;
    }

    /**
     * Obtenir l'interface réseau surveillée
     */
    public String getNetworkInterface() {
        return networkInterface;
    }
}
