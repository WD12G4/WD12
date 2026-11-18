# BASTION

Application de gestion du matériel informatique, des salles et de supervision des postes de cours (projet WD12, groupe 4).

BASTION répond à trois besoins opérationnels majeurs :

1. **Gérer le parc informatique** : inventaire complet du matériel (numéro de série, type, localisation en salle ou en stock, date de fin de disponibilité, archivage soft delete), gestion des salles, bâtiments et comptes utilisateurs.
2. **Remontée et suivi des incidents** : signalement simplifié des pannes matérielles par les formateurs, traitement et suivi par les gestionnaires, et génération d'un document d'état des lieux imprimable.
3. **Supervision en direct des postes de cours** : suivi en temps réel des applications en cours d'exécution sur chaque poste, arrêt à distance de processus, application d'une liste noire d'exécutables interdits et consultation du journal des arrêts.

---

## 1. Rôles et Habilitations

Les rôles s'héritent de manière hiérarchique : l'Administrateur dispose de tous les droits du Gestionnaire, qui dispose lui-même de tous les droits du Formateur.

| Rôle | Code rôle | Périmètre et fonctionnalités |
|---|---|---|
| **Formateur** | `USER` | Sélectionne sa salle de cours à la connexion ; consulte le matériel et les postes de sa salle ; signale les pannes ; supervise en direct les applications des postes de sa salle et peut fermer un processus non autorisé. |
| **Gestionnaire** | `MANAGER` | Supervision globale de l'inventaire : crée, modifie, affecte, transfère et archive le matériel ; gère les états des lieux et traite l'ensemble des remontées d'incidents. |
| **Administrateur** | `ADMIN` | Tous les droits précédents, plus : gestion des bâtiments, des salles, des comptes utilisateurs et attribution des rôles ; gestion du catalogue d'applications et de la liste noire ; déclenchement de la masterisation. |

---

## 2. Architecture Technique

### Schéma d'architecture des flux

```
       +--------------------+              +----------------------+
       | Navigateur Client  |              | Agent de Supervision |
       | (Vue 3 / DSFR)     |              | (.NET 10 stagiaires) |
       +---------+----------+              +----------+-----------+
                 | HTTPS (:443)                       | WSS (:443 /ws/agent)
                 +-----------------+  +---------------+
                                   |  |
                                   v  v
                      +---------------------------+
                      |    Reverse Proxy Nginx    |
                      +----+-----------+-----+----+
                           |           |     |
                 / (SPA)   | /api & /ws|     | /auth
                 v         v           |     v
          +------------+ +-------------+  +------------+
          |  Frontend  | |   Backend   |  |  Keycloak  |
          | (Nginx SPA)| | Spring Boot |  | (IdP SSO / |
          |            | |   Java 21   |  |   OAuth2)  |
          +------------+ +------+------+  +-----+------+
                                |               |
                                v               v
                         +-----------------------------+
                         |         PostgreSQL          |
                         |          (BDD v16)          |
                         +-----------------------------+
```

### Organisation du dépôt

| Répertoire | Rôle | Technologies principales |
|---|---|---|
| [`materiel-backend/`](./materiel-backend) | API REST, sécurité OIDC Resource Server, WebSocket supervision | Java 21, Spring Boot 3, Spring Security, JPA / Hibernate, PostgreSQL |
| [`materiel-frontend/`](./materiel-frontend) | Interface utilisateur SPA, authentification PKCE, thèmes & DSFR | Vue 3, TypeScript, Vite, Tailwind CSS / Vanilla CSS |
| [`materiel-agent/`](./materiel-agent) | Agent léger installé sur les postes stagiaires | C# (.NET 10 console), multiplateforme (Windows, Linux, macOS) |
| [`docker/`](./docker) | Scripts SQL d'initialisation (`initdb/`) et configuration du realm (`keycloak/`) | PostgreSQL SQL, JSON realm Keycloak |
| [`.github/workflows/`](./.github/workflows) | Chaîne d'intégration et déploiement continus | GitHub Actions (Build, Test, SonarQube, Docker GHCR) |

### Mécanisme d'authentification
Le frontend ne manipule jamais de mot de passe en clair :
- L'authentification repose sur **Keycloak** via le protocole **OpenID Connect** (Authorization Code Flow avec PKCE).
- Le jeton JWT d'accès est conservé uniquement en mémoire vive (`keycloak-js`) et transmis dans l'en-tête `Authorization: Bearer <token>`.
- Le backend Spring Boot valide la signature cryptographique du jeton auprès de Keycloak, puis charge les autorisations et rôles applicatifs depuis sa propre table `users` en base de données.

---

## 3. Lancer l'application (Docker Compose)

### Prérequis
* [Docker](https://www.docker.com/) et Docker Compose (v2+).
* Un fichier d'environnement `.env` initialisé.

### Démarrage rapide

1. **Initialiser l'environnement :**
   ```bash
   cp .env.example .env
   ```
   *Pour un test en local, les valeurs par défaut (`https://localhost` et `https://localhost/auth`) sont directement prêtes à l'emploi.*

2. **Lancer les conteneurs :**
   * Pour construire les images depuis les sources locales :
     ```bash
     docker compose up -d --build
     ```
   * Ou pour exécuter les images précompilées par la CI :
     ```bash
     docker compose -f docker-compose.prod.yml pull
     docker compose -f docker-compose.prod.yml up -d
     ```

3. **Accéder aux services :**
   * **Interface Web :** [https://localhost](https://localhost)  
     *(Le certificat SSL étant auto-signé pour l'environnement local, acceptez l'avertissement de sécurité du navigateur).*
   * **API Backend REST :** [https://localhost/api](https://localhost/api) (ou port direct `http://localhost:8090`)
   * **Console Keycloak :** [https://localhost/auth/admin](https://localhost/auth/admin) (`admin` / `admin`)
   * **Adminer (Base de données) :** [http://localhost:8082](http://localhost:8082) (système : PostgreSQL, serveur : `postgres`, base/utilisateur/mdp : `postgres`)

> [!IMPORTANT]
> **Règle de rattachement d'un poste :**  
> Pour qu'un poste apparaisse sur la page de supervision, son unité centrale (UC) doit obligatoirement être déclarée dans l'inventaire matériel avec le numéro de série exact émis par l'agent. Dans le cas contraire, le serveur WebSocket rejette la connexion de l'agent.

---

## 4. Comptes de démonstration

Des comptes de test sont préconfigurés dans Keycloak et dans le jeu de données initial (`docker/initdb/02-data.sql`) :

| Identifiant | Mot de passe | Rôle | Périmètre de test |
|---|---|---|---|
| `admin` | `admin123` | **Administrateur** | Accès intégral, administration des salles/bâtiments/comptes, liste noire, masterisation. |
| `manager` | `manager123` | **Gestionnaire** | Inventaire complet, gestion des matériels, traitement des signalements de pannes. |
| `user` | `user123` | **Formateur** | Salle assignée, consultation locale, déclaration d'incidents et supervision de ses postes. |

> [!TIP]
> **Mode Démonstration :**  
> Le bandeau supérieur de l'interface intègre un sélecteur instantané permettant de basculer de rôle sans devoir se reconnecter (actif tant que `DEMO_ACCOUNTS` et `VITE_DEMO_ACCOUNTS` ne sont pas définis à `false`).

---

## 5. Qualité du code et Tests

| Composant | Commande d'exécution | Périmètre vérifié |
|---|---|---|
| **Backend** | `./gradlew test` (dans `materiel-backend/`) | 335 tests unitaires et d'intégration (contrôleurs REST, services métier, unicité, sécurité, WebSocket). |
| **Frontend** | `npm run build`, puis `npm run test:e2e` (dans `materiel-frontend/`) | Contrôle strict du typage TypeScript (`vue-tsc`), compilation de production Vite, et 33 tests navigateur Playwright sur une fausse API (voir `materiel-frontend/README.md`). |
| **Agent** | `dotnet build` (dans `materiel-agent/`) | Compilation de la solution C# multiplateforme. |

À chaque push sur le dépôt, la chaîne CI GitHub Actions rejoue l'ensemble des vérifications, exécute l'analyse de qualité SonarQube, publie les images conteneurs sur GHCR (`latest` et tag de commit) et génère l'exécutable de l'agent dans la release `agent-latest`.

---

## 6. Rapprochement Architecture Cible vs Prototype

Ce dépôt présente la solution au stade de **prototype de démonstration** :

* **En prototype (démonstration actuelle) :** Pour faciliter les démonstrations et les diagnostics, les ports directs du backend (8090), de PostgreSQL (5432) et d'Adminer (8082) sont exposés sur la machine hôte ; Keycloak fonctionne en mode `start-dev` ; le mode démonstration est activé.
* **En architecture cible (spécification DAT) :** Seuls les ports 80 et 443 de Nginx sont publiés vers l'extérieur ; les accès directs à la base de données sont totalement coupés ; Keycloak est raccordé à l'annuaire d'entreprise sécurisé.

---

## 7. Limites connues du prototype

* **Périmètre formateur :** La restriction du formateur à sa seule salle de cours est actuellement appliquée par l'interface web (via `use-teaching-room.ts` et les filtres de navigation) ; l'API backend n'associe pas encore un utilisateur formateur à une salle unique en base.
* **Liste noire applicative :** Les règles d'interdiction sont vérifiées et appliquées lorsque la supervision de la salle est activement ouverte.
* **Masterisation :** La masterisation est simulée côté interface (`services/*.mock.ts`) pour les besoins de la démonstration, en l'absence de protocole PXE/TFTP déployé dans l'environnement.
* **Validation SSL de l'agent :** En environnement de maquette avec certificat auto-signé, l'agent autorise le certificat local via `RemoteCertificateValidationCallback`.

---

## 8. Documents de Référence

* [`REVUE_DE_CODE.md`](./REVUE_DE_CODE.md) : Audit de code, cartographie des vulnérabilités traitées et conformité.
* [`REVUE_GLOBALE_WD12_G4.md`](./REVUE_GLOBALE_WD12_G4.md) : Analyse d'alignement avec les exigences et synthèse globale du projet.
* [`materiel-frontend/README.md`](./materiel-frontend/README.md) : Architecture de l'interface, gestion des thèmes et guide de développement.
* [`materiel-agent/README.md`](./materiel-agent/README.md) : Guide d'installation et de configuration de l'agent de supervision.
