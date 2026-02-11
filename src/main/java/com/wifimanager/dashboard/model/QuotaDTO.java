package com.wifimanager.dashboard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuotaDTO {
    private String macAddress;
    private int timeLimitMinutes;
    private int dataLimitMB;
    private int timeUsedMinutes;
    private int dataUsedMB;
    private boolean isActive;
    private boolean isExceeded;
    private String quotaId;
    
    // Constructeurs
    public QuotaDTO() {}
    
    // Getters et Setters
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    
    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(int timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }
    
    public int getDataLimitMB() { return dataLimitMB; }
    public void setDataLimitMB(int dataLimitMB) { this.dataLimitMB = dataLimitMB; }
    
    public int getTimeUsedMinutes() { return timeUsedMinutes; }
    public void setTimeUsedMinutes(int timeUsedMinutes) { this.timeUsedMinutes = timeUsedMinutes; }
    
    public int getDataUsedMB() { return dataUsedMB; }
    public void setDataUsedMB(int dataUsedMB) { this.dataUsedMB = dataUsedMB; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isExceeded() { return isExceeded; }
    public void setExceeded(boolean exceeded) { isExceeded = exceeded; }
    
    public String getQuotaId() { return quotaId; }
    public void setQuotaId(String quotaId) { this.quotaId = quotaId; }
    
    // Méthodes utilitaires
    public int getTimeRemaining() {
        return Math.max(0, timeLimitMinutes - timeUsedMinutes);
    }
    
    public int getDataRemaining() {
        return Math.max(0, dataLimitMB - dataUsedMB);
    }
    
    public int getTimePercentage() {
        if (timeLimitMinutes <= 0) return 0;
        return (int) ((timeUsedMinutes * 100.0) / timeLimitMinutes);
    }
    
    public int getDataPercentage() {
        if (dataLimitMB <= 0) return 0;
        return (int) ((dataUsedMB * 100.0) / dataLimitMB);
    }
    
    public String getProgressClass() {
        int percentage = getDataPercentage();
        if (percentage >= 90) return "danger";
        if (percentage >= 70) return "warning";
        return "";
    }
}