package com.wifimanager.dashboard.service;

import com.wifimanager.dashboard.model.QuotaDTO;
import com.wifimanager.dashboard.model.DashboardStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnforcerService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnforcerService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${enforcer.api.url:http://localhost:8082}")
    private String enforcerApiUrl;
    
    /**
     * Récupère tous les quotas actifs
     */
    public List<QuotaDTO> getAllQuotas() {
        try {
            String url = enforcerApiUrl + "/quotas";
            logger.debug("Appel Module 2: GET {}", url);
            
            ResponseEntity<List<QuotaDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<QuotaDTO>>() {}
            );
            
            List<QuotaDTO> quotas = response.getBody();
            logger.info("Module 2: {} quotas récupérés", quotas != null ? quotas.size() : 0);
            
            return quotas != null ? quotas : new ArrayList<>();
            
        } catch (RestClientException e) {
            logger.error("Erreur Module 2 (Enforcer): {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Crée un nouveau quota
     */
    public QuotaDTO createQuota(String macAddress, int timeMinutes, int dataMB) {
        QuotaDTO result = new QuotaDTO();
        
        try {
            String url = enforcerApiUrl + "/quota/set";
            logger.debug("Appel Module 2: POST {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> request = new HashMap<>();
            request.put("mac", macAddress);
            request.put("timeMinutes", timeMinutes);
            request.put("dataMB", dataMB);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<QuotaDTO> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                QuotaDTO.class
            );
            
            if (response.getBody() != null) {
                result = response.getBody();
                logger.info("Quota créé pour {}: {} min, {} MB", macAddress, timeMinutes, dataMB);
            }
            
        } catch (RestClientException e) {
            logger.error("Erreur création quota: {}", e.getMessage());
            result.setMacAddress(macAddress);
            result.setActive(false);
        }
        
        return result;
    }
    
    /**
     * Supprime un quota
     */
    public boolean deleteQuota(String macAddress) {
        try {
            String url = enforcerApiUrl + "/quota/" + macAddress;
            logger.debug("Appel Module 2: DELETE {}", url);
            
            restTemplate.delete(url);
            logger.info("Quota supprimé pour {}", macAddress);
            return true;
            
        } catch (RestClientException e) {
            logger.error("Erreur suppression quota: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Déconnecte un client
     */
    public boolean disconnectClient(String macAddress) {
        try {
            String url = enforcerApiUrl + "/disconnect/" + macAddress;
            logger.debug("Appel Module 2: POST {}", url);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Client déconnecté: {}", macAddress);
                return true;
            }
            
        } catch (RestClientException e) {
            logger.error("Erreur déconnexion: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Récupère les stats du Module 2
     */
    public DashboardStats getStats(DashboardStats stats) {
        try {
            String url = enforcerApiUrl + "/stats";
            logger.debug("Appel Module 2: GET {}", url);
            
            ResponseEntity<DashboardStats> response = restTemplate.getForEntity(
                url,
                DashboardStats.class
            );
            
            if (response.getBody() != null) {
                DashboardStats moduleStats = response.getBody();
                stats.setTotalQuotas(moduleStats.getTotalQuotas());
                stats.setActiveQuotas(moduleStats.getActiveQuotas());
            }
            
            stats.setModule2Online(true);
            
        } catch (RestClientException e) {
            logger.warn("Module 2 hors ligne: {}", e.getMessage());
            stats.setModule2Online(false);
            stats.setTotalQuotas(0);
            stats.setActiveQuotas(0);
        }
        
        return stats;
    }
    
    /**
     * Vérifie si le Module 2 est en ligne
     */
    public boolean isModuleOnline() {
        try {
            String url = enforcerApiUrl + "/health";
            restTemplate.getForEntity(url, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}