# ETAPE CRITIQUE QUE CHAQUE COLLABORATEUR DOIVENT FAIRE AVANT DE COMMENCET A TRAVAILLER

# À LIRE ABSOLUMENT AVANT DE COMMENCER :
https://github.com/tiavinax/wifi-manager-java/blob/main/GUIDE_GIT.md

# ⚠️ RÈGLES GIT STRICTES - LISEZ AVANT DE COMMENCER

## 🚫 **NE JAMAIS FAIRE :**
```bash
# ❌ INTERDIT ABSOLUMENT :
git add .              # Ajoute TOUS les fichiers
git add *              # Ajoute TOUS les fichiers
git commit -a          # Commit TOUS les changements


# Toujours ajouter fichier par fichier :

git add chemin_vers_fichier

# Chacun a son dossier sécurisé :

    [tiavina] : detector/

    [Sandria] : enforcer/

    [Larissa] : inspector/

    [Miagola & Stephanie] : dashboard/

On reste sur la branche main pour simplifier.
Toujours git pull avant de git push.

# 1. Se mettre à jour
git pull origin main

# 2. Travailler
# ... modifier ArpScanner.java ...

# 3. Ajouter UNIQUEMENT ses fichiers (otanzao no tokony atao rehefa i commit modification)
git add chemin_vers_fichier

# 4. Commit
git commit -m "feat(detector): optimisation scan ARP"

# 5. Pull (vérifier conflits)
git pull origin main

# 6. Push
git push origin main


# Configure Git (une seule fois) : (TSY MAITSY ATAO RAHA MBOLA TSY NANAO)

git config --global user.name "Ton Nom"
git config --global user.email "ton.email@example.com"