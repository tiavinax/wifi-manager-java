package com.wifimanager.inspector;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Portail Captif - Page de connexion/rechargement pour les clients WiFi
 * Gère l'authentification et la redirection des clients
 */
@Component
@RestController
@RequestMapping("/portal")
public class CaptivePortal {

    // Sessions actives des clients (MAC -> Session Info)
    private final Map<String, ClientSession> activeSessions = new ConcurrentHashMap<>();
    
    // Clients autorisés (MAC -> Autorisation)
    private final Map<String, ClientAuthorization> authorizedClients = new ConcurrentHashMap<>();
    
    // Configuration du portail
    private final String portalDomain = "wifi.local";
    private final int portalPort = 8083;
    private final int sessionTimeout = 3600; // 1 heure en secondes

    /**
     * Page d'accueil du portail captif (HTML)
     */
    @GetMapping("/")
    public String getPortalPage(@RequestParam(required = false) String mac) {
        return generatePortalHTML(mac);
    }

    /**
     * Authentifier un client et créer une session
     */
    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> authenticate(
            @RequestParam String macAddress,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String voucher) {
        
        // Valider les paramètres
        if (macAddress == null || macAddress.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "Adresse MAC requise"));
        }

        // Vérifier si le client est déjà authentifié
        if (isClientAuthorized(macAddress)) {
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Client déjà authentifié",
                "macAddress", macAddress,
                "redirectUrl", "http://www.google.com"
            ));
        }

        // Authentifier avec nom d'utilisateur/mot de passe
        if (username != null && password != null) {
            if (authenticateWithCredentials(username, password)) {
                return authorizeClient(macAddress, "credentials", username);
            }
        }

        // Authentifier avec voucher
        if (voucher != null) {
            if (authenticateWithVoucher(voucher)) {
                return authorizeClient(macAddress, "voucher", voucher);
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("status", "error", "message", "Authentification échouée"));
    }

    /**
     * Vérifier le statut d'un client
     */
    @GetMapping("/status/{macAddress}")
    public ResponseEntity<Map<String, Object>> getClientStatus(@PathVariable String macAddress) {
        ClientAuthorization auth = authorizedClients.get(macAddress);
        
        if (auth == null) {
            return ResponseEntity.ok(Map.of(
                "status", "unauthorized",
                "macAddress", macAddress,
                "portalUrl", getPortalUrl(macAddress)
            ));
        }

        // Vérifier l'expiration
        if (auth.isExpired()) {
            authorizedClients.remove(macAddress);
            return ResponseEntity.ok(Map.of(
                "status", "expired",
                "macAddress", macAddress,
                "portalUrl", getPortalUrl(macAddress)
            ));
        }

        return ResponseEntity.ok(Map.of(
            "status", "authorized",
            "macAddress", macAddress,
            "authMethod", auth.authMethod,
            "authorizedAt", auth.authorizedAt.toString(),
            "expiresAt", auth.expiresAt.toString(),
            "remainingTime", Duration.between(LocalDateTime.now(), auth.expiresAt).getSeconds()
        ));
    }

    /**
     * Déconnecter un client
     */
    @PostMapping("/logout/{macAddress}")
    public ResponseEntity<Map<String, Object>> logout(@PathVariable String macAddress) {
        authorizedClients.remove(macAddress);
        activeSessions.remove(macAddress);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Client déconnecté",
            "macAddress", macAddress
        ));
    }

    /**
     * Obtenir l'URL du portail pour un client
     */
    public String getPortalUrl(String macAddress) {
        return String.format("http://%s:%d/portal?mac=%s", 
            portalDomain, portalPort, macAddress);
    }

    /**
     * Vérifier si un client est autorisé
     */
    public boolean isClientAuthorized(String macAddress) {
        ClientAuthorization auth = authorizedClients.get(macAddress);
        
        if (auth == null) {
            return false;
        }

        // Vérifier l'expiration
        if (auth.isExpired()) {
            authorizedClients.remove(macAddress);
            return false;
        }

        return true;
    }

    /**
     * Autoriser un client
     */
    private ResponseEntity<Map<String, Object>> authorizeClient(
            String macAddress, String authMethod, String identifier) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(sessionTimeout);
        
        ClientAuthorization auth = new ClientAuthorization(
            macAddress, authMethod, identifier, now, expiresAt
        );
        
        authorizedClients.put(macAddress, auth);
        
        // Créer une session
        ClientSession session = new ClientSession(macAddress, now);
        activeSessions.put(macAddress, session);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Authentification réussie",
            "macAddress", macAddress,
            "expiresAt", expiresAt.toString(),
            "redirectUrl", "http://www.google.com"
        ));
    }

    /**
     * Authentifier avec nom d'utilisateur et mot de passe
     */
    private boolean authenticateWithCredentials(String username, String password) {
        // Simulation - dans une vraie implémentation:
        // - Vérifier dans une base de données
        // - Hasher le mot de passe
        // - Implémenter des tentatives limitées
        
        // Pour la démo, accepter quelques comptes test
        Map<String, String> testAccounts = Map.of(
            "admin", "admin123",
            "user1", "password1",
            "guest", "guest123"
        );
        
        return testAccounts.containsKey(username) && 
               testAccounts.get(username).equals(password);
    }

    /**
     * Authentifier avec un voucher
     */
    private boolean authenticateWithVoucher(String voucher) {
        // Simulation - dans une vraie implémentation:
        // - Vérifier le voucher dans une base de données
        // - Marquer le voucher comme utilisé
        // - Vérifier la date d'expiration
        
        // Pour la démo, accepter des vouchers au format XXXX-XXXX-XXXX
        return voucher != null && 
               voucher.matches("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}");
    }

    /**
     * Générer la page HTML du portail
     */
    private String generatePortalHTML(String macAddress) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='fr'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Portail WiFi - Authentification</title>");
        html.append("<style>");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; }");
        html.append(".container { background: white; padding: 40px; border-radius: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); max-width: 450px; width: 100%; }");
        html.append("h1 { color: #667eea; margin-bottom: 10px; text-align: center; }");
        html.append(".subtitle { text-align: center; color: #666; margin-bottom: 30px; }");
        html.append(".form-group { margin-bottom: 20px; }");
        html.append("label { display: block; margin-bottom: 8px; color: #333; font-weight: 500; }");
        html.append("input { width: 100%; padding: 12px; border: 2px solid #e0e0e0; border-radius: 8px; font-size: 16px; transition: border-color 0.3s; }");
        html.append("input:focus { outline: none; border-color: #667eea; }");
        html.append("button { width: 100%; padding: 14px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }");
        html.append("button:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4); }");
        html.append(".divider { text-align: center; margin: 25px 0; color: #999; }");
        html.append(".info { background: #f5f5f5; padding: 15px; border-radius: 8px; margin-top: 20px; font-size: 14px; color: #666; }");
        html.append(".mac-address { font-family: monospace; background: #e0e0e0; padding: 2px 6px; border-radius: 4px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<h1>🌐 Portail WiFi</h1>");
        html.append("<p class='subtitle'>Veuillez vous authentifier pour accéder à Internet</p>");
        html.append("<form id='authForm'>");
        html.append("<div class='form-group'>");
        html.append("<label>Nom d'utilisateur</label>");
        html.append("<input type='text' id='username' name='username' placeholder='Entrez votre nom d\\'utilisateur'>");
        html.append("</div>");
        html.append("<div class='form-group'>");
        html.append("<label>Mot de passe</label>");
        html.append("<input type='password' id='password' name='password' placeholder='Entrez votre mot de passe'>");
        html.append("</div>");
        html.append("<button type='submit'>Se connecter</button>");
        html.append("<div class='divider'>OU</div>");
        html.append("<div class='form-group'>");
        html.append("<label>Code Voucher</label>");
        html.append("<input type='text' id='voucher' name='voucher' placeholder='XXXX-XXXX-XXXX'>");
        html.append("</div>");
        html.append("<button type='button' onclick='authenticateVoucher()'>Utiliser un Voucher</button>");
        html.append("</form>");
        html.append("<div class='info'>");
        html.append("<strong>Votre appareil:</strong><br>");
        html.append("MAC: <span class='mac-address'>").append(macAddress != null ? macAddress : "Unknown").append("</span>");
        html.append("</div>");
        html.append("</div>");
        html.append("<script>");
        html.append("const macAddress = '").append(macAddress != null ? macAddress : "Unknown").append("';");
        html.append("document.getElementById('authForm').addEventListener('submit', async (e) => {");
        html.append("e.preventDefault();");
        html.append("const username = document.getElementById('username').value;");
        html.append("const password = document.getElementById('password').value;");
        html.append("if (!username || !password) { alert('Veuillez remplir tous les champs'); return; }");
        html.append("try {");
        html.append("const response = await fetch('/portal/auth', {");
        html.append("method: 'POST',");
        html.append("headers: { 'Content-Type': 'application/x-www-form-urlencoded' },");
        html.append("body: `macAddress=${macAddress}&username=${username}&password=${password}`");
        html.append("});");
        html.append("const data = await response.json();");
        html.append("if (data.status === 'success') {");
        html.append("alert('Authentification réussie! Vous allez être redirigé...');");
        html.append("window.location.href = data.redirectUrl || 'http://www.google.com';");
        html.append("} else {");
        html.append("alert('Erreur: ' + data.message);");
        html.append("}");
        html.append("} catch (error) {");
        html.append("alert('Erreur de connexion au serveur');");
        html.append("}");
        html.append("});");
        html.append("async function authenticateVoucher() {");
        html.append("const voucher = document.getElementById('voucher').value;");
        html.append("if (!voucher) { alert('Veuillez entrer un code voucher'); return; }");
        html.append("try {");
        html.append("const response = await fetch('/portal/auth', {");
        html.append("method: 'POST',");
        html.append("headers: { 'Content-Type': 'application/x-www-form-urlencoded' },");
        html.append("body: `macAddress=${macAddress}&voucher=${voucher}`");
        html.append("});");
        html.append("const data = await response.json();");
        html.append("if (data.status === 'success') {");
        html.append("alert('Authentification réussie! Vous allez être redirigé...');");
        html.append("window.location.href = data.redirectUrl || 'http://www.google.com';");
        html.append("} else {");
        html.append("alert('Erreur: ' + data.message);");
        html.append("}");
        html.append("} catch (error) {");
        html.append("alert('Erreur de connexion au serveur');");
        html.append("}");
        html.append("}");
        html.append("</script>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }

    /**
     * Nettoyer les sessions expirées (tâche planifiée)
     */
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        
        authorizedClients.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        activeSessions.entrySet().removeIf(entry -> {
            ClientSession session = entry.getValue();
            return Duration.between(session.createdAt, now).getSeconds() > sessionTimeout;
        });
    }

    /**
     * Classe interne: Autorisation client
     */
    @SuppressWarnings("unused")
    private static class ClientAuthorization {
        final String macAddress;
        final String authMethod;
        final String identifier;
        final LocalDateTime authorizedAt;
        final LocalDateTime expiresAt;

        ClientAuthorization(String macAddress, String authMethod, String identifier,
                          LocalDateTime authorizedAt, LocalDateTime expiresAt) {
            this.macAddress = macAddress;
            this.authMethod = authMethod;
            this.identifier = identifier;
            this.authorizedAt = authorizedAt;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /**
     * Classe interne: Session client
     */
    @SuppressWarnings("unused")
    private static class ClientSession {
        final String macAddress;
        final LocalDateTime createdAt;
        LocalDateTime lastActivity;

        ClientSession(String macAddress, LocalDateTime createdAt) {
            this.macAddress = macAddress;
            this.createdAt = createdAt;
            this.lastActivity = createdAt;
        }

        void updateActivity() {
            this.lastActivity = LocalDateTime.now();
        }
    }
}
