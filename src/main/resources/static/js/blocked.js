/**
 * WiFi Manager - Page Clients bloqués
 * Gestion de l'affichage et déblocage des clients
 */

let blockedClientsData = [];

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚫 Page Clients bloqués chargée');
    
    // Afficher le contenu
    const content = document.getElementById('pageContent');
    if (content) content.style.display = 'block';
    
    // Charger les données
    loadBlockedClients();
    
    // Configurer les événements
    setupEventListeners();
    
    // Rafraîchissement automatique toutes les 10s
    setInterval(loadBlockedClients, 10000);
});

function setupEventListeners() {
    // Recherche en temps réel
    const searchInput = document.getElementById('searchBlocked');
    if (searchInput) {
        searchInput.addEventListener('keyup', filterBlocked);
    }
    
    // Bouton confirmer déblocage
    const confirmBtn = document.getElementById('confirmUnblockBtn');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', confirmUnblock);
    }
    
    // Bouton confirmer nettoyage
    const cleanBtn = document.getElementById('confirmCleanBtn');
    if (cleanBtn) {
        cleanBtn.addEventListener('click', confirmClean);
    }
}

/**
 * Charger la liste des clients bloqués depuis Module 2
 */
function loadBlockedClients() {
    fetch('/api/enforcer/blocked')
        .then(response => {
            if (!response.ok) throw new Error('Module 2 hors ligne');
            return response.json();
        })
        .then(data => {
            console.log('✅ Clients bloqués chargés:', data);
            
            if (data.blocked && Array.isArray(data.blocked)) {
                blockedClientsData = data.blocked;
            } else {
                blockedClientsData = [];
            }
            
            updateBlockedTable();
            updateBlockedStats();
            updateBlockedBadge();
        })
        .catch(error => {
            console.error('❌ Erreur chargement clients bloqués:', error);
            showNotification('❌ Impossible de charger les clients bloqués', 'error');
            
            const tbody = document.getElementById('blockedTableBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center text-muted py-5">
                            <i class="bi bi-exclamation-triangle-fill text-warning" style="font-size: 3rem;"></i>
                            <p class="mt-3">Module 2 (Enforcer) non disponible</p>
                            <p class="small">Vérifiez que le module est démarré sur le port 8082</p>
                            <button class="btn btn-primary mt-3" onclick="loadBlockedClients()">
                                <i class="bi bi-arrow-clockwise"></i> Réessayer
                            </button>
                        </td>
                    </tr>
                `;
            }
        });
}

/**
 * Mettre à jour le tableau des clients bloqués
 */
function updateBlockedTable() {
    const tbody = document.getElementById('blockedTableBody');
    if (!tbody) return;
    
    if (!blockedClientsData || blockedClientsData.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-muted py-5">
                    <i class="bi bi-shield-check" style="font-size: 3rem;"></i>
                    <p class="mt-3">Aucun client bloqué</p>
                    <p class="small">Tous les clients respectent leurs quotas</p>
                </td>
            </tr>
        `;
        return;
    }
    
    let html = '';
    blockedClientsData.forEach(client => {
        const reason = getReasonText(client.reason);
        const reasonClass = getReasonClass(client.reason);
        const blockedSince = client.blockedSince ? 
            formatDateTime(client.blockedSince) : '-';
        const duration = client.blockedSince ? 
            getDuration(client.blockedSince) : '-';
        
        html += `
            <tr>
                <td>
                    <strong>${client.macAddress || 'N/A'}</strong>
                    <br><small class="text-muted">${client.ipAddress || '-'}</small>
                </td>
                <td>${client.ipAddress || 'N/A'}</td>
                <td>
                    <span class="badge ${reasonClass}">${reason}</span>
                </td>
                <td>${blockedSince}</td>
                <td><span class="badge bg-secondary">${duration}</span></td>
                <td>
                    ${client.timeRemaining > 0 ? 
                        `<span class="badge bg-warning">${client.timeRemaining} min restantes</span>` : 
                        `<span class="badge bg-danger">Quota dépassé</span>`
                    }
                </td>
                <td>
                    <button class="btn btn-sm btn-success" 
                            onclick="showUnblockModal('${client.macAddress}', '${client.ipAddress}')"
                            title="Débloquer ce client">
                        <i class="bi bi-shield"></i> Débloquer
                    </button>
                </td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

/**
 * Mettre à jour les statistiques
 */
function updateBlockedStats() {
    const total = blockedClientsData.length;
    
    // Compter par raison
    const quotaExceeded = blockedClientsData.filter(c => 
        c.reason && c.reason.includes('QUOTA')).length;
    const manualBlock = blockedClientsData.filter(c => 
        c.reason && c.reason.includes('MANUAL')).length;
    
    document.getElementById('totalBlocked').textContent = total;
    document.getElementById('quotaExceeded').textContent = quotaExceeded;
    document.getElementById('manualBlock').textContent = manualBlock;
}

/**
 * Mettre à jour le badge dans la sidebar
 */
function updateBlockedBadge() {
    const badge = document.getElementById('blockedCount');
    if (badge) {
        const count = blockedClientsData.length;
        badge.textContent = count;
        badge.style.background = count > 0 ? '#e63946' : '#6c757d';
    }
}

/**
 * Afficher la modal de confirmation de déblocage
 */
function showUnblockModal(macAddress, ipAddress) {
    document.getElementById('unblockMacAddress').textContent = macAddress;
    document.getElementById('unblockIpAddress').textContent = ipAddress || '';
    
    // Stocker la MAC pour l'utiliser dans confirmUnblock
    document.getElementById('confirmUnblockBtn').setAttribute('data-mac', macAddress);
    
    new bootstrap.Modal(document.getElementById('unblockModal')).show();
}

/**
 * Confirmer le déblocage d'un client
 */
function confirmUnblock() {
    const mac = document.getElementById('confirmUnblockBtn').getAttribute('data-mac');
    
    if (!mac) return;
    
    fetch('/api/enforcer/unblock/' + mac, {
        method: 'POST'
    })
    .then(response => {
        if (!response.ok) throw new Error('Échec déblocage');
        return response.json();
    })
    .then(data => {
        showNotification(`✅ Client ${mac} débloqué avec succès`, 'success');
        
        // Fermer la modal
        bootstrap.Modal.getInstance(document.getElementById('unblockModal')).hide();
        
        // Recharger la liste
        loadBlockedClients();
    })
    .catch(error => {
        console.error('❌ Erreur déblocage:', error);
        showNotification('❌ Erreur lors du déblocage', 'error');
    });
}

/**
 * Afficher la modal de nettoyage
 */
function cleanExpired() {
    new bootstrap.Modal(document.getElementById('cleanModal')).show();
}

/**
 * Confirmer le nettoyage des blocages expirés
 */
function confirmClean() {
    fetch('/api/enforcer/blocked/clean', {
        method: 'POST'
    })
    .then(response => {
        if (!response.ok) throw new Error('Échec nettoyage');
        return response.json();
    })
    .then(data => {
        showNotification('🧹 Blocages expirés nettoyés', 'success');
        bootstrap.Modal.getInstance(document.getElementById('cleanModal')).hide();
        loadBlockedClients();
    })
    .catch(error => {
        console.error('❌ Erreur nettoyage:', error);
        showNotification('❌ Erreur lors du nettoyage', 'error');
    });
}

/**
 * Rafraîchir la liste
 */
function refreshBlocked() {
    showNotification('🔄 Rafraîchissement...', 'info');
    loadBlockedClients();
}

/**
 * Filtrer le tableau
 */
function filterBlocked() {
    const searchTerm = document.getElementById('searchBlocked').value.toLowerCase();
    const rows = document.querySelectorAll('#blockedTable tbody tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(searchTerm) ? '' : 'none';
    });
}

/**
 * Obtenir le texte de la raison
 */
function getReasonText(reason) {
    if (!reason) return 'Inconnu';
    
    if (reason.includes('QUOTA_TIME')) return 'Quota temps dépassé';
    if (reason.includes('QUOTA_DATA')) return 'Quota données dépassé';
    if (reason.includes('MANUAL')) return 'Blocage manuel';
    if (reason.includes('RESTORE')) return 'Restauration démarrage';
    
    return reason;
}

/**
 * Obtenir la classe CSS pour la raison
 */
function getReasonClass(reason) {
    if (!reason) return 'bg-secondary';
    
    if (reason.includes('QUOTA')) return 'bg-danger';
    if (reason.includes('MANUAL')) return 'bg-warning';
    if (reason.includes('RESTORE')) return 'bg-info';
    
    return 'bg-secondary';
}

/**
 * Formater la date
 */
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    try {
        const date = new Date(dateTimeStr);
        return date.toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (e) {
        return dateTimeStr;
    }
}

/**
 * Calculer la durée depuis le blocage
 */
function getDuration(dateTimeStr) {
    if (!dateTimeStr) return '-';
    try {
        const blocked = new Date(dateTimeStr);
        const now = new Date();
        const diffMs = now - blocked;
        const diffHrs = Math.floor(diffMs / (1000 * 60 * 60));
        const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
        
        if (diffHrs > 0) {
            return `${diffHrs}h${diffMins}`;
        } else {
            return `${diffMins}min`;
        }
    } catch (e) {
        return '-';
    }
}

/**
 * Afficher une notification
 */
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