package com.wifimanager.dashboard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientDTO {
    private String macAddress;
    private String ipAddress;
    private String hostname;
    private boolean active;
    private long bytesDownloaded;
    private long bytesUploaded;
    private long totalBytes;
    private String firstSeen;
    private String lastSeen;
    private String deviceType;
    private int signalStrength;
    
    // Constructeurs
    public ClientDTO() {}
    
    public ClientDTO(String macAddress, String ipAddress) {
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.active = true;
        this.bytesDownloaded = 0;
        this.bytesUploaded = 0;
        this.totalBytes = 0;
    }
    
    // Getters et Setters
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public long getBytesDownloaded() { return bytesDownloaded; }
    public void setBytesDownloaded(long bytesDownloaded) { this.bytesDownloaded = bytesDownloaded; }
    
    public long getBytesUploaded() { return bytesUploaded; }
    public void setBytesUploaded(long bytesUploaded) { this.bytesUploaded = bytesUploaded; }
    
    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
    
    public String getFirstSeen() { return firstSeen; }
    public void setFirstSeen(String firstSeen) { this.firstSeen = firstSeen; }
    
    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public int getSignalStrength() { return signalStrength; }
    public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }
    
    // Méthodes utilitaires
    public String getFormattedMac() {
        return macAddress != null ? macAddress.toUpperCase() : "";
    }
    
    public String getStatusClass() {
        return active ? "status-active" : "status-inactive";
    }
    
    public String getStatusText() {
        return active ? "Actif" : "Inactif";
    }
    
    public String getFormattedFirstSeen() {
        if (firstSeen == null) return "-";
        try {
            LocalDateTime dt = LocalDateTime.parse(firstSeen);
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return firstSeen;
        }
    }
    
    public String getFormattedBytes() {
        return formatBytes(totalBytes);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}