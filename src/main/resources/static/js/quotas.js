/**
 * WiFi Manager - Page Quotas
 * Version CORRIGÉE - Utilisation du proxy DashboardServer
 */

let quotasData = [];
let clientsData = [];

document.addEventListener('DOMContentLoaded', function() {
    console.log('⚙️ Page Quotas chargée');
    
    const content = document.getElementById('pageContent');
    if (content) content.style.display = 'block';
    
    loadClients();
    loadAllQuotas();
    
    setupEventListeners();
    
    setInterval(loadAllQuotas, 10000);
});

function setupEventListeners() {
    const submitBtn = document.getElementById('submitQuotaBtn');
    if (submitBtn) {
        submitBtn.addEventListener('click', createQuota);
    }
    
    const searchInput = document.getElementById('searchQuotas');
    if (searchInput) {
        searchInput.addEventListener('keyup', filterQuotas);
    }
    
    const resetBtn = document.getElementById('resetRulesBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', resetRules);
    }
}

// ========== MODULE 1 - DETECTOR API (via proxy) ==========

function loadClients() {
    // ✅ UTILISE LE PROXY
    fetch('/api/detector/api/clients')
        .then(response => response.text())
        .then(text => {
            try {
                clientsData = JSON.parse(text);
                updateClientSelect();
            } catch (e) {
                console.error('Erreur parsing clients:', e);
                clientsData = [];
            }
        })
        .catch(error => {
            console.error('Erreur chargement clients:', error);
            clientsData = [];
        });
}

// ========== MODULE 2 - ENFORCER API (via proxy) ==========

function loadAllQuotas() {
    // ✅ UTILISE LE PROXY - GET /quota/
    fetch('/api/enforcer/quota/')
        .then(response => {
            if (!response.ok) throw new Error('Module 2 hors ligne');
            return response.text();
        })
        .then(text => {
            try {
                const data = JSON.parse(text);
                quotasData = data.quotas || [];
                console.log('✅ Quotas chargés:', quotasData.length);
                updateQuotasTable();
                updateQuotasBadge();
            } catch (e) {
                console.error('Erreur parsing quotas:', e);
                quotasData = [];
                updateQuotasTable();
            }
        })
        .catch(error => {
            console.error('Erreur chargement quotas:', error);
            showNotification('❌ Module 2 (Enforcer) hors ligne', 'error');
            
            const tbody = document.getElementById('quotasTableBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center text-muted py-5">
                            <i class="bi bi-exclamation-triangle-fill text-warning" style="font-size: 3rem;"></i>
                            <p class="mt-3">Module 2 (Enforcer) non disponible</p>
                            <p class="small">Vérifiez que le module est démarré sur le port 8082</p>
                            <button class="btn btn-primary mt-3" onclick="loadAllQuotas()">
                                <i class="bi bi-arrow-clockwise"></i> Réessayer
                            </button>
                        </td>
                    </tr>
                `;
            }
            quotasData = [];
        });
}

function createQuota() {
    const mac = document.getElementById('macSelect').value;
    const time = document.getElementById('timeMinutes').value;
    const data = document.getElementById('dataMB').value;
    
    if (!mac) {
        showNotification('❌ Veuillez sélectionner un client', 'warning');
        return;
    }
    
    // Cas spécial: "ALL" = tous les clients
    if (mac === 'ALL') {
        createQuotasForAllClients(time, data);
        return;
    }
    
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
            showNotification(`✅ Quota créé pour ${data.macAddress}`, 'success');
        } catch (e) {
            showNotification('✅ Quota créé avec succès', 'success');
        }
        
        // Réinitialiser le formulaire
        document.getElementById('macSelect').value = '';
        document.getElementById('timeMinutes').value = '60';
        document.getElementById('dataMB').value = '500';
        
        // Recharger les quotas
        loadAllQuotas();
    })
    .catch(error => {
        console.error('❌ Erreur:', error);
        showNotification('❌ ' + error.message, 'error');
    });
}

function createQuotasForAllClients(time, data) {
    if (!clientsData || clientsData.length === 0) {
        showNotification('❌ Aucun client disponible', 'error');
        return;
    }
    
    let successCount = 0;
    let errorCount = 0;
    let total = clientsData.length;
    
    showNotification(`🔄 Création de quotas pour ${total} clients...`, 'info');
    
    clientsData.forEach((client, index) => {
        if (!client.macAddress) return;
        
        const quotaData = {
            mac: client.macAddress,
            timeMinutes: parseInt(time),
            dataMB: parseInt(data)
        };
        
        // ✅ UTILISE LE PROXY
        fetch('/api/enforcer/quota/set', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(quotaData)
        })
        .then(response => {
            if (response.ok) successCount++;
            else errorCount++;
        })
        .catch(() => errorCount++)
        .finally(() => {
            if (index === total - 1) {
                showNotification(`✅ Quotas créés: ${successCount} succès, ${errorCount} échecs`, 
                    errorCount === 0 ? 'success' : 'warning');
                loadAllQuotas();
            }
        });
    });
}

function deleteQuota(mac) {
    if (confirm(`Voulez-vous supprimer le quota pour ${mac} ?`)) {
        showNotification('ℹ️ Fonctionnalité de suppression à implémenter', 'info');
    }
}

function resetRules() {
    if (confirm('Réinitialiser toutes les règles réseau ?')) {
        // ✅ UTILISE LE PROXY - POST /rules/reset
        fetch('/api/enforcer/rules/reset', {
            method: 'POST'
        })
        .then(response => response.text())
        .then(text => {
            try {
                const data = JSON.parse(text);
                showNotification(data.message || '✅ Règles réinitialisées', 'success');
            } catch (e) {
                showNotification('✅ Règles réinitialisées', 'success');
            }
            loadAllQuotas();
        })
        .catch(error => {
            console.error('❌ Erreur reset:', error);
            showNotification('❌ Erreur réinitialisation', 'error');
        });
    }
}

// ========== AFFICHAGE ==========

function updateClientSelect() {
    const select = document.getElementById('macSelect');
    if (!select) return;
    
    let options = '<option value="">📋 Sélectionner un client...</option>';
    options += '<option value="ALL">🚀 Tous les clients</option>';
    
    clientsData.forEach(client => {
        if (client.macAddress) {
            const status = client.active ? '🟢' : '⚪';
            options += `<option value="${client.macAddress}">
                ${status} ${client.macAddress} - ${client.ipAddress || 'N/A'}
            </option>`;
        }
    });
    
    select.innerHTML = options;
}

function updateQuotasTable() {
    const tbody = document.getElementById('quotasTableBody');
    if (!tbody) return;
    
    if (!quotasData || quotasData.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted py-5">
                    <i class="bi bi-sliders2" style="font-size: 3rem;"></i>
                    <p class="mt-3">Aucun quota actif</p>
                    <p class="small">Créez un quota pour limiter l'accès</p>
                </td>
            </tr>
        `;
        return;
    }
    
    let html = '';
    quotasData.forEach(quota => {
        const client = findClientByMac(quota.macAddress);
        const timePercentage = calculatePercentage(quota.timeUsedMinutes, quota.timeLimitMinutes);
        const dataPercentage = calculatePercentage(quota.dataUsedMB, quota.dataLimitMB);
        const progressClass = getProgressClass(dataPercentage);
        
        let statusClass = 'bg-success';
        let statusText = 'Actif';
        if (quota.isExceeded) {
            statusClass = 'bg-danger';
            statusText = 'Dépassé';
        } else if (!quota.isActive) {
            statusClass = 'bg-secondary';
            statusText = 'Inactif';
        }
        
        html += `
            <tr>
                <td>
                    <strong>${quota.macAddress || 'N/A'}</strong>
                    <br>
                    <small class="text-muted">${client?.ipAddress || '-'}</small>
                </td>
                <td>
                    <span>${quota.timeUsedMinutes || 0}/${quota.timeLimitMinutes || 0} min</span>
                    <br>
                    <small class="text-muted">${quota.timeRemainingMinutes || 0} min restantes</small>
                </td>
                <td>
                    <span>${quota.dataUsedMB || 0}/${quota.dataLimitMB || 0} Mo</span>
                    <br>
                    <small class="text-muted">${quota.dataRemainingMB || 0} Mo restants</small>
                </td>
                <td style="min-width: 150px;">
                    <div class="progress" style="height: 8px;">
                        <div class="progress-bar ${progressClass}" 
                             style="width: ${dataPercentage}%">
                        </div>
                    </div>
                    <small class="text-muted">${dataPercentage}% utilisé</small>
                    <br>
                    <small class="text-muted">Temps: ${timePercentage}%</small>
                </td>
                <td>
                    <span class="badge ${statusClass}">${statusText}</span>
                </td>
                <td>
                    <button class="btn btn-sm btn-outline-danger" 
                            onclick="deleteQuota('${quota.macAddress}')"
                            title="Supprimer le quota">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

function findClientByMac(mac) {
    if (!clientsData || !mac) return null;
    return clientsData.find(c => c.macAddress === mac);
}

function calculatePercentage(used, limit) {
    if (!limit || limit <= 0) return 0;
    return Math.min(100, Math.round((used || 0) * 100 / limit));
}

function getProgressClass(percentage) {
    if (percentage >= 90) return 'danger';
    if (percentage >= 70) return 'warning';
    return '';
}

function updateQuotasBadge() {
    const badge = document.getElementById('quotasCount');
    if (badge && quotasData) {
        const active = quotasData.filter(q => q.isActive && !q.isExceeded).length;
        badge.textContent = active;
        badge.style.background = active > 0 ? '#e63946' : '#6c757d';
    }
}

function filterQuotas() {
    const searchTerm = document.getElementById('searchQuotas').value.toLowerCase();
    const rows = document.querySelectorAll('#quotasTable tbody tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(searchTerm) ? '' : 'none';
    });
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