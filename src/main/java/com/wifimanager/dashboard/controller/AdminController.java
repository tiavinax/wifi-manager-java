package com.wifimanager.dashboard.controller;

import com.wifimanager.dashboard.model.ClientDTO;
import com.wifimanager.dashboard.model.DashboardStats;
import com.wifimanager.dashboard.model.QuotaDTO;
import com.wifimanager.dashboard.service.DashboardService;
import com.wifimanager.dashboard.service.DetectorService;
import com.wifimanager.dashboard.service.EnforcerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class AdminController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private DetectorService detectorService;
    
    @Autowired
    private EnforcerService enforcerService;
    
    /**
     * Page d'accueil - Dashboard
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "Tableau de bord");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("content", "index");
        
        // Statistiques
        DashboardStats stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        
        // Derniers clients
        List<ClientDTO> recentClients = detectorService.getAllClients();
        if (recentClients.size() > 5) {
            recentClients = recentClients.subList(0, 5);
        }
        model.addAttribute("recentClients", recentClients);
        
        return "layout";
    }
    
    /**
     * Page Clients
     */
    @GetMapping("/clients")
    public String clients(Model model) {
        model.addAttribute("pageTitle", "Clients");
        model.addAttribute("activeMenu", "clients");
        model.addAttribute("content", "clients");
        
        // Liste des clients
        List<ClientDTO> clients = dashboardService.getClientsWithQuotas();
        model.addAttribute("clients", clients);
        
        // Statistiques
        DashboardStats stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        
        return "layout";
    }
    
    /**
     * Page Quotas
     */
    @GetMapping("/quotas")
    public String quotas(Model model) {
        model.addAttribute("pageTitle", "Gestion des quotas");
        model.addAttribute("activeMenu", "quotas");
        model.addAttribute("content", "quotas");
        
        // Liste des quotas
        List<QuotaDTO> quotas = enforcerService.getAllQuotas();
        model.addAttribute("quotas", quotas);
        
        // Liste des clients pour le formulaire
        List<ClientDTO> clients = detectorService.getAllClients();
        model.addAttribute("clients", clients);
        
        // Statistiques
        DashboardStats stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        
        return "layout";
    }
    
    /**
     * Page Statistiques
     */
    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("pageTitle", "Statistiques");
        model.addAttribute("activeMenu", "stats");
        model.addAttribute("content", "stats");
        
        DashboardStats stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        
        return "layout";
    }
    
    /**
     * Action: Déconnecter un client
     */
    @PostMapping("/api/clients/{mac}/disconnect")
    @ResponseBody
    public String disconnectClient(@PathVariable("mac") String macAddress) {
        boolean success = enforcerService.disconnectClient(macAddress);
        return success ? "OK" : "ERROR";
    }
    
    /**
     * Action: Créer un quota
     */
    @PostMapping("/api/quotas/create")
    @ResponseBody
    public QuotaDTO createQuota(
            @RequestParam("mac") String macAddress,
            @RequestParam("time") int timeMinutes,
            @RequestParam("data") int dataMB) {
        return enforcerService.createQuota(macAddress, timeMinutes, dataMB);
    }
    
    /**
     * Action: Supprimer un quota
     */
    @DeleteMapping("/api/quotas/{mac}")
    @ResponseBody
    public String deleteQuota(@PathVariable("mac") String macAddress) {
        boolean success = enforcerService.deleteQuota(macAddress);
        return success ? "OK" : "ERROR";
    }
    
    /**
     * API: Récupérer les stats en JSON
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public DashboardStats getStatsJson() {
        return dashboardService.getDashboardStats();
    }
    
    /**
     * API: Récupérer les clients en JSON
     */
    @GetMapping("/api/clients")
    @ResponseBody
    public List<ClientDTO> getClientsJson() {
        return detectorService.getAllClients();
    }
    
    /**
     * Health check
     */
    @GetMapping("/api/health")
    @ResponseBody
    public String health() {
        boolean m1 = detectorService.isModuleOnline();
        boolean m2 = enforcerService.isModuleOnline();
        
        return String.format("{\"module1\":%b,\"module2\":%b}", m1, m2);
    }
}