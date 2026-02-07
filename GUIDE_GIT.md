# ETAPE CRITIQUE QUE CHAQUE COLLABORATEUR DOIVENT FAIRE AVANT DE COMMENCET A TRAVAILLER

# 1. Se mettre à jour
git pull origin main

# 2. Travailler
# ... modifier ArpScanner.java ...

# 3. Ajouter UNIQUEMENT ses fichiers (otanzao no tokony atao rehefa i commit modification)
git add src/main/java/com/wifimanager/detector/ArpScanner.java

# 4. Commit
git commit -m "feat(detector): optimisation scan ARP"

# 5. Pull (vérifier conflits)
git pull origin main

# 6. Push
git push origin main


# Configure Git (une seule fois) : (TSY MAITSY ATAO RAHA MBOLA TSY NANAO)

git config --global user.name "Ton Nom"
git config --global user.email "ton.email@example.com"