# 🌿 Guide des Branches Git

Commandes Essentielles

CREER & NAVIGUER : 

# Voir toutes les branches
git branch

# Créer une nouvelle branche
git branch nom-de-la-branche

# Changer de branche
git checkout nom-de-la-branche

# Créer ET changer de branche
git checkout -b nouvelle-branche

TRAVAILER SUR UNE BRANCHE :

# 1. Se mettre sur la branche
git checkout ma-feature

# 2. Travailler normalement (modifier fichiers)
# 3. Ajouter les changements
git add .

# 4. Sauvegarder (commiter)
git commit -m "Description des modifications"

# 5. Envoyer sur GitHub (si publiée)
git push origin ma-feature

🤝 FUSIONNER (Merge)

# 1. Retourner sur main
git checkout main

# 2. Mettre à jour main
git pull origin main

# 3. Fusionner votre branche
git merge ma-branche

# 4. Pousser les changements
git push origin main

🧹 Nettoyage 

# Supprimer une branche locale
git branch -d nom-branche

# Supprimer une branche distante
git push origin --delete nom-branche

# Voir toutes les branches (mêmes distantes)
git branch -a