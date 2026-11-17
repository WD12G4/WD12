# BASTION

Application de gestion du matériel informatique, des salles et de supervision des postes de cours
(projet d'école WD12, groupe 4).

BASTION répond à trois besoins du client :

1. **Gérer le parc** : matériel (numéro de série, type, salle ou stock, fin de disponibilité, archivage),
   salles, bâtiments, comptes.
2. **Faire remonter les problèmes** : un formateur signale un matériel en panne, le gestionnaire traite la
   remontée, un document d'état des lieux s'imprime.
3. **Superviser les postes d'une salle** : voir les applications ouvertes sur chaque poste, en arrêter une,
   interdire une application (liste noire), consulter le journal des arrêts.

## Rôles

Les rôles s'héritent : l'administrateur a tous les droits du gestionnaire, qui a ceux du formateur.

| Rôle | Dans le code | Ce qu'il fait |
|---|---|---|
| Formateur | `USER` | Choisit sa salle de cours à la connexion, voit le matériel de cette salle, signale un problème, supervise les postes de sa salle. |
| Gestionnaire | `MANAGER` | Tout le parc : crée, modifie, affecte et archive le matériel ; traite les remontées. |
| Administrateur | `ADMIN` | Gère en plus les bâtiments, les salles, les comptes et la liste noire ; lance la masterisation (simulée). |

## Architecture

```
Navigateur ──HTTPS──► Nginx (conteneur du front)
                        ├── /        application Vue (fichiers statiques)
                        ├── /api     ──► backend Spring Boot ──► PostgreSQL
                        ├── /auth    ──► Keycloak (joue MinDef Connect)
                        └── /ws      ──► backend (WebSocket des agents)

Poste de cours ── agent C# ──wss──► /ws/agent
```

| Dossier | Contenu | Technologies |
|---|---|---|
| `materiel-frontend/` | Interface web | Vue 3, TypeScript, Vite, servie par Nginx en HTTPS |
| `materiel-backend/` | API REST et canal des agents | Java 21, Spring Boot, JPA, PostgreSQL |
| `materiel-agent/` | Agent installé sur chaque poste supervisé | C# .NET 10 (Windows, Linux, macOS) |
| `docker/initdb/` | Schéma et données de démonstration de la base | SQL |
| `docker/keycloak/` | Realm `gestion-parc` : client et comptes de démonstration | Keycloak |
| `anasi/` | Analyse : diagramme de classes, diagramme des cas d'utilisation | Mermaid, PlantUML, draw.io |
| `.github/workflows/` | Intégration continue du front, du backend et de l'agent | GitHub Actions |

Connexion : le front ne voit jamais de mot de passe. Keycloak prouve l'identité (code d'autorisation et
PKCE, jeton gardé en mémoire) ; le backend vérifie le jeton, puis lit le rôle du compte dans sa propre
table `users`.

## Lancer l'application

Prérequis : Docker et Docker Compose.

```bash
cp .env.example .env
```

Dans `.env`, mettre l'adresse de la machine dans `APP_URL` et `KEYCLOAK_URL` (en local :
`https://localhost` et `https://localhost/auth`). Puis, pour construire les images depuis le code :

```bash
docker compose up -d --build
```

Ou, pour utiliser les images publiées par la CI (c'est ce qui tourne sur la VM de l'école) :

```bash
docker compose -f docker-compose.prod.yml pull
```

```bash
docker compose -f docker-compose.prod.yml up -d
```

L'application répond sur `https://<adresse>`. Le certificat est auto-signé : le navigateur demande une
fois de l'accepter. Au premier démarrage, la base est créée avec des données de démonstration.

Pour développer le front ou installer un agent, voir `materiel-frontend/README.md` et
`materiel-agent/README.md`. Un poste n'apparaît en supervision que si son unité centrale est déclarée
dans le matériel, avec le numéro de série que l'agent envoie : sinon le serveur refuse l'agent.

### Comptes de démonstration

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin123` | Administrateur |
| `manager` | `manager123` | Gestionnaire |
| `user` | `user123` | Formateur |

Ce sont des comptes de démonstration, comme les mots de passe par défaut de `.env.example` : à changer
avant tout usage réel. Un compte existe à deux endroits, avec le même identifiant : dans Keycloak (pour se
connecter) et dans la page « Comptes » de l'application (pour le rôle).

## Vérifier

| Partie | Commande | Ce qui est vérifié |
|---|---|---|
| Backend | `./gradlew test` dans `materiel-backend/` | 336 tests unitaires (services, contrôleurs, sécurité, WebSocket) |
| Front | `npm run build` dans `materiel-frontend/` | Types (`vue-tsc`) et construction de production |
| Agent | `dotnet build` dans `materiel-agent/` | Compilation |

La CI rejoue ces vérifications à chaque push, lance l'analyse SonarQube, publie les images Docker du front
et du backend sur GHCR (`latest` et l'identifiant du commit) et les binaires de l'agent dans la release
`agent-latest`. La mise à jour de la VM se fait à la main, avec les deux commandes ci-dessus.

## Limites connues

Elles sont détaillées dans `REVUE_DE_CODE.md`. Les principales :

- **Le formateur n'est limité à sa salle que par l'interface.** L'API ne connaît pas la salle du formateur :
  avec un outil qui appelle l'API directement, il peut lire les remontées des autres ou arrêter une
  application sur un poste d'une autre salle.
- **La liste noire ne s'applique que lorsqu'une page de supervision est ouverte**, et l'arrêt est alors
  attribué à la personne qui regarde.
- **L'agent accepte par défaut n'importe quel certificat** et tourne avec les droits système.
- **Le déploiement est celui d'une démonstration** : base, Adminer, backend et console Keycloak sont
  publiés sur la machine, Keycloak tourne en mode développement, et le changement de rôle « de
  démonstration » est actif tant que `DEMO_ACCOUNTS` et `VITE_DEMO_ACCOUNTS` ne valent pas `false`.
- **La masterisation est simulée** : elle ne porte que sur des postes simulés, côté interface.
- **Pas encore fait** : statuts « hors service » et « à rendre », destination du matériel, date de
  garantie, import depuis RECON, type de problème enregistré à part dans une remontée.
- **Aucun test automatisé du front** dans le dépôt.

## Documents

| Document | Contenu |
|---|---|
| `REVUE_DE_CODE.md` | Revue de code : constats de sécurité et de qualité, avec leur état |
| `materiel-frontend/README.md` | Développement du front, organisation du code, écrans |
| `materiel-agent/README.md` | Configuration, compilation et lancement de l'agent |
| `anasi/` | Diagrammes d'analyse |

Les numéros `RG-xx` cités dans le code renvoient aux règles de gestion validées avec le client. Leur liste
(fichier de suivi des règles de gestion) et le cadre de développement du front (`CADRE_FRONT_IA.md`) sont
des documents de travail de l'équipe, conservés hors de ce dépôt.
