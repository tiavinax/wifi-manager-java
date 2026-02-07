#!/bin/bash
# Script d'installation pour WiFi Manager

echo "🚀 Installation de WiFi Manager pour Businesses"
echo "=============================================="

# Vérifier Java
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé"
    echo "Installation de Java 11..."
    sudo apt-get update
    sudo apt-get install -y openjdk-11-jdk
fi

# Vérifier Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven n'est pas installé"
    echo "Installation de Maven..."
    sudo apt-get install -y maven
fi

# Outils réseau nécessaires
echo "📡 Installation des outils réseau..."
sudo apt-get install -y \
    iptables \
    iproute2 \
    arp-scan \
    tcpdump \
    net-tools \
    nmap

# Vérifier les droits
echo "🔐 Configuration des droits..."
sudo setcap cap_net_raw,cap_net_admin+eip $(readlink -f $(which java)) 2>/dev/null || true

# Créer la structure de répertoires
echo "📁 Création de la structure..."
mkdir -p logs lib

# Télécharger les librairies externes
echo "📦 Téléchargement des dépendances externes..."
if [ ! -f "lib/jpcap.jar" ]; then
    echo "Téléchargement de jpcap..."
    wget -O lib/jpcap.jar https://github.com/mgodave/Jpcap/raw/master/lib/jpcap.jar
fi

# Compilation du projet
echo "🔨 Compilation du projet..."
mvn clean compile

echo "✅ Installation terminée !"
echo ""
echo "Commandes disponibles :"
echo "  mvn compile        # Compiler le projet"
echo "  mvn test           # Lancer les tests"
echo "  ./scripts/start-all.sh  # Démarrer tous les modules"
echo "  ./scripts/demo.sh       # Lancer la démo"