package com.wifimanager.dashboard.service;

import com.wifimanager.dashboard.model.ClientDTO;
import com.wifimanager.dashboard.model.DashboardStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DetectorService {
    
    private static final Logger logger = LoggerFactory.getLogger(DetectorService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${detector.api.url:http://localhost:8081}")
    private String detectorApiUrl;
    
    /**
     * Récupère tous les clients depuis le Module 1
     */
    public List<ClientDTO> getAllClients() {
        try {
            String url = detectorApiUrl + "/api/clients";
            logger.debug("Appel Module 1: GET {}", url);
            
            ResponseEntity<List<ClientDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ClientDTO>>() {}
            );
            
            List<ClientDTO> clients = response.getBody();
            logger.info("Module 1: {} clients récupérés", clients != null ? clients.size() : 0);
            
            return clients != null ? clients : new ArrayList<>();
            
        } catch (RestClientException e) {
            logger.error("Erreur Module 1 (Détection): {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Récupère les statistiques du Module 1
     */
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();
        
        try {
            String url = detectorApiUrl + "/api/stats";
            logger.debug("Appel Module 1: GET {}", url);
            
            ResponseEntity<DashboardStats> response = restTemplate.getForEntity(
                url,
                DashboardStats.class
            );
            
            if (response.getBody() != null) {
                stats = response.getBody();
            }
            
            stats.setModule1Online(true);
            
        } catch (RestClientException e) {
            logger.warn("Module 1 hors ligne: {}", e.getMessage());
            stats.setModule1Online(false);
            stats.setTotalClients(0);
            stats.setActiveClients(0);
        }
        
        return stats;
    }
    
    /**
     * Vérifie si le Module 1 est en ligne
     */
    public boolean isModuleOnline() {
        try {
            String url = detectorApiUrl + "/api/health";
            restTemplate.getForEntity(url, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}