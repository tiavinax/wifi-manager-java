#!/bin/bash
echo "🔨 Compilation du module détection..."
cd ~/RESEAU-ADMIN_SYS/wifi-manager-java

# Nettoie
rm -rf target/classes
mkdir -p target/classes

# Compile toutes les classes dans le bon ordre
echo "1. Compilation des classes partagées..."
javac -d target/classes src/main/java/com/wifimanager/shared/model/Client.java
javac -d target/classes src/main/java/com/wifimanager/shared/config/Constants.java
javac -d target/classes src/main/java/com/wifimanager/shared/api/ApiResponse.java

echo "2. Compilation du module détection..."
javac -d target/classes \
      -cp "target/classes:lib/slf4j-api-2.0.3.jar" \
      src/main/java/com/wifimanager/detector/*.java

echo "✅ Compilation terminée"

echo ""
echo "🚀 Test 1: Création d'un client simple..."
java -cp "target/classes:lib/slf4j-api-2.0.3.jar:lib/slf4j-simple-2.0.3.jar" \
     com.wifimanager.detector.DetectorMain --help

echo ""
echo "🚀 Test 2: Scanner réseau simple..."
cat > TestScannerSimple.java << 'EOF'
package com.wifimanager.detector;

public class TestScannerSimple {
    public static void main(String[] args) {
        System.out.println("=== Test Scanner Simple ===");
        try {
            ArpScanner scanner = new ArpScanner("lo", "127.0.0.0/24");
            System.out.println("✅ Scanner créé avec succès");
            System.out.println("Interface: " + scanner.getNetworkInterface());
            System.out.println("Subnet: " + scanner.getSubnet());
            
            ClientDiscoveryService service = new ClientDiscoveryService();
            System.out.println("✅ Service créé avec succès");
            
            System.out.println("Tentative de scan...");
            service.discoverClients();
            
            int total = service.getTotalClientCount();
            System.out.println("Clients trouvés: " + total);
            
            if (total > 0) {
                System.out.println("Détails:");
                for (var client : service.getAllClients()) {
                    System.out.println("- " + client.getMacAddress() + " -> " + client.getIpAddress());
                }
            } else {
                System.out.println("ℹ️ Aucun client trouvé (peut être normal)");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }
}
EOF

# Compile et exécute le test
javac -d target/classes -cp "target/classes:lib/slf4j-api-2.0.3.jar" TestScannerSimple.java
java -cp "target/classes:lib/slf4j-api-2.0.3.jar:lib/slf4j-simple-2.0.3.jar" \
     com.wifimanager.detector.TestScannerSimple
rm TestScannerSimple.java