package com.wifimanager.dashboard.model;

public class DashboardStats {
    private int totalClients;
    private int activeClients;
    private int totalQuotas;
    private int activeQuotas;
    private long totalTrafficBytes;
    private String networkInterface;
    private String subnet;
    private boolean module1Online;
    private boolean module2Online;
    
    // Getters et Setters
    public int getTotalClients() { return totalClients; }
    public void setTotalClients(int totalClients) { this.totalClients = totalClients; }
    
    public int getActiveClients() { return activeClients; }
    public void setActiveClients(int activeClients) { this.activeClients = activeClients; }
    
    public int getTotalQuotas() { return totalQuotas; }
    public void setTotalQuotas(int totalQuotas) { this.totalQuotas = totalQuotas; }
    
    public int getActiveQuotas() { return activeQuotas; }
    public void setActiveQuotas(int activeQuotas) { this.activeQuotas = activeQuotas; }
    
    public long getTotalTrafficBytes() { return totalTrafficBytes; }
    public void setTotalTrafficBytes(long totalTrafficBytes) { this.totalTrafficBytes = totalTrafficBytes; }
    
    public String getNetworkInterface() { return networkInterface; }
    public void setNetworkInterface(String networkInterface) { this.networkInterface = networkInterface; }
    
    public String getSubnet() { return subnet; }
    public void setSubnet(String subnet) { this.subnet = subnet; }
    
    public boolean isModule1Online() { return module1Online; }
    public void setModule1Online(boolean module1Online) { this.module1Online = module1Online; }
    
    public boolean isModule2Online() { return module2Online; }
    public void setModule2Online(boolean module2Online) { this.module2Online = module2Online; }
    
    // Méthodes utilitaires
    public String getFormattedTraffic() {
        long bytes = totalTrafficBytes;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}