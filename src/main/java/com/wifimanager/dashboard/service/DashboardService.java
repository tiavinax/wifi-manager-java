package com.wifimanager.dashboard.service;

import com.wifimanager.dashboard.model.ClientDTO;
import com.wifimanager.dashboard.model.DashboardStats;
import com.wifimanager.dashboard.model.QuotaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {
    
    @Autowired
    private DetectorService detectorService;
    
    @Autowired
    private EnforcerService enforcerService;
    
    /**
     * Récupère toutes les stats pour le dashboard
     */
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        // Stats Module 1
        DashboardStats detectorStats = detectorService.getStats();
        stats.setTotalClients(detectorStats.getTotalClients());
        stats.setActiveClients(detectorStats.getActiveClients());
        stats.setNetworkInterface(detectorStats.getNetworkInterface());
        stats.setSubnet(detectorStats.getSubnet());
        stats.setModule1Online(detectorStats.isModule1Online());
        
        // Stats Module 2
        stats = enforcerService.getStats(stats);
        
        return stats;
    }
    
    /**
     * Récupère les clients avec leurs quotas
     */
    public List<ClientDTO> getClientsWithQuotas() {
        List<ClientDTO> clients = detectorService.getAllClients();
        List<QuotaDTO> quotas = enforcerService.getAllQuotas();
        
        // Associer les quotas aux clients
        for (ClientDTO client : clients) {
            for (QuotaDTO quota : quotas) {
                if (quota.getMacAddress() != null && 
                    quota.getMacAddress().equals(client.getMacAddress())) {
                    // Ajouter les infos quota au client
                    client.setDeviceType("Quota: " + quota.getTimeRemaining() + "min");
                    break;
                }
            }
        }
        
        return clients;
    }
}