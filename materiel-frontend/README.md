# materiel-frontend

Front de BASTION, l'application de gestion du parc informatique, des salles et de supervision des postes (WD12 G4).
Vue 3 + TypeScript + Vite, écrit selon `CADRE_FRONT_IA.md` (conventions du projet de cours `vue1` ; document de
travail de l'équipe, conservé hors du dépôt). Présentation d'ensemble et lancement par Docker : `../Readme.md`.

## Lancer en développement

Prérequis : Node 24 (la version de la CI et de l'image Docker), le backend `materiel-backend` démarré sur le port 8080 (avec sa base PostgreSQL),
et Keycloak démarré (`docker compose up -d keycloak` à la racine du dépôt) avec, dans `.env`,
`APP_URL=http://localhost:5173` et `KEYCLOAK_URL=http://localhost:5173/auth`.

```bash
npm install
```

```bash
npm run dev
```

L'application est servie sur http://localhost:5173. Le proxy Vite relaie `/api` vers `http://localhost:8080`
**sans réécrire le chemin** : le front et l'API restent sur la même origine.

### Connexion : Keycloak (joue MinDef Connect)

Le front ne demande jamais le mot de passe : « S'identifier avec MinDefConnect » ouvre la page de Keycloak
(realm `gestion-parc`, client public `materiel-frontend`, code d'autorisation + PKCE), qui ramène ensuite sur
la page demandée. Le jeton reste en mémoire (`keycloak-js`) et part dans l'en-tête `Authorization`.
Le backend vérifie sa signature, retrouve le compte par son identifiant et lui applique son rôle :
Keycloak prouve l'identité, les rôles restent dans la base (page « Comptes »).

- Adresse de Keycloak : celle de l'application suivie de `/auth`. Le Nginx du front (ou le proxy de Vite)
  relaie `/auth/` vers Keycloak : même origine et même port 80 que l'application, donc rien à ouvrir de
  plus et le proxy de l'école laisse passer.
- HTTPS obligatoire hors `localhost` : keycloak-js utilise l'API Web Crypto, que les navigateurs ne fournissent
  qu'en HTTPS. Nginx sert l'application en HTTPS (port 443, le port 80 redirige) avec un certificat auto-signé
  créé au premier démarrage pour l'adresse de `APP_URL` (volume `certs_data`). Le navigateur demande une
  fois de l'accepter (« Avancé » puis « Accepter le risque et poursuivre »). Pour un certificat signé par une
  autorité, déposer `server.crt` et `server.key` dans le volume.
- Sur une autre machine (VM) : dans `.env`, `APP_URL=https://<adresse>` et `KEYCLOAK_URL=https://<adresse>/auth`,
  puis `docker compose rm -sf keycloak && docker compose up -d --build keycloak backend frontend`.
- Console d'administration : `<adresse de l'application>/auth/admin` (compte `admin` / `admin`, voir `.env`).
- Configuration versionnée : `docker/keycloak/gestion-parc-realm.json`, rechargée à chaque démarrage.

Comptes de démonstration (même identifiant dans Keycloak et dans `docker/initdb/02-data.sql`) :

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin123` | Administrateur |
| `manager` | `manager123` | Gestionnaire |
| `user` | `user123` | Formateur |

Un compte créé depuis la page « Comptes » doit aussi être créé dans Keycloak avec le même identifiant :
sans lui, Keycloak refuse la connexion ; sans compte dans la base, c'est l'application qui la refuse.

## Vérifier avant de pousser

```bash
npm run build
```

`vue-tsc` doit passer sans erreur ni avertissement. Il n'y a pas encore de test automatisé du front dans le
dépôt : la CI vérifie les types, construit l'application et lance l'analyse SonarQube.

## Organisation

```
src/
├── main.ts, App.vue, style.css
├── router/        routes.ts (déclaration typée), index.ts, guards.ts (accès par rôle), route-types.ts, navigation.ts
├── models/        <entite>.model.ts (types, listes as const, libellés) et <entite>.dto.ts (forme JSON + conversions)
├── services/      http-client.ts (seul appel à fetch), un service par ressource, *.services.mock.ts (maquettes)
├── shared/        composants et composables utilisés par plusieurs features
└── features/      auth, home, materials, rooms, buildings, users, reports, supervision
```

Écart avec l'arborescence du cadre : les routes sont dans `router/routes.ts` et non dans `router/index.ts`,
pour respecter la limite de 120 lignes par fichier.

## Écrans

Tous les écrans appellent l'API du backend.

| Écran | Contenu | Rôle |
|---|---|---|
| Connexion | Keycloak (joue MinDef Connect), session restaurée au rechargement, menu selon le rôle | tous |
| Choix de la salle | Le formateur choisit sa salle de cours avant toute page ; il en change par le bouton de la barre du haut | formateur |
| Tableau de bord | Compteurs (liens vers la liste déjà filtrée), fins de disponibilité à 30, 60 ou 90 jours, salles en tuiles par bâtiment, tuile « En stock ». Formateur : sa salle seulement, avec un aperçu de ses postes et de la supervision | tous |
| Matériel | Recherche et filtres, fiche, création, modification, affectation, archivage | tous / gestionnaire et administrateur pour modifier |
| Salles, Bâtiments | Listes, fiche d'une salle avec son matériel ; création, modification, suppression | tous / administrateur pour modifier |
| Comptes | Création, rôle, archivage et réactivation (pas de mot de passe : il est dans Keycloak) | administrateur |
| Remontées | Signalement, liste, traitement, document d'état des lieux imprimable (PDF par l'impression du navigateur), compteur des remontées à traiter dans le menu | tous / gestionnaire et administrateur pour traiter |
| Supervision | Postes d'une salle, applications ouvertes, arrêt après confirmation, « Fermer partout », journal des arrêts, liste noire | tous / administrateur pour la liste noire |

### Ce qui reste simulé

Les postes marqués « Simulé » et la masterisation (`services/*.mock.ts`) : il n'existe pas d'API de masterisation.
Leurs données sont en mémoire et disparaissent au rechargement de la page. Les vrais postes viennent de l'agent
de supervision.

### Formateur : limité à sa salle de cours

La salle choisie est dans l'adresse de chaque page (`?salle=12`). Le formateur ne voit que cette salle : tableau
de bord, matériel, salles, bâtiments, formulaire de remontée (salle imposée) et supervision. La fiche d'une salle
ou d'un matériel hors de sa salle lui est refusée, même s'il en tape l'adresse. Ce filtre est fait
par le front seulement : l'API ne limite pas encore le formateur à une salle ni à ses propres remontées.

### Apparence et mode démonstration

- Six thèmes (Givre, Nuit, Aurore, Lagon, Sauge, DSFR), deux dispositions du menu, contenu en bloc central ou en
  pleine largeur (par défaut). Le choix reste en mémoire : pas de stockage dans le navigateur, un rechargement
  revient au thème du système.
- Mode démonstration : un administrateur change de rôle sans se déconnecter, et la page de connexion propose les
  trois comptes. Actif tant que l'image n'est pas construite avec `VITE_DEMO_ACCOUNTS=false` (et le backend lancé
  avec `DEMO_ACCOUNTS=false`).

## Règles de gestion

Les commentaires du code citent les règles de gestion (`RG-xx`) qu'ils appliquent. Leur liste est dans le fichier
de suivi des règles de gestion de l'équipe, conservé hors du dépôt.

## Points ouverts

Les questions encore ouvertes sont marquées `TODO(question)` dans le code :

- RG-03 : le type « switch » n'existe pas côté API.
- RG-08 : statuts « HS » et « À rendre » sans champ côté API.
- RG-21 : la liste des types de problème est à valider avec le client ; faute de champ côté API, le type est écrit
  en tête de la description.
- Format des dates et heures renvoyées par l'API (sans fuseau), à confirmer avec le backend.

Écart connu avec les règles validées :

- RG-34, RG-38, RG-46 : la limite du formateur (sa salle, ses remontées) n'est tenue que par le front.

Les `TODO(perf)` signalent que les listes sont chargées en une seule page (1000 éléments) et filtrées côté front,
faute de filtre côté API.
