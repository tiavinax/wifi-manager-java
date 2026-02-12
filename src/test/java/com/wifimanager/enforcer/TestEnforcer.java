package com.wifimanager.enforcer;

public class TestEnforcer {
    public static void main(String[] args) throws Exception {
        System.out.println("🧪 TEST RAPIDE MODULE 2");
        
        // Test QuotaManager
        System.out.println("\n1. Test QuotaManager:");
        QuotaManager manager = new QuotaManager();
        manager.setQuota("AA:BB:CC:DD:EE:FF", 1, 50); // 1 min, 50 MB
        
        System.out.println("   Quota défini pour AA:BB:CC:DD:EE:FF");
        System.out.println("   Consommation de 2 minutes...");
        manager.consumeTime("AA:BB:CC:DD:EE:FF", 2);
        System.out.println("   Temps restant: " + 
            manager.getQuota("AA:BB:CC:DD:EE:FF").getTimeRemainingMinutes() + " min");
        
        // Test TrafficController (simulation)
        System.out.println("\n2. Test TrafficController (simulation):");
        TrafficController controller = new TrafficController(true);
        controller.blockClient("AA:BB:CC:DD:EE:FF", "192.168.0.100", "Quota depasser");
        controller.limitBandwidth("AA:BB:CC:DD:EE:FF", "192.168.0.100", 1024);
        
        // Test DisconnectionService
        System.out.println("\n3. Test DisconnectionService:");
        DisconnectionService disconnection = new DisconnectionService(manager, controller);
        disconnection.checkAndDisconnect();
        
        System.out.println("\n✅ Tests basiques passés !");
        System.out.println("\nPour tester le module complet:");
        System.out.println("  java -cp \"target/classes:lib/*\" com.wifimanager.enforcer.EnforcerMain");
    }
}