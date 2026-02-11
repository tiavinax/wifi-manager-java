package com.wifimanager.shared.model;

import java.time.LocalDateTime;

public class Quota {
    private String macAddress;
    private int timeLimitMinutes;     // Temps total autorisé (minutes)
    private int dataLimitMB;          // Données totales autorisées (MB)
    private int timeUsedMinutes;      // Temps déjà consommé
    private int dataUsedMB;           // Données déjà consommées
    private LocalDateTime startTime;  // Quand le quota a commencé
    private LocalDateTime lastUpdate; // Dernière mise à jour
    private boolean isActive;         // Le quota est-il actif ?
    private String quotaId;           // ID unique pour ce quota
    
    public Quota() {}
    
    public Quota(String macAddress, int timeLimitMinutes, int dataLimitMB) {
        this.macAddress = macAddress.toUpperCase();
        this.timeLimitMinutes = timeLimitMinutes;
        this.dataLimitMB = dataLimitMB;
        this.timeUsedMinutes = 0;
        this.dataUsedMB = 0;
        this.startTime = LocalDateTime.now();
        this.lastUpdate = LocalDateTime.now();
        this.isActive = true;
        this.quotaId = generateQuotaId();
    }

    public void setExceeded(boolean exceeded) {
        this.isActive = !exceeded;
    }
    
    private String generateQuotaId() {
        return macAddress + "_" + System.currentTimeMillis();
    }
    
    // Méthodes utilitaires
    public boolean hasTimeRemaining() {
        return timeUsedMinutes < timeLimitMinutes;
    }
    
    public boolean hasDataRemaining() {
        return dataUsedMB < dataLimitMB;
    }
    
    public boolean isExceeded() {
        return !hasTimeRemaining() || !hasDataRemaining();
    }
    
    public int getTimeRemainingMinutes() {
        return Math.max(0, timeLimitMinutes - timeUsedMinutes);
    }
    
    public int getDataRemainingMB() {
        return Math.max(0, dataLimitMB - dataUsedMB);
    }
    
    public void consumeTime(int minutes) {
        if (isActive) {
            this.timeUsedMinutes += minutes;
            this.lastUpdate = LocalDateTime.now();
        }
    }
    
    public void consumeData(int megabytes) {
        if (isActive) {
            this.dataUsedMB += megabytes;
            this.lastUpdate = LocalDateTime.now();
        }
    }
    
    // Getters et Setters
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { 
        this.macAddress = macAddress.toUpperCase(); 
    }
    
    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(int timeLimitMinutes) { 
        this.timeLimitMinutes = timeLimitMinutes; 
    }
    
    public int getDataLimitMB() { return dataLimitMB; }
    public void setDataLimitMB(int dataLimitMB) { 
        this.dataLimitMB = dataLimitMB; 
    }
    
    public int getTimeUsedMinutes() { return timeUsedMinutes; }
    public void setTimeUsedMinutes(int timeUsedMinutes) { 
        this.timeUsedMinutes = timeUsedMinutes; 
    }
    
    public int getDataUsedMB() { return dataUsedMB; }
    public void setDataUsedMB(int dataUsedMB) { 
        this.dataUsedMB = dataUsedMB; 
    }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { 
        this.startTime = startTime; 
    }
    
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(LocalDateTime lastUpdate) { 
        this.lastUpdate = lastUpdate; 
    }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public String getQuotaId() { return quotaId; }
    public void setQuotaId(String quotaId) { this.quotaId = quotaId; }
    
    @Override
    public String toString() {
        return String.format(
            "Quota{mac=%s, time=%d/%d min, data=%d/%d MB, active=%s}",
            macAddress, timeUsedMinutes, timeLimitMinutes, 
            dataUsedMB, dataLimitMB, isActive
        );
    }
}