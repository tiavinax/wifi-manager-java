/**
 * WiFi Manager - Dashboard
 * Fonctions communes - Version CORRIGÉE
 */

document.addEventListener('DOMContentLoaded', function() {
    updateDateTime();
    setInterval(updateDateTime, 1000);
    checkModulesStatus();
    setInterval(checkModulesStatus, 30000);
});

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
    const element = document.getElementById('currentDateTime');
    if (element) {
        element.textContent = now.toLocaleDateString('fr-FR', options);
    }
}

function checkModulesStatus() {
    // Module 1 - ✅ VIA PROXY
    fetch('/api/detector/api/health')
        .then(response => response.ok ? 'En ligne' : 'Hors ligne')
        .then(status => {
            const el = document.getElementById('module1Status');
            if (el) {
                el.className = 'status-badge ' + (status === 'En ligne' ? 'status-online' : 'status-offline');
                el.innerHTML = `<i class="bi bi-${status === 'En ligne' ? 'check-circle' : 'exclamation-circle'}-fill"></i> ${status}`;
            }
        })
        .catch(() => {
            const el = document.getElementById('module1Status');
            if (el) {
                el.className = 'status-badge status-offline';
                el.innerHTML = '<i class="bi bi-exclamation-circle-fill"></i> Hors ligne';
            }
        });
    
    // Module 2 - ✅ VIA PROXY
    fetch('/api/enforcer/health')
        .then(response => response.ok ? 'En ligne' : 'Hors ligne')
        .then(status => {
            const el = document.getElementById('module2Status');
            if (el) {
                el.className = 'status-badge ' + (status === 'En ligne' ? 'status-online' : 'status-offline');
                el.innerHTML = `<i class="bi bi-${status === 'En ligne' ? 'check-circle' : 'exclamation-circle'}-fill"></i> ${status}`;
            }
        })
        .catch(() => {
            const el = document.getElementById('module2Status');
            if (el) {
                el.className = 'status-badge status-offline';
                el.innerHTML = '<i class="bi bi-exclamation-circle-fill"></i> Hors ligne';
            }
        });
}

function confirmLogout() {
    return confirm('Êtes-vous sûr de vouloir vous déconnecter ?');
}