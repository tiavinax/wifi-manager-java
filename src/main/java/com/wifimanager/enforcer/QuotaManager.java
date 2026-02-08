package com.wifimanager.enforcer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* GESTIONNAIRE DES QUOTAS*/
public class QuotaManager {
    
    private static final String DATA_DIR = "target/data";           // Répertoire de stockage
    private static final String QUOTAS_FILE = "quotas.json";        // Fichier JSON
    private static final int AUTO_SAVE_INTERVAL_MS = 10_000;        // 10 secondes
    
    // STOCKAGE EN MÉMOIRE 
    // ConcurrentHashMap = thread-safe, permet accès simultanés
    private final Map<String, Quota> quotas = new ConcurrentHashMap<>();
    
    // SÉRIALISATION JSON (Gson)
    private final Gson gson;
    
    // THREAD AUTO-SAVE (économise ressources, évite sauvegardes trop fréquentes)
    private Thread autoSaveThread;
    private volatile boolean running = false;  // volatile = visible par tous les threads
    
    // STATISTIQUES
    private int totalQuotasCreated = 0;
    private LocalDateTime lastSaveTime = null;
    
    // CONSTRUCTEUR
    
    public QuotaManager() {
        // Configurer Gson avec support LocalDateTime
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()  // JSON lisible avec indentation
            .create();
        
        // Créer le répertoire de données
        createDataDirectory();
        
        // Charger les quotas existants
        load();
        
        // Démarrer l'auto-sauvegarde
        startAutoSave();
        
        System.out.println("[QuotaManager] ✅ Initialisé avec " + quotas.size() + " quota(s) existant(s)");
    }
    
    // ========== MÉTHODES PRINCIPALES ==========
    
    /* Définir un nouveau quota pour un client.*/
    public void setQuota(String macAddress, int timeMinutes, int dataMB) {
        // Validation des paramètres
        if (macAddress == null || macAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("MAC address ne peut pas être vide");
        }
        if (timeMinutes < 0) {
            throw new IllegalArgumentException("Temps ne peut pas être négatif: " + timeMinutes);
        }
        if (dataMB < 0) {
            throw new IllegalArgumentException("Données ne peuvent pas être négatives: " + dataMB);
        }
        
        // Normaliser la MAC (majuscules, pas d'espaces)
        macAddress = macAddress.trim().toUpperCase();
        
        // Créer le nouveau quota
        Quota quota = new Quota(macAddress, timeMinutes, dataMB);
        
        // Stocker dans la map
        Quota oldQuota = quotas.put(macAddress, quota);
        
        // Statistiques
        if (oldQuota == null) {
            totalQuotasCreated++;
            System.out.println("[QuotaManager] ➕ Nouveau quota créé: " + quota);
        } else {
            System.out.println("[QuotaManager] 🔄 Quota mis à jour: " + oldQuota + " → " + quota);
        }
    }
    
    /* Récupérer le quota actuel d'un client.*/
    public Quota getQuota(String macAddress) {
        if (macAddress == null) return null;
        macAddress = macAddress.trim().toUpperCase();
        return quotas.get(macAddress);
    }
    
    /* Consommer du temps pour un client.*/
    public boolean consumeTime(String macAddress, int minutes) {
        if (macAddress == null || minutes < 0) return false;
        
        macAddress = macAddress.trim().toUpperCase();
        Quota quota = quotas.get(macAddress);
        
        if (quota == null) {
            System.out.println("[QuotaManager] ⚠️ Impossible de consommer temps: client " + macAddress + " introuvable");
            return false;
        }
        
        // Incrémenter la consommation
        int oldConsumed = quota.getConsumedTimeMinutes();
        quota.setConsumedTimeMinutes(oldConsumed + minutes);
        
        System.out.printf("[QuotaManager] ⏱️ %s a consommé %d min (total: %d/%d min)%n",
            macAddress, minutes, quota.getConsumedTimeMinutes(), quota.getInitialTimeMinutes());
        
        // Alerter si quota dépassé
        if (quota.isTimeExceeded()) {
            System.out.println("[QuotaManager] 🚨 QUOTA TEMPS DÉPASSÉ pour " + macAddress);
        }
        
        return true;
    }
    
    /* Consommer des données pour un client.*/
    public boolean consumeData(String macAddress, int megabytes) {
        if (macAddress == null || megabytes < 0) return false;
        
        macAddress = macAddress.trim().toUpperCase();
        Quota quota = quotas.get(macAddress);
        
        if (quota == null) {
            System.out.println("[QuotaManager] ⚠️ Impossible de consommer données: client " + macAddress + " introuvable");
            return false;
        }
        
        // Incrémenter la consommation
        int oldConsumed = quota.getConsumedDataMB();
        quota.setConsumedDataMB(oldConsumed + megabytes);
        
        System.out.printf("[QuotaManager] 📊 %s a consommé %d MB (total: %d/%d MB)%n",
            macAddress, megabytes, quota.getConsumedDataMB(), quota.getInitialDataMB());
        
        // Alerter si quota dépassé
        if (quota.isDataExceeded()) {
            System.out.println("[QuotaManager] 🚨 QUOTA DONNÉES DÉPASSÉ pour " + macAddress);
        }
        
        return true;
    }
    
    /*Vérifier si un client a dépassé ses quotas.*/
    public boolean isExceeded(String macAddress) {
        Quota quota = getQuota(macAddress);
        return quota != null && quota.isExceeded();
    }
    
    /* Obtenir tous les clients qui ont dépassé leurs quotas.*/
    public List<String> getExceededClients() {
        List<String> exceeded = new ArrayList<>();
        for (Quota quota : quotas.values()) {
            if (quota.isExceeded()) {
                exceeded.add(quota.getMacAddress());
            }
        }
        return exceeded;
    }
    
    /*  Obtenir tous les quotas (copie).*/
    public Map<String, Quota> getAllQuotas() {
        return new HashMap<>(quotas);
    }
    
    /* Supprimer le quota d'un client.*/
    public boolean removeQuota(String macAddress) {
        if (macAddress == null) return false;
        macAddress = macAddress.trim().toUpperCase();
        Quota removed = quotas.remove(macAddress);
        if (removed != null) {
            System.out.println("[QuotaManager] ❌ Quota supprimé: " + macAddress);
            return true;
        }
        return false;
    }
    
    // PERSISTENCE (SAUVEGARDE/CHARGEMENT)
    public synchronized void persist() {
        try {
            Path filePath = Paths.get(DATA_DIR, QUOTAS_FILE);
            
            // Convertir la Map en JSON
            String json = gson.toJson(quotas);
            
            // Écrire dans le fichier
            Files.writeString(filePath, json);
            
            lastSaveTime = LocalDateTime.now();
            System.out.println("[QuotaManager] 💾 Sauvegarde réussie: " + quotas.size() + " quota(s) dans " + filePath);
            
        } catch (IOException e) {
            System.err.println("[QuotaManager] ❌ Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /* Charger les quotas depuis le disque (fichier JSON).*/
    public synchronized void load() {
        try {
            Path filePath = Paths.get(DATA_DIR, QUOTAS_FILE);
            
            // Vérifier si le fichier existe
            if (!Files.exists(filePath)) {
                System.out.println("[QuotaManager] ℹ️ Aucun fichier de quotas trouvé, démarrage à zéro");
                return;
            }
            
            // Lire le fichier JSON
            String json = Files.readString(filePath);
            
            // Convertir JSON → Map
            Type type = new TypeToken<ConcurrentHashMap<String, Quota>>(){}.getType();
            Map<String, Quota> loadedQuotas = gson.fromJson(json, type);
            
            if (loadedQuotas != null) {
                quotas.clear();
                quotas.putAll(loadedQuotas);
                System.out.println("[QuotaManager] 📂 Chargement réussi: " + quotas.size() + " quota(s) depuis " + filePath);
            }
            
        } catch (IOException e) {
            System.err.println("[QuotaManager] ❌ Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // AUTO-SAUVEGARDE 
    private void startAutoSave() {
        running = true;
        autoSaveThread = new Thread(() -> {
            System.out.println("[QuotaManager] 🔄 Auto-sauvegarde démarrée (intervalle: " + AUTO_SAVE_INTERVAL_MS + "ms)");
            
            while (running) {
                try {
                    Thread.sleep(AUTO_SAVE_INTERVAL_MS);
                    persist();
                } catch (InterruptedException e) {
                    System.out.println("[QuotaManager] ⚠️ Thread auto-sauvegarde interrompu");
                    break;
                }
            }
            
            System.out.println("[QuotaManager] 🛑 Auto-sauvegarde arrêtée");
        });
        
        autoSaveThread.setDaemon(true);  // Thread daemon = s'arrête avec le programme
        autoSaveThread.setName("QuotaManager-AutoSave");
        autoSaveThread.start();
    }
    
    /* Arrêter l'auto-sauvegarde et faire une dernière sauvegarde. */
    public void shutdown() {
        System.out.println("[QuotaManager] 🛑 Arrêt en cours...");
        running = false;
        
        // Attendre que le thread se termine
        if (autoSaveThread != null) {
            try {
                autoSaveThread.join(2000);  // Max 2 secondes
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Sauvegarde finale
        persist();
        System.out.println("[QuotaManager] ✅ Arrêt terminé");
    }
    
    /*Créer le répertoire de données s'il n'existe pas.*/
    private void createDataDirectory() {
        try {
            Path path = Paths.get(DATA_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("[QuotaManager] 📁 Répertoire créé: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[QuotaManager] ❌ Impossible de créer le répertoire: " + e.getMessage());
        }
    }
    
    /*Statistiques du gestionnaire.*/
    public void printStats() {
        System.out.println("\n========== STATISTIQUES QUOTAMANAGER ==========");
        System.out.println("Total quotas actifs: " + quotas.size());
        System.out.println("Total quotas créés: " + totalQuotasCreated);
        System.out.println("Clients dépassant quota: " + getExceededClients().size());
        System.out.println("Dernière sauvegarde: " + (lastSaveTime != null ? lastSaveTime : "Jamais"));
        System.out.println("==============================================\n");
    }
    
    // MAIN DE TEST

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== TEST QUOTAMANAGER ==========\n");
        
        QuotaManager manager = new QuotaManager();
        
        // Test 1: Créer des quotas
        System.out.println("\n--- Test 1: Création de quotas ---");
        manager.setQuota("AA:BB:CC:DD:EE:FF", 5, 100);
        manager.setQuota("11:22:33:44:55:66", 10, 200);
        
        // Test 2: Consulter un quota
        System.out.println("\n--- Test 2: Consultation ---");
        Quota q1 = manager.getQuota("AA:BB:CC:DD:EE:FF");
        System.out.println("Quota récupéré: " + q1);
        
        // Test 3: Consommer du temps
        System.out.println("\n--- Test 3: Consommation temps ---");
        manager.consumeTime("AA:BB:CC:DD:EE:FF", 1);
        manager.consumeTime("AA:BB:CC:DD:EE:FF", 2);
        manager.consumeTime("AA:BB:CC:DD:EE:FF", 3);  // Total: 6 minutes > 5 minutes allouées
        
        // Test 4: Consommer des données
        System.out.println("\n--- Test 4: Consommation données ---");
        manager.consumeData("11:22:33:44:55:66", 50);
        manager.consumeData("11:22:33:44:55:66", 100);  // Total: 150 MB < 200 MB
        
        // Test 5: Vérifier dépassements
        System.out.println("\n--- Test 5: Vérification dépassements ---");
        System.out.println("AA:BB:CC:DD:EE:FF dépassé? " + manager.isExceeded("AA:BB:CC:DD:EE:FF"));
        System.out.println("11:22:33:44:55:66 dépassé? " + manager.isExceeded("11:22:33:44:55:66"));
        
        // Test 6: Liste des clients dépassés
        System.out.println("\n--- Test 6: Clients dépassés ---");
        List<String> exceeded = manager.getExceededClients();
        System.out.println("Clients à déconnecter: " + exceeded);
        
        // Test 7: Statistiques
        System.out.println("\n--- Test 7: Statistiques ---");
        manager.printStats();
        
        // Test 8: Sauvegarde manuelle
        System.out.println("\n--- Test 8: Sauvegarde manuelle ---");
        manager.persist();
        
        // Test 9: Attendre l'auto-sauvegarde
        System.out.println("\n--- Test 9: Auto-sauvegarde (attente 12s) ---");
        Thread.sleep(12000);
        
        // Test 10: Arrêt propre
        System.out.println("\n--- Test 10: Arrêt ---");
        manager.shutdown();
        
        System.out.println("\n========== FIN DES TESTS ==========");
    }
}
