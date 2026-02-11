/**
 * WiFi Manager - Page Clients
 * Version CORRIGÉE - Utilisation du proxy DashboardServer
 */

let clientsData = [];
let quotasData = [];
let currentMacAddress = '';

document.addEventListener('DOMContentLoaded', function() {
    console.log('📋 Page Clients chargée');
    
    // Afficher le contenu
    const content = document.getElementById('pageContent');
    if (content) content.style.display = 'block';
    
    // Charger les données
    loadClients();
    loadAllQuotas();
    
    // Configurer les événements
    setupEventListeners();
    
    // Rafraîchissement automatique toutes les 10s
    setInterval(loadClients, 10000);
    setInterval(loadAllQuotas, 15000);
});

function setupEventListeners() {
    // Recherche en temps réel
    const searchInput = document.getElementById('searchClients');
    if (searchInput) {
        searchInput.addEventListener('keyup', filterClients);
    }
    
    // Bouton créer quota
    const createBtn = document.getElementById('createQuotaBtn');
    if (createBtn) {
        createBtn.addEventListener('click', createQuota);
    }
}

// ========== MODULE 1 - DETECTOR API (via proxy) ==========

function loadClients() {
    // ✅ UTILISE LE PROXY - Plus de CORS !
    fetch('/api/detector/api/clients')
        .then(response => {
            if (!response.ok) throw new Error('Module 1 hors ligne');
            return response.text(); // API retourne du texte JSON
        })
        .then(text => {
            try {
                clientsData = JSON.parse(text);
                console.log('✅ Clients chargés:', clientsData.length);
                updateClientsTable();
                updateClientsStats();
                updateClientsBadge();
            } catch (e) {
                console.error('Erreur parsing JSON:', e);
                clientsData = [];
                updateClientsTable();
            }
        })
        .catch(error => {
            console.error('Erreur chargement clients:', error);
            showNotification('❌ Module 1 (Détection) hors ligne', 'error');
            
            const tbody = document.getElementById('clientsTableBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center text-muted py-5">
                            <i class="bi bi-exclamation-triangle-fill text-warning" style="font-size: 3rem;"></i>
                            <p class="mt-3">Module 1 (Détection) non disponible</p>
                            <p class="small">Vérifiez que le module est démarré sur le port 8081</p>
                            <button class="btn btn-primary mt-3" onclick="loadClients()">
                                <i class="bi bi-arrow-clockwise"></i> Réessayer
                            </button>
                        </td>
                    </tr>
                `;
            }
        });
}

// ========== MODULE 2 - ENFORCER API (via proxy) ==========

function loadAllQuotas() {
    // ✅ UTILISE LE PROXY - GET /quota/ (sans MAC = tous les quotas)
    fetch('/api/enforcer/quota/')
        .then(response => {
            if (!response.ok) throw new Error('Module 2 hors ligne');
            return response.text();
        })
        .then(text => {
            try {
                const data = JSON.parse(text);
                // La réponse est { "count": X, "quotas": [...] }
                quotasData = data.quotas || [];
                console.log('✅ Quotas chargés:', quotasData.length);
                updateQuotasBadge();
                updateClientsTable(); // Mettre à jour l'affichage des quotas
            } catch (e) {
                console.error('Erreur parsing quotas:', e);
                quotasData = [];
            }
        })
        .catch(error => {
            console.error('Erreur chargement quotas:', error);
            quotasData = [];
        });
}

function loadQuotaForClient(mac) {
    // ✅ UTILISE LE PROXY - GET /quota/{mac}
    return fetch('/api/enforcer/quota/' + mac)
        .then(response => {
            if (response.status === 404) return null;
            if (!response.ok) throw new Error('Erreur');
            return response.text();
        })
        .then(text => {
            if (!text) return null;
            try {
                return JSON.parse(text);
            } catch (e) {
                return null;
            }
        })
        .catch(() => null);
}

function createQuota() {
    const mac = currentMacAddress;
    const time = document.getElementById('modalTimeMinutes').value;
    const data = document.getElementById('modalDataMB').value;
    
    if (!mac) {
        showNotification('❌ Adresse MAC requise', 'error');
        return;
    }
    
    // Format exact attendu par l'API
    const quotaData = {
        mac: mac,
        timeMinutes: parseInt(time),
        dataMB: parseInt(data)
    };
    
    console.log('📤 Création quota:', quotaData);
    
    // ✅ UTILISE LE PROXY - POST /quota/set
    fetch('/api/enforcer/quota/set', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(quotaData)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                try {
                    const error = JSON.parse(text);
                    throw new Error(error.message || 'Erreur création quota');
                } catch (e) {
                    throw new Error('Erreur ' + response.status);
                }
            });
        }
        return response.text();
    })
    .then(text => {
        try {
            const data = JSON.parse(text);
            console.log('✅ Quota créé:', data);
            showNotification('✅ Quota créé avec succès', 'success');
            
            // Fermer le modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('quotaModal'));
            if (modal) modal.hide();
            
            // Recharger les données
            loadAllQuotas();
            setTimeout(() => loadClients(), 1000);
        } catch (e) {
            console.error('Erreur parsing réponse:', e);
            showNotification('✅ Quota créé (réponse non parsée)', 'success');
            loadAllQuotas();
        }
    })
    .catch(error => {
        console.error('❌ Erreur:', error);
        showNotification('❌ ' + error.message, 'error');
    });
}

function disconnectClient(mac) {
    if (confirm(`Voulez-vous déconnecter le client ${mac} ?`)) {
        // ✅ UTILISE LE PROXY - POST /disconnect/{mac}
        fetch('/api/enforcer/disconnect/' + mac, {
            method: 'POST'
        })
        .then(response => {
            if (!response.ok) throw new Error('Échec déconnexion');
            return response.text();
        })
        .then(text => {
            try {
                const data = JSON.parse(text);
                showNotification('🔌 ' + (data.message || 'Client déconnecté'), 'success');
            } catch (e) {
                showNotification('🔌 Client déconnecté', 'success');
            }
            // Mettre à jour l'affichage
            setTimeout(() => loadClients(), 1000);
        })
        .catch(error => {
            console.error('❌ Erreur déconnexion:', error);
            showNotification('❌ Erreur déconnexion', 'error');
        });
    }
}

// ========== AFFICHAGE ==========

function showQuotaForm(mac) {
    currentMacAddress = mac;
    document.getElementById('modalMacAddress').value = mac;
    
    // Réinitialiser les valeurs par défaut
    document.getElementById('modalTimeMinutes').value = '60';
    document.getElementById('modalDataMB').value = '500';
    
    new bootstrap.Modal(document.getElementById('quotaModal')).show();
}

function updateClientsTable() {
    const tbody = document.getElementById('clientsTableBody');
    if (!tbody) return;
    
    if (!clientsData || clientsData.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-muted py-5">
                    <i class="bi bi-wifi-off" style="font-size: 3rem;"></i>
                    <p class="mt-3">Aucun client connecté</p>
                    <p class="small">Le scan réseau est en cours...</p>
                </td>
            </tr>
        `;
        return;
    }
    
    let html = '';
    clientsData.forEach(client => {
        const quota = findQuotaForClient(client.macAddress);
        const quotaHtml = getQuotaHtml(quota);
        const statusClass = client.active ? 'status-active' : 'status-inactive';
        const statusText = client.active ? 'Actif' : 'Inactif';
        const totalBytes = formatBytes(client.totalBytes || 0);
        const deviceType = getDeviceType(client.macAddress);
        
        html += `
            <tr>
                <td>
                    <strong>${client.macAddress || 'N/A'}</strong>
                    <br><small class="text-muted">${deviceType}</small>
                </td>
                <td>${client.ipAddress || 'N/A'}</td>
                <td>${client.hostname || '-'}</td>
                <td>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                </td>
                <td>${totalBytes}</td>
                <td>${quotaHtml}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="disconnectClient('${client.macAddress}')" 
                            title="Déconnecter">
                        <i class="bi bi-power"></i>
                    </button>
                    <button class="btn btn-sm btn-primary" onclick="showQuotaForm('${client.macAddress}')"
                            title="Définir un quota">
                        <i class="bi bi-sliders2"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

function findQuotaForClient(mac) {
    if (!quotasData || !Array.isArray(quotasData)) return null;
    return quotasData.find(q => q.macAddress === mac);
}

function getQuotaHtml(quota) {
    if (!quota) {
        return '<span class="badge bg-secondary">Aucun quota</span>';
    }
    
    const timeRemaining = quota.timeRemainingMinutes || 0;
    const dataRemaining = quota.dataRemainingMB || 0;
    const isExceeded = quota.isExceeded || false;
    
    if (isExceeded) {
        return '<span class="badge bg-danger">Quota dépassé</span>';
    }
    
    return `
        <span class="badge bg-info">
            ${timeRemaining} min / ${dataRemaining} Mo
        </span>
        <br>
        <small class="text-muted">${quota.timeUsedMinutes || 0}/${quota.timeLimitMinutes || 0} min</small>
        <br>
        <small class="text-muted">${quota.dataUsedMB || 0}/${quota.dataLimitMB || 0} Mo</small>
    `;
}

function updateClientsStats() {
    const total = clientsData.length;
    const active = clientsData.filter(c => c.active).length;
    const totalTraffic = clientsData.reduce((sum, c) => sum + (c.totalBytes || 0), 0);
    
    document.getElementById('totalClients').textContent = total;
    document.getElementById('activeClients').textContent = active;
    document.getElementById('totalTraffic').textContent = formatBytes(totalTraffic);
}

function updateClientsBadge() {
    const badge = document.getElementById('clientsCount');
    if (badge) {
        const active = clientsData.filter(c => c.active).length;
        badge.textContent = active;
        badge.style.background = active > 0 ? '#e63946' : '#6c757d';
    }
}

function updateQuotasBadge() {
    const badge = document.getElementById('quotasCount');
    if (badge && quotasData) {
        const active = quotasData.filter(q => q.isActive && !q.isExceeded).length;
        badge.textContent = active;
        badge.style.background = active > 0 ? '#e63946' : '#6c757d';
    }
}

function filterClients() {
    const searchTerm = document.getElementById('searchClients').value.toLowerCase();
    const rows = document.querySelectorAll('#clientsTable tbody tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(searchTerm) ? '' : 'none';
    });
}

function getDeviceType(mac) {
    if (!mac) return 'Inconnu';
    mac = mac.toUpperCase();
    
    if (mac.startsWith('D8:42:F7')) return 'Routeur TP-Link';
    if (mac.startsWith('7C:5C:F8')) return 'PC Portable HP';
    if (mac.startsWith('00:11:22')) return 'Équipement Cisco';
    if (mac.startsWith('AA:BB:CC')) return 'Appareil test';
    if (mac.startsWith('64:BC:0C') || mac.startsWith('3C:CD:5D')) return 'Smartphone';
    if (mac.startsWith('54:60:B8')) return 'Ordinateur';
    
    return 'Appareil réseau';
}

function formatBytes(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function showNotification(message, type) {
    const notificationArea = document.getElementById('notificationArea');
    if (!notificationArea) {
        alert(message);
        return;
    }
    
    const icons = {
        success: 'check-circle-fill',
        error: 'exclamation-circle-fill',
        warning: 'exclamation-triangle-fill',
        info: 'info-circle-fill'
    };
    
    notificationArea.style.display = 'block';
    notificationArea.className = 'notification-area alert alert-' + type;
    notificationArea.innerHTML = `
        <div class="d-flex align-items-center">
            <i class="bi bi-${icons[type] || 'bell-fill'} me-2"></i>
            <span>${message}</span>
            <button type="button" class="btn-close ms-auto" onclick="this.parentElement.parentElement.style.display='none'"></button>
        </div>
    `;
    
    setTimeout(() => {
        notificationArea.style.display = 'none';
    }, 5000);
}