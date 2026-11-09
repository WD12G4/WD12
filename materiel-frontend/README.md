# materiel-frontend

Front de l'application de gestion du parc informatique et des salles (WD12 G4).
Vue 3 + TypeScript + Vite, généré selon le document `CADRE_FRONT_IA.md` (conventions du projet de cours `vue1`).

## Lancer en développement

Prérequis : Node 22, et le backend `materiel-backend` démarré sur le port 8080 (avec sa base PostgreSQL).

```bash
npm install
```

```bash
npm run dev
```

L'application est servie sur http://localhost:5173. Le proxy Vite relaie `/api` vers `http://localhost:8080`
**sans réécrire le chemin** : le front et l'API restent sur la même origine, condition pour que le cookie de
rafraîchissement de session (`SameSite=Strict`) soit envoyé.

Comptes de démonstration (créés par `docker/initdb/02-data.sql`) :

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin123` | Administrateur |
| `manager` | `manager123` | Gestionnaire |
| `user` | `user123` | Formateur |

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
| 1 | Connexion, menu selon le rôle, session restaurée au rechargement | oui | tous |
| 2 | Recherche et filtres du matériel, fiche, création, modification, affectation, archivage | oui | tous / ADMIN / MANAGER |
| 3 | Bâtiments, salles (avec le matériel de la salle), comptes | oui | tous / ADMIN |
| 4 | Tableau de bord : compteurs (liens vers la liste déjà filtrée), alerte groupée des fins de disponibilité, toutes les salles en tuiles par bâtiment (un clic ouvre la salle), tuile « En stock » | oui | tous |
| 5 | Remontées : signalement, liste, document d'état des lieux imprimable, pop-up du gestionnaire | **maquette** | tous / MANAGER |
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
