package com.wifimanager.enforcer;

import com.wifimanager.shared.communication.ModuleBridge;

public class TestIntegrationAvancee {
    public static void main(String[] args) throws Exception {
         System.out.println("🧪 TEST D'INTÉGRATION MODULE 1 + MODULE 2\n");
        
        // 1. Vérifier que les deux modules sont en ligne
        System.out.println("1. Vérification des modules:");
        ModuleBridge.checkAllModules();
        
        // 2. Récupérer les clients du Module 1
        System.out.println("\n2. Récupération des clients depuis Module 1:");
        String clients = ModuleBridge.getClientsFromDetector();
        System.out.println("   " + clients);
        
        // 3. Définir un quota via Module 2
        System.out.println("\n3. Définition d'un quota via Module 2:");
        String testMac = "D8:42:F7:2A:20:4F";
        ModuleBridge.QuotaResponse response = ModuleBridge.setQuotaViaEnforcer(
            testMac, 3, 50);
        
        if (response.success) {
            System.out.println("   ✅ Quota défini: " + response.quotaId);
            System.out.println("   MAC: " + response.macAddress);
            System.out.println("   Temps: " + response.timeMinutes + " min");
            System.out.println("   Données: " + response.dataMB + " MB");
        } else {
            System.out.println("   ❌ Échec: " + response.message);
        }
        
        // 4. Vérifier le quota créé
        System.out.println("\n4. Vérification du quota:");
        ModuleBridge.QuotaResponse quota = ModuleBridge.getQuotaFromEnforcer(testMac);
        if (quota.success) {
            System.out.println("   " + quota.toString());
        }
        
        // 5. Statistiques du Module 2
        System.out.println("\n5. Statistiques Module 2:");
        var stats = ModuleBridge.getEnforcerStats();
        System.out.println("   Total quotas: " + stats.get("totalQuotas"));
        System.out.println("   Quotas actifs: " + stats.get("activeQuotas"));
        System.out.println("   Mode: " + stats.get("trafficControllerMode"));
        
        System.out.println("\n✅ Test d'intégration terminé!");
    
    }
}