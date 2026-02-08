package com.wifimanager.enforcer;

import java.time.LocalDateTime;

/* POJO représentant le quota d'un client WiFi. */

public class Quota {
    
    private String macAddress;          // Adresse MAC du client (ex: "AA:BB:CC:DD:EE:FF")
    
    private int initialTimeMinutes;     // Temps alloué au départ (ex: 30 minutes)
    private int initialDataMB;          // Données allouées au départ (ex: 500 MB)
    
    private int consumedTimeMinutes;    // Temps déjà consommé (incrémenté chaque minute)
    private int consumedDataMB;         // Données déjà consommées (incrémenté par paquets)
    
    private LocalDateTime createdAt;    // Date/heure de création du quota
    private LocalDateTime lastUpdated;  // Dernière modification
    
    
    /* Constructeur vide pour la désérialisation JSON.*/
    public Quota() {
    }
    
    /*Constructeur principal pour créer un nouveau quota.*/
    public Quota(String macAddress, int timeMinutes, int dataMB) {
        this.macAddress = macAddress;
        this.initialTimeMinutes = timeMinutes;
        this.initialDataMB = dataMB;
        this.consumedTimeMinutes = 0;      // Commence à zéro
        this.consumedDataMB = 0;           // Commence à zéro
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    /* Calculer le temps restant en minutes*/
    public int getRemainingTimeMinutes() {
        return initialTimeMinutes - consumedTimeMinutes;
    }
    
    /* Calculer les données restantes en MB. */
    public int getRemainingDataMB() {
        return initialDataMB - consumedDataMB;
    }
    
    /*Vérifier si le quota de temps est dépassé.*/
    public boolean isTimeExceeded() {
        return consumedTimeMinutes >= initialTimeMinutes;
    }
    
    /* Vérifier si le quota de données est dépassé.*/
    public boolean isDataExceeded() {
        return consumedDataMB >= initialDataMB;
    }
    
    /*Vérifier si au moins un quota est dépassé.*/
    public boolean isExceeded() {
        return isTimeExceeded() || isDataExceeded();
    }
    
    /*Calculer le pourcentage de temps consommé.*/
    public double getTimeUsagePercent() {
        if (initialTimeMinutes == 0) return 0;
        return (consumedTimeMinutes * 100.0) / initialTimeMinutes;
    }
    
    /*Calculer le pourcentage de données consommées.*/
    public double getDataUsagePercent() {
        if (initialDataMB == 0) return 0;
        return (consumedDataMB * 100.0) / initialDataMB;
    }
    
    // GETTERS & SETTERS
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    public int getInitialTimeMinutes() {
        return initialTimeMinutes;
    }
    
    public void setInitialTimeMinutes(int initialTimeMinutes) {
        this.initialTimeMinutes = initialTimeMinutes;
    }
    
    public int getInitialDataMB() {
        return initialDataMB;
    }
    
    public void setInitialDataMB(int initialDataMB) {
        this.initialDataMB = initialDataMB;
    }
    
    public int getConsumedTimeMinutes() {
        return consumedTimeMinutes;
    }
    
    public void setConsumedTimeMinutes(int consumedTimeMinutes) {
        this.consumedTimeMinutes = consumedTimeMinutes;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public int getConsumedDataMB() {
        return consumedDataMB;
    }
    
    public void setConsumedDataMB(int consumedDataMB) {
        this.consumedDataMB = consumedDataMB;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    // AFFICHAGE 
    
    @Override
    public String toString() {
        return String.format(
            "Quota[MAC=%s, Time=%d/%dmin (%.1f%%), Data=%d/%dMB (%.1f%%), Exceeded=%s]",
            macAddress,
            consumedTimeMinutes, initialTimeMinutes, getTimeUsagePercent(),
            consumedDataMB, initialDataMB, getDataUsagePercent(),
            isExceeded() ? "OUI" : "NON"
        );
    }
}
