package com.wifimanager.shared.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Représente un client connecté au réseau WiFi
 */
public class Client {
    private String macAddress;
    private String ipAddress;
    private String hostname;
    private String deviceType;
    private boolean active;
    
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    
    private long bytesDownloaded;
    private long bytesUploaded;
    private int signalStrength;
    private String vendor;
    
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructeurs
    public Client() {
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.active = true;
    }

    public Client(String macAddress, String ipAddress) {
        this();
        this.macAddress = macAddress != null ? macAddress.toUpperCase() : null;
        this.ipAddress = ipAddress;
    }

    // Getters et Setters
    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress != null ? macAddress.toUpperCase() : null;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }
    
    public String getFirstSeenFormatted() {
        return firstSeen != null ? firstSeen.format(FORMATTER) : "";
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public String getLastSeenFormatted() {
        return lastSeen != null ? lastSeen.format(FORMATTER) : "";
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public long getBytesUploaded() {
        return bytesUploaded;
    }

    public void setBytesUploaded(long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public long getTotalBytes() {
        return bytesDownloaded + bytesUploaded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(macAddress, client.macAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(macAddress);
    }

    @Override
    public String toString() {
        return "Client{" +
                "macAddress='" + macAddress + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", hostname='" + hostname + '\'' +
                ", active=" + active +
                ", bytesDownloaded=" + bytesDownloaded +
                ", bytesUploaded=" + bytesUploaded +
                '}';
    }
}