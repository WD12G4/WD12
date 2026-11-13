# materiel-frontend

Front de l'application de gestion du parc informatique et des salles (WD12 G4).
Vue 3 + TypeScript + Vite, généré selon le document `CADRE_FRONT_IA.md` (conventions du projet de cours `vue1`).

## Lancer en développement

Prérequis : Node 22, le backend `materiel-backend` démarré sur le port 8080 (avec sa base PostgreSQL),
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

`vue-tsc` doit passer sans erreur ni avertissement.

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

| Lot | Écran | API | Rôle |
|---|---|---|---|
| 1 | Connexion par Keycloak (joue MinDef Connect), menu selon le rôle, session restaurée au rechargement | oui | tous |
| 2 | Recherche et filtres du matériel, fiche, création, modification, affectation, archivage | oui | tous / ADMIN / MANAGER |
| 3 | Bâtiments, salles (avec le matériel de la salle), comptes | oui | tous / ADMIN |
| 4 | Tableau de bord : compteurs (liens vers la liste déjà filtrée), alerte groupée des fins de disponibilité, toutes les salles en tuiles par bâtiment (un clic ouvre la salle), tuile « En stock » | oui | tous |
| 5 | Remontées : signalement, liste, document d'état des lieux imprimable, compteur du gestionnaire dans le menu | **maquette** | tous / MANAGER |
| 6 | Supervision : postes d'une salle, processus, arrêt, masterisation | **maquette** | tous / ADMIN |

Les écrans de maquette affichent un bandeau « Données de démonstration ». Leurs données sont en mémoire :
elles disparaissent au rechargement de la page.

## Points ouverts

Chaque point est marqué `TODO(question)` dans le code :

- RG-04 : la date de fin de disponibilité est encore obligatoire côté API (le client la veut facultative).
- RG-03 : le type « switch » n'existe pas côté API.
- RG-08 : statuts « HS » et « À rendre » sans champ côté API.
- RG-22 : génération et stockage du PDF de remontée (côté backend recommandé).
- RG-21 : liste des types de problème proposée, à valider avec le client.
- RG-29 / RG-30 : identification des postes (MAC ou IP) et libellé du stagiaire.
- RG-31 : effet de la suppression d'un bâtiment ou d'une salle sur le matériel.

Les `TODO(perf)` signalent que les listes sont chargées en une seule page (1000 éléments) et filtrées côté front,
faute de filtre côté API.
