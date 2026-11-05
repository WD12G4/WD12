# materiel-terrain — PWA RECON

Application terrain de l'opération **RECON** : un agent passe de salle en salle avec son téléphone,
**scanne** le Data Matrix (ou QR code, code-barres) collé sur chaque matériel et remet la situation à jour sur place.

| Effet recherché | Dans l'application |
|---|---|
| Reconnaître | Scan caméra en continu, photo du code, ou saisie manuelle → numéro de série extrait |
| Rendre compte | Fiche connue (type, salle déclarée, fin de disponibilité) ou « Non recensé » |
| Actualiser | « Relocaliser » dans la salle courante (matériel déplacé) et « Prendre en compte » (matériel inconnu) |
| Réseau dégradé | Parc gardé sur le téléphone, actions mises en file et rejouées au retour du réseau |

Vue 3 + TypeScript + Vite, mêmes conventions que `materiel-frontend`. **Le backend n'est pas modifié** :
la PWA n'utilise que les routes existantes.

| Route du backend | Usage |
|---|---|
| `POST /api/v1/auth/login/`, `refresh/`, `logout/`, `GET me/` | Session (jeton en mémoire, cookie HttpOnly) |
| `GET /api/v1/materials?size=1000` | Parc complet, gardé sur le téléphone : l'API n'a pas de recherche par n° de série |
| `GET /api/v1/rooms?size=1000` | Salles (avec leur bâtiment) pour choisir sa position |
| `PATCH /api/v1/materials/{id}/room` | Relocalisation (MANAGER, ADMIN) |
| `POST /api/v1/materials` | Prise en compte d'un matériel non recensé (MANAGER, ADMIN) |

## Lecture du Data Matrix

La lecture se fait **dans le navigateur** avec ZXing-C++ compilé en WebAssembly (paquet `barcode-detector`) :
Safari iOS ne fournit pas l'API `BarcodeDetector`. Le fichier `.wasm` est servi par l'application et mis en cache
par le service worker : aucun appel à un CDN, le scan marche hors réseau.

Étiquette de référence, telle que lue par l'iPhone :

```
SGY12.SS4SG0548
SS4SG0548  2CF05DC90096 2020-057 1300157350 10/2024 7021-146027780
```

Règle d'extraction (`src/features/scan/lib/parse-scan.ts`, testée) : le numéro de série est le mot qui **termine
la 1re ligne après le point** et **ouvre la 2e ligne** → `SS4SG0548`. Si l'étiquette a une autre forme, tous les mots
lus sont essayés contre le parc ; pour un matériel inconnu, le numéro proposé reste modifiable.
Sont aussi relevés : l'adresse MAC probable (`2CF05DC90096`) et le numéro OTAN (`7021-14-602-7780`), dont la classe
7021 (unité centrale) pré-remplit le type à la prise en compte (simple suggestion).

## Lancer pour la démonstration (Mac + iPhone)

Les images sont construites par la CI GitHub (`projet-terrain-backend`, `projet-terrain-frontend`) :
sur le Mac, on ne fait que `pull` et `up`. Seule étape locale, une fois : créer les certificats HTTPS.

```bash
sh materiel-terrain/scripts/generate-certs.sh
```

```bash
docker compose -f docker-compose.projet-terrain.yml --env-file .env.projet-terrain pull
```

```bash
docker compose -f docker-compose.projet-terrain.yml --env-file .env.projet-terrain up -d
```

- Le script (openssl seulement, pas besoin de Node) crée une autorité locale et un certificat pour
  `<nom-du-mac>.local` dans `materiel-terrain/certs/` (jamais poussé, jamais copié dans une image).
  HTTPS est **obligatoire** : sans lui, Safari refuse la caméra et le service worker.
- Le compose monte ces certificats et `https/nginx-https.conf` dans le conteneur `frontend` :
  la PWA est servie en **HTTPS sur le port 443**, et `http://<nom-du-mac>.local` y renvoie.
  Sans certificats, le conteneur démarre quand même, en HTTP seul (`https/40-recon-https.sh`).
- Vérifier au démarrage : `docker logs projet_terrain_frontend` affiche `RECON : HTTPS activé`.

### Sur l'iPhone (une seule fois)

L'iPhone partage sa connexion avec le Mac : le Mac n'a alors que des adresses IPv6, qui changent à chaque
connexion. On passe donc par son nom Bonjour `<nom-du-mac>.local`, couvert par le certificat.

1. **Sur le Mac**, Réglages Système › Réseau › Coupe-feu › Options : décocher **« Bloquer toutes les connexions
   entrantes »**. Tant que cette case est cochée, le Mac coupe toute connexion venue de l'iPhone (Docker compris).
2. Envoyer `materiel-terrain/certs/recon-ca.crt` sur l'iPhone (AirDrop), puis Réglages › *Profil téléchargé* › **Installer**.
3. Réglages › Général › Informations › **Réglages des certificats** › activer **RECON Dev CA**.
4. Ouvrir dans **Safari** `https://<nom-du-mac>.local` (ici `https://MacPaul.local`).
5. Partager › **Sur l'écran d'accueil** : RECON s'ouvre ensuite en plein écran, comme une application.

Test de liaison si la page ne s'ouvre pas : `http://MacPaul.local:8081` (Adminer) dans Safari sur l'iPhone.

### En développement

`npm install` puis `npm run dev` dans `materiel-terrain` : même PWA en rechargement à chaud sur
`https://<nom-du-mac>.local:5174` (mêmes certificats), qui relaie `/api` vers `http://localhost:8080`
(autre cible : `RECON_API_TARGET=http://localhost:8092 npm run dev`). `npm run terrain` sert le build avec
son service worker sur le même port.

Comptes de démonstration (créés par `docker/initdb/02-data.sql`) : `manager` / `manager123` (gestionnaire),
`admin` / `admin123`, `user` / `user123` (formateur : consultation seule).

### Scénario de démonstration

1. Se connecter en `manager`, choisir sa **position** (ex. Bât. 101 – Salle 10).
2. **Démarrer le scan** et viser l'étiquette `SS4SG0548` → *Non recensé* → date de fin de disponibilité → **Prendre en compte**.
3. Rescanner → *Conforme* (bip, le panneau se ferme seul : on enchaîne).
4. Changer de position (Bât. 102 – Salle 20), rescanner → *Déplacé* → **Relocaliser en Bât. 102 – Salle 20**.
5. Couper le Wi-Fi (ou arrêter le backend), relocaliser un autre matériel → *HORS LIGNE*, 1 action en attente ;
   rétablir le réseau → l'action part seule (relance au retour du réseau, au retour au premier plan, puis toutes les 20 s).
6. Ouvrir le bilan de salle : déclarés / vus / **manquants** (déclarés ici mais pas scannés).

## Vérifier avant de pousser

```bash
npm run build
```

```bash
npm test
```

`vue-tsc` doit passer sans erreur ; les tests couvrent l'extraction du numéro de série, l'identification
(conforme, déplacé, stock, archivé, inconnu) et le rafraîchissement de session unique.

## Organisation

```
https/         nginx-https.conf + 40-recon-https.sh : HTTPS du conteneur frontend (montés par le compose)
scripts/       generate-certs.sh : autorité locale + certificat <nom-du-mac>.local
nginx.conf     configuration HTTP de l'image (projet-terrain-frontend.dockerfile)
src/
├── main.ts, App.vue (synchronisation automatique), style.css (charte RECON)
├── router/        routes.ts, guards.ts (session en ligne ou hors ligne), index.ts
├── models/        <entite>.model.ts / .dto.ts (repris du front), scan.model.ts, sync.model.ts
├── services/      http-client.ts (délai 10 s, échec immédiat sans réseau), services par ressource, local-store.ts
├── shared/        use-auth (session + profil hors ligne), use-network, status-bar, flash-message…
└── features/
    ├── scan/       lib/ (parse-scan, identify, barcode-reader, feedback), composables/ (caméra, bilan, historique), vue principale
    ├── inventory/  parc en cache, file d'attente, actions terrain, synchronisation, écran Synchronisation
    ├── location/   salle courante et écran de choix
    └── auth/       connexion
```

## Limites connues

- **Session** : sans réseau au démarrage, l'agent reprend son dernier profil (rôle) pour consulter et mettre en file ;
  au retour du réseau, si le cookie de session (2 h) a expiré, il doit se reconnecter (les actions restent en file).
- **Une seule file par téléphone** : les actions en attente sont rejouées avec le compte connecté au moment de l'envoi.
- **Conflits** : une action refusée par l'API (numéro déjà créé entre-temps, matériel archivé…) n'est pas rejouée en
  boucle : elle s'affiche en rouge dans *Synchronisation* (Réessayer / Abandonner).
- **Taille du parc** : une page de 1000 matériels (pas de recherche par numéro côté API).
- **Matériel inconnu pour un formateur** : le backend n'a pas de route de signalement ; il est invité à noter le numéro.
- **HTTPS local** : certificat signé par une autorité de développement, à installer sur chaque téléphone de démonstration.
