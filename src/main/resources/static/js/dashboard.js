/**
 * WiFi Manager Dashboard
 * JavaScript pour interface premium
 */

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Dashboard initialisé');
    
    // Initialisation
    initSidebar();
    initDateTime();
    initSearch();
    initNotifications();
    
    // Rafraîchissement automatique
    startAutoRefresh();
});

/**
 * Sidebar Toggle
 */
function initSidebar() {
    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('sidebarToggle');
    
    // Vérifier si l'état est sauvegardé
    const sidebarState = localStorage.getItem('sidebarCollapsed');
    if (sidebarState === 'true') {
        sidebar.classList.add('collapsed');
    }
    
    if (toggleBtn) {
        toggleBtn.addEventListener('click', function() {
            sidebar.classList.toggle('collapsed');
            
            // Sauvegarder l'état
            localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
            
            // Changer l'icône
            const icon = this.querySelector('i');
            if (sidebar.classList.contains('collapsed')) {
                icon.className = 'bi bi-chevron-right';
            } else {
                icon.className = 'bi bi-chevron-left';
            }
        });
    }
}

/**
 * Date et temps réel
 */
function initDateTime() {
    updateDateTime();
    setInterval(updateDateTime, 1000);
}

function updateDateTime() {
    const now = new Date();
    const options = { 
        weekday: 'long', 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };
    
    const dateTimeStr = now.toLocaleDateString('fr-FR', options);
    const element = document.getElementById('currentDateTime');
    if (element) {
        element.textContent = dateTimeStr;
    }
}

/**
 * Recherche en temps réel
 */
function initSearch() {
    const searchInputs = document.querySelectorAll('.table-search input');
    searchInputs.forEach(input => {
        input.addEventListener('keyup', function() {
            const searchTerm = this.value.toLowerCase();
            const tableId = this.getAttribute('data-table');
            filterTable(tableId, searchTerm);
        });
    });
}

function filterTable(tableId, searchTerm) {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    const rows = table.querySelectorAll('tbody tr');
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        if (text.includes(searchTerm)) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

/**
 * Notifications
 */
function initNotifications() {
    // Notifications automatiques
    checkModulesStatus();
    setInterval(checkModulesStatus, 30000);
}

function checkModulesStatus() {
    fetch('/api/health')
        .then(response => response.json())
        .then(data => {
            updateModuleStatus('module1', data.module1);
            updateModuleStatus('module2', data.module2);
        })
        .catch(error => {
            console.error('Erreur vérification modules:', error);
        });
}

function updateModuleStatus(moduleId, isOnline) {
    const element = document.getElementById(moduleId + 'Status');
    if (element) {
        if (isOnline) {
            element.innerHTML = '<i class="bi bi-check-circle-fill"></i> En ligne';
            element.className = 'status-badge status-online';
        } else {
            element.innerHTML = '<i class="bi bi-exclamation-circle-fill"></i> Hors ligne';
            element.className = 'status-badge status-offline';
        }
    }
}

/**
 * Afficher une notification
 */
function showNotification(message, type = 'success') {
    const notificationArea = document.getElementById('notificationArea');
    if (!notificationArea) return;
    
    notificationArea.style.display = 'block';
    notificationArea.className = 'notification-area ' + type;
    notificationArea.innerHTML = `
        <div class="d-flex align-items-center">
            <i class="bi bi-${getIconForType(type)} me-2"></i>
            <span>${message}</span>
            <button type="button" class="btn-close ms-auto" onclick="this.parentElement.parentElement.style.display='none'"></button>
        </div>
    `;
    
    // Auto-cacher après 5 secondes
    setTimeout(() => {
        notificationArea.style.display = 'none';
    }, 5000);
}

function getIconForType(type) {
    switch(type) {
        case 'success': return 'check-circle-fill';
        case 'error': return 'exclamation-circle-fill';
        case 'warning': return 'exclamation-triangle-fill';
        case 'info': return 'info-circle-fill';
        default: return 'bell-fill';
    }
}

/**
 * Confirmation avant action
 */
function confirmAction(message, callback) {
    const modal = new bootstrap.Modal(document.getElementById('confirmModal'));
    document.getElementById('confirmModalMessage').textContent = message;
    document.getElementById('confirmModalBtn').onclick = function() {
        callback();
        modal.hide();
    };
    modal.show();
}

function confirmLogout() {
    confirmAction('Êtes-vous sûr de vouloir vous déconnecter ?', function() {
        window.location.href = '/logout';
    });
    return false;
}

/**
 * Rafraîchir les données
 */
function refreshData() {
    showNotification('Mise à jour des données en cours...', 'info');
    
    // Rafraîchir les clients
    if (typeof refreshClients === 'function') {
        refreshClients();
    }
    
    // Rafraîchir les quotas
    if (typeof refreshQuotas === 'function') {
        refreshQuotas();
    }
    
    // Rafraîchir les stats
    if (typeof refreshStats === 'function') {
        refreshStats();
    }
}

/**
 * Auto-refresh toutes les 30 secondes
 */
function startAutoRefresh() {
    setInterval(function() {
        // Ne pas afficher de notification pour le refresh auto
        if (typeof refreshClients === 'function') {
            refreshClients();
        }
        if (typeof refreshQuotas === 'function') {
            refreshQuotas();
        }
        if (typeof refreshStats === 'function') {
            refreshStats();
        }
    }, 30000);
}

/**
 * Formatage des données
 */
function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDuration(minutes) {
    if (minutes < 60) return minutes + ' min';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return hours + 'h' + (mins > 0 ? mins + 'm' : '');
}

function formatMac(mac) {
    if (!mac) return '';
    return mac.toUpperCase().replace(/-/g, ':');
}

function validateMac(mac) {
    const regex = /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/;
    return regex.test(mac);
}
