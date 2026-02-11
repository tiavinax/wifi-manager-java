package com.wifimanager.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
// import java.util.concurrent.CountDownLatch;

@SpringBootApplication
@EnableScheduling
public class DashboardMain {
    
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(DashboardMain.class, args);
        
        System.out.println("\n" +
            "╔════════════════════════════════════════════════════════════╗\n" +
            "║         WiFi Manager - Dashboard (Module 4)              ║\n" +
            "║                    🌐 PRÊT !                             ║\n" +
            "╠════════════════════════════════════════════════════════════╣\n" +
            "║  📍 http://localhost:8080                                 ║\n" +
            "║                                                            ║\n" +
            "║  ⌛ Dashboard en cours d'exécution...                     ║\n" +
            "║  🔴 Pour arrêter: Ctrl+C                                  ║\n" +
            "╚════════════════════════════════════════════════════════════╝\n");
        
        // 🔥 BLOQUE LE PROGRAMME POUR QU'IL RESTE ACTIF
        Thread.currentThread().join();
    }
}