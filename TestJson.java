package com.wifimanager.enforcer;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestJson {
    public static void main(String[] args) throws Exception {
        System.out.println("📄 TEST PERSISTENCE JSON");
        
        QuotaManager manager = new QuotaManager();
        
        // 1. Créer quota
        manager.setQuota("TEST:MAC:01:02:03:04", 5, 100);
        System.out.println("✅ Quota créé");
        
        // 2. Consommer
        manager.consumeTime("TEST:MAC:01:02:03:04", 3);
        manager.consumeData("TEST:MAC:01:02:03:04", 40);
        System.out.println("✅ Temps et données consommés");
        
        // 3. Attendre sauvegarde
        Thread.sleep(2000);
        
        // 4. Lire fichier
        String json = new String(Files.readAllBytes(Paths.get("data/quotas.json")));
        System.out.println("\n📁 Contenu du fichier:");
        System.out.println(json);
        
        // 5. Vérifier valeurs
        if (json.contains("\"timeUsedMinutes\":3") && 
            json.contains("\"dataUsedMB\":40") &&
            json.contains("\"isActive\":true")) {
            System.out.println("\n✅ JSON CORRECT !");
        } else {
            System.out.println("\n⚠️  Problème dans le JSON");
        }
    }
}
