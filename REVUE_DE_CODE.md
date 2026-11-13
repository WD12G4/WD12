# BASTION (WD12 G4) — Revue de code complète

- **Date :** 13 novembre 2026
- **État relu :** branche `proto-backend-frontend`, commit `4a8a7a4` (copie extraite hors du dépôt ; aucun fichier du projet n'a été modifié)
- **Périmètre :** backend Spring, agent C#, front Vue, Docker / Nginx / Keycloak, CI
- **Références :** tous les `chemin:ligne` renvoient au commit `4a8a7a4`, pas à la branche `frontBase_ESL` actuellement ouverte en local.

> Ce fichier remplace la première version, qui portait sur `frontBase_ESL`. Il est exclu de Git en local (`.git/info/exclude`) pour ne pas partir sur GitHub par erreur.

## Ce qui a été exécuté, et ce qui ne l'a pas été

| Vérification | Résultat |
|---|---|
| Front : `npm ci && npm run build` (vue-tsc + vite) | ✅ passe |
| Cadre front : `outils/check_rules.py` | 3 écarts, ceux déjà connus (voir § 4) |
| Backend : `./gradlew test` | ✅ **passe** (Java 21 LTS, tests exécutés avec succès) |
| Application lancée, agent réel, Keycloak | ❌ non testés : tout le reste vient de la lecture du code |

Chaque constat est marqué **Vérifié** (lu dans le code, mécanisme certain), **À confirmer** (probable, à reproduire avant de corriger) ou **Corrigé** (correctif appliqué et testé).

---

## 1. Résumé

Le socle « priorité 1 » (matériel, salles, comptes) est propre, cohérent entre front et back, et le front respecte presque entièrement son cadre. La connexion par Keycloak est bien faite : jeton en mémoire, PKCE, rôle porté par l'application et non par le jeton.

Les risques étaient concentrés sur la **supervision**, la partie la plus récente :

1. **Deux contrôleurs sans aucun contrôle de rôle** : n'importe quel compte connecté peut interdire une application ou arrêter un processus sur n'importe quel poste.
2. ~~**Le canal des agents est ouvert**~~ : ✅ **Corrigé (commit `a744c4b`)** via secret partagé (Handshake) et validation du matériel en base.
3. **L'agent tourne en SYSTEM / root et ne vérifie pas le certificat du serveur** : qui se place entre l'agent et le serveur peut lui faire arrêter des processus.
4. **Les remontées et le journal ne sont protégés que par le front** : un formateur peut lire toutes les remontées et écrire de fausses lignes dans le journal des arrêts (I2 ✅ **Corrigé** : route `POST /killevents` retirée).
5. **Le déploiement publie tout** : base, Adminer, backend et console Keycloak (`admin / admin`) sont joignables directement.

Pour la soutenance, les points 1 et 4 se corrigent en quelques lignes d'annotations et sont les plus rentables.

### Qui est concerné

| Étiquette | Personne |
|---|---|
| back, agent, déploiement, CI | `wolkern721` |
| Keycloak, HTTPS Nginx | `r.adamczak` |
| front | Paul |

---

## 2. Constats

### 🔴 Critique

#### C1 — Catalogue et liste noire modifiables par tout compte connecté
- **Où :** `materiel-backend/src/main/java/net/ent/etnc/controllers/ApplicationController.java:59`, `:70`, `:82`, `:94`
- **Statut :** Vérifié — **Effort :** faible — **Concerné :** back
- **Problème :** aucune annotation `@PreAuthorize` sur la création, la modification, l'interdiction et la suppression. Le front réserve bien l'onglet à l'administrateur (`supervision-view.vue:42-46`), mais l'API accepte tout le monde. Écart à RG-27.
- **Scénario :** un formateur envoie `PATCH /api/v1/applications/12/blacklist {"blacklisted": true}` sur le navigateur de la salle → à la prochaine lecture des processus, le backend ferme ce navigateur sur tous les postes supervisés, dans toutes les salles.
- **Correction :** `@PreAuthorize("hasRole('ADMIN')")` sur les quatre écritures (ou sur la classe, lecture comprise si elle n'a pas à être publique).

#### C2 — Supervision : arrêt d'un processus sur n'importe quel poste, par n'importe qui
- **Où :** `…/controllers/SupervisionController.java:31`, `:39`, `:47`, `:55` ; `…/services/impl/SupervisionServiceImpl.java:221-233`
- **Statut :** Vérifié — **Effort :** moyen — **Concerné :** back
- **Problème :** aucun contrôle de rôle ni de salle. La salle du formateur n'existe que dans l'adresse du front (`?salle=`, `use-teaching-room.ts`) ; le backend ne la connaît pas. Écart à RG-34.
- **Scénario :** un formateur de la salle 10 envoie `POST /api/v1/supervision/kill {"workstationId": 42, "pid": 4312, "processName": "x"}` pour un poste de la salle 20 → le processus est arrêté.
- **Correction :** au minimum, vérifier dans `killProcess` et `findProcessesForWorkstations` que le poste appartient à la salle demandée, et passer la salle en paramètre. La vraie solution (rattacher le formateur à une salle côté serveur) est plus lourde : à présenter comme limite connue si le temps manque.

#### C3 — Canal des agents sans authentification
- **Où :** `…/security/WebSecurityConfig.java:60`, `…/websocket/WebSocketConfig.java:26`, `…/websocket/AgentWebSocketHandler.java:110-132`, `…/services/impl/SupervisionServiceImpl.java:66-76`
- **Statut :** ✅ **Corrigé (commit `a744c4b`)** — **Effort :** moyen — **Concerné :** back, agent
- **Problème initial :** `/ws/agent` était en `permitAll`, toutes origines, et `REGISTER` était cru sur parole. `connectedAgents.put(serialNumber, …)` remplaçait l'agent déjà connecté sous ce numéro. Tout numéro inconnu créait une fiche « UC » dans le matériel.
- **Scénario :** depuis n'importe quel PC du réseau, un client WebSocket envoyait `{"type":"REGISTER","serialNumber":"<série d'un vrai poste>"}` → il prenait la place du vrai agent, renvoyait une fausse liste de processus et répondait « arrêté » aux ordres. Avec des numéros inventés, il remplissait l'inventaire de fausses UC.
- **Correction appliquée :**
  - Authentification au handshake WebSocket via secret partagé (`AgentHandshakeInterceptor`) vérifiant l'en-tête `X-Agent-Secret` (comparaison constante `MessageDigest.isEqual` contre les attaques de timing).
  - Validation en base du numéro de série dans `AgentWebSocketHandler` : fermeture immédiate de la session (`POLICY_VIOLATION`) si le matériel est inconnu, archivé ou n'est pas une UC.
  - Suppression de l'auto-création aveugle de fiches UC dans `SupervisionServiceImpl`.
  - Intégration du secret partagé dans l'agent C# .NET 10 (`Config.cs`, `config.json`, argument `--secret`, variable `WD12_AGENT_SECRET`).
  - Tests unitaires complets au vert : `AgentHandshakeInterceptorTest` et `AgentWebSocketHandlerTest`.

---

### 🟠 Important

#### I1 — Un formateur peut lire toutes les remontées
- **Où :** `…/controllers/ReportController.java:39-57` et `:62-67` ; `…/services/impl/ReportServiceImpl.java:130-144`
- **Statut :** Vérifié — **Effort :** faible — **Concerné :** back
- **Problème :** `GET /reports`, `?userId=`, `?materialId=`, `?handled=` et `GET /reports/{id}` ne regardent pas qui demande. Seul le front filtre (`report-view.vue:44`). Écart à RG-38.
- **Scénario :** un formateur appelle `GET /api/v1/reports?userId=3` → il reçoit les remontées d'un collègue.
- **Correction :** reprendre le modèle déjà écrit dans `KillEventServiceImpl.java:49-91` : si le rôle n'est pas au moins MANAGER, forcer le filtre sur l'utilisateur courant et refuser l'accès à une remontée qui n'est pas la sienne.

#### I2 — Le journal des arrêts peut être falsifié
- **Où :** `…/controllers/KillEventController.java:60-66`, `…/services/impl/KillEventServiceImpl.java:98-105`, `…/dtos/killevent/KillEventRequest.java:27`
- **Statut :** ✅ **Corrigé** — **Effort :** faible — **Concerné :** back
- **Problème initial :** `POST /killevents` était ouvert à tout compte et acceptait un `userId` arbitraire. Le front ne l'appelle jamais : les arrêts réels sont journalisés exclusivement par `SupervisionServiceImpl` lors des communications WebSocket avec l'agent.
- **Scénario :** un compte envoyait `POST /killevents {"processName":"chrome.exe","ucId":5,"killDateTime":"…","userId":1}` → le journal affichait un arrêt attribué à l'administrateur, qui n'avait jamais eu lieu.
- **Correction appliquée :** suppression complète de la route `POST /api/v1/killevents` dans `KillEventController`. Le journal des arrêts est désormais strictement en lecture seule via l'API REST ; les enregistrements sont générés exclusivement par le service interne de supervision. Test automatisé adapté vérifiant le retour `405 Method Not Allowed`.

#### I3 — L'agent ne vérifie pas le serveur et tourne avec tous les droits
- **Où :** `materiel-agent/Program.cs:49-52`, `materiel-agent/Config.cs:21`, `materiel-agent/install-service.bat:34`, `materiel-agent/install-service.sh:36` ; `materiel-frontend/nginx.conf:9-19`
- **Statut :** Vérifié — **Effort :** moyen — **Concerné :** agent, déploiement
- **Problème :** `allow_self_signed_cert` vaut `true` par défaut et accepte **n'importe quel** certificat. L'agent tourne en SYSTEM (Windows) ou root (Linux) et exécute tout ordre `KILL_PROCESS` reçu. Nginx sert aussi `/ws` en HTTP clair sur le port 80.
- **Scénario :** un stagiaire détourne le trafic de son poste (ARP) vers sa machine, présente un certificat quelconque → l'agent s'y connecte et arrête les processus qu'on lui désigne.
- **Correction :** le certificat auto-signé est une décision assumée ; il suffit d'**épingler son empreinte** dans `config.json` et de la comparer dans le callback, au lieu de renvoyer `true`. Retirer `/ws` du port 80 une fois les agents en `wss://`.

#### I4 — Après une reconnexion, un poste connecté peut apparaître éteint
- **Où :** `…/websocket/AgentWebSocketHandler.java:126-128` et `:194-199`
- **Statut :** ✅ **Corrigé (commit `a744c4b`)** — **Effort :** faible — **Concerné :** back
- **Problème initial :** à la fermeture d'une session, l'agent était retiré **par numéro de série**, sans vérifier que la connexion enregistrée était bien celle qui se fermait.
- **Scénario :** coupure réseau → l'agent se reconnecte et s'enregistre (nouvelle session) → le serveur détecte ensuite la mort de l'ancienne session → `connectedAgents.remove(serial)` supprimait la **nouvelle**. Le poste restait « Agent déconnecté » jusqu'au redémarrage de l'agent.\n- **Correction appliquée :** nettoyage ciblé par identifiant de session WebSocket (`computeIfPresent`) dans `afterConnectionClosed` et fermeture propre de l'ancienne session lors d'un remplacement.

#### I5 — La lecture des processus bloque le serveur, poste par poste
- **Où :** `…/services/impl/SupervisionServiceImpl.java:157-173`, `…/websocket/AgentWebSocketHandler.java:231-232` et `:260-261` ; `materiel-frontend/src/features/supervision/composables/use-supervision.ts:97-98`
- **Statut :** ✅ **Corrigé** — **Effort :** moyen — **Concerné :** back, front
- **Problème initial :** trois choses s'additionnaient :
  - Les postes étaient interrogés **l'un après l'autre**, avec 3 s d'attente chacun, dans une transaction SQL qui bloquait une connexion de base de données (sur les 10 du pool HikariCP).
  - `session.sendMessage` était appelé concurremment sans synchronisation, risquant l'exception Tomcat `IllegalStateException` (`TEXT_PARTIAL_WRITING`).
  - Le front relançait la lecture toutes les 10 s, même si la précédente n'était pas terminée.
- **Scénario :** une salle de 15 postes dont les agents ne répondent plus (veille) → chaque lecture dure 45 s, les lectures s'empilent, les 10 connexions sont prises et toute l'application ralentit. Deux personnes sur la même salle → processus qui disparaissent par intermittence.
- **Correction appliquée :**
  - Sortie de l'interrogation réseau de la transaction SQL (`@Transactional` retiré de `findProcessesForWorkstations`).
  - Interrogation en parallèle de tous les agents via `CompletableFuture` et méthode asynchrone non-bloquante `fetchProcessesAsync` dans `AgentWebSocketHandler`, bornée par un délai par poste (3 s) et un délai global (4 s au lieu de N × 3 s).
  - Sessions WebSocket encapsulées dans un `ConcurrentWebSocketSessionDecorator` pour sécuriser les envois concurrents multi-threads.
  - Côté front (`use-supervision.ts`), ajout d'un verrou `refreshing` empêchant le déclenchement d'une nouvelle relecture tant que la précédente est en cours, et vérification de la concordance de salle (`state.roomId !== roomId`).

#### I6 — La liste noire ne s'applique que si quelqu'un regarde, et l'arrêt est attribué au spectateur
- **Où :** `…/services/impl/SupervisionServiceImpl.java:180-200` et `:262-269`
- **Statut :** Vérifié — **Effort :** moyen — **Concerné :** back
- **Problème :** l'arrêt automatique est un effet de bord de `GET /supervision/processes`. Sans page de supervision ouverte, rien n'est fermé. L'arrêt est journalisé au nom de la personne qui consulte ; le repli `admin` est écrit en dur.
- **Scénario :** personne n'a la supervision ouverte → une application interdite tourne librement. Un formateur ouvre la page → le journal indique qu'**il** a arrêté l'application.
- **Correction :** une tâche planifiée (`@Scheduled`) qui relit les postes connectés et applique la liste noire, avec un auteur « système » explicite. `GET` ne doit plus rien modifier (ni arrêt, ni création d'UC, ni ajout au catalogue).

#### I7 — Supprimer un compte efface ses remontées ; schéma SQL et entités divergent
- **Où :** `…/controllers/UserController.java:83-90` ; `docker/initdb/01-schema.sql:57`, `:70`, `:72` ; `…/models/KillEvent.java:20-21` et `:47-49` ; `…/resources/application.yml:37`
- **Statut :** Vérifié — **Effort :** faible (compte), moyen (schéma) — **Concerné :** back
- **Problème :** `DELETE /users/{id}` supprime physiquement (RG-40 demande un archivage ; le front archive déjà par `enabled=false`). Avec le schéma SQL, les remontées du compte partent en cascade et l'auteur des arrêts passe à `NULL`, alors que l'entité le déclare obligatoire. La contrainte d'unicité de `killevents` n'est pas la même dans le script et dans l'entité. `ddl-auto: update` laisse Hibernate retoucher la base par-dessus le script.
- **Scénario :** un administrateur appelle `DELETE /users/7` → toutes les remontées du formateur 7 disparaissent. Sur une base créée par Hibernate seul, la même requête donne une erreur 500 (clé étrangère).
- **Correction :** faire de `DELETE /users/{id}` une désactivation, ou retirer la route. Choisir une seule source pour le schéma (`ddl-auto: validate` + script, ou Flyway).

#### I8 — Erreurs de l'API : 500 sur un doublon, message Java brut sur une validation
- **Où :** `…/handlers/GlobalExceptionHandler.java:40-44` ; `…/controllers/BuildingController.java:70`, `…/controllers/RoomController.java:70` ; `materiel-frontend/src/services/api-error.ts:44`
- **Statut :** Vérifié — **Effort :** faible — **Concerné :** back
- **Problème :** l'unicité n'est contrôlée qu'à la création ; en modification la contrainte SQL échoue et `DataIntegrityViolationException` n'a pas de gestionnaire. Pour `MethodArgumentNotValidException`, c'est `getMessage()` qui est renvoyé, et le front affiche tel quel le message des 400.
- **Scénario :** renommer le bâtiment 102 en 101 → « Une erreur inattendue est survenue ». Envoyer un formulaire invalide → « Validation failed for argument [0] in public … ».
- **Correction :** un gestionnaire `DataIntegrityViolationException` → 409 avec un message clair ; un gestionnaire dédié qui assemble les messages des champs (déjà rédigés en français dans les DTO).

#### I9 — Le déploiement publie la base, Adminer, le backend et la console Keycloak
- **Où :** `docker-compose.prod.yml:12`, `:27-38`, `:53`, `:87`, `:89-90`, `:102` ; `.env.example:6`, `:51-52` ; `docker/keycloak/gestion-parc-realm.json:5`, `:52`, `:61`, `:70`
- **Statut :** Vérifié — **Effort :** faible — **Concerné :** déploiement, Keycloak
- **Problème :** PostgreSQL, Adminer, le backend (8090) et Keycloak (8180) sont publiés sur la VM, en plus de Nginx. La console d'administration de Keycloak est aussi servie sous `/auth/admin`, avec `admin / admin`. Keycloak tourne en `start-dev` (base H2 dans le conteneur, `sslRequired: none`). Les comptes du realm ont pour mots de passe `admin123`, `manager123`, `user123`.
- **Scénario :** depuis un poste de la salle, ouvrir `https://<vm>/auth/admin`, entrer `admin / admin` → contrôle complet des identités. Ou `http://<vm>:8082` (Adminer) avec `postgres / postgres` → passer son propre compte en ADMIN dans la table `users`.
- **Correction :** ne publier que les ports 80 et 443 ; retirer Adminer du compose de production ; changer les mots de passe dans le `.env` de la VM ; bloquer `/auth/admin` dans Nginx. Les comptes de démonstration sont une décision assumée : les garder pour la soutenance, les annoncer comme tels.

#### I10 — Le tag `latest` est republié par chaque branche
- **Où :** `.github/workflows/backend.yml:155-159`, `.github/workflows/frontend.yml` (même bloc) ; `docker-compose.prod.yml:44`, `:61`
- **Statut :** Vérifié — **Effort :** faible — **Concerné :** CI
- **Problème :** tout push sur `main`, `proto-backend-frontend` ou `frontBase_ESL` pousse `:latest`, et la production déploie `:latest`.
- **Scénario :** un push sur `frontBase_ESL` la veille de la soutenance → le prochain `docker compose pull` sur la VM installe cette branche à la place de la version prévue.
- **Correction :** ne pousser `latest` que depuis la branche de référence ; déployer la VM sur un tag de commit figé pour la soutenance.

#### I11 — Les tests ne vérifient aucune règle de sécurité
- **Où :** `materiel-backend/src/test/java/net/ent/etnc/security/WebSecurityConfigTest.java` (un seul test : l'objet s'instancie) ; tests de contrôleurs en MockMvc autonome
- **Statut :** Vérifié — **Effort :** moyen — **Concerné :** back
- **Problème :** aucun test n'exerce `@PreAuthorize` ni la chaîne de filtres. C'est ce qui a laissé passer C1 et C2 avec une suite annoncée au vert.
- **Correction :** quelques tests `@SpringBootTest` + `@AutoConfigureMockMvc` avec `jwt()` : pour chaque route d'écriture, un rôle insuffisant doit recevoir 403.

---

### ❓ À confirmer (à reproduire avant de corriger)

| # | Sujet | Où | Pourquoi le vérifier |
|---|---|---|---|
| A1 | Taille des messages WebSocket | `AgentWebSocketHandler.java:88`, aucune limite configurée | Tomcat limite par défaut un message texte à 8 Ko. Une liste de processus avec icônes PNG en base64 devrait la dépasser et fermer la connexion de l'agent. À tester avec un poste Windows chargé ; si confirmé, déclarer un `ServletServerContainerFactoryBean` avec une taille plus grande. |
| A2 | Numéro de série trop long | `SupervisionServiceImpl.java:66-76`, `Material.java` (`@Size(max = 100)`) | Un faux agent enregistré avec un numéro de plus de 100 caractères devrait faire échouer `findWorkstations` pour tout le monde tant qu'il reste connecté. Dépend de C3. |
| A3 | Journal vu par le gestionnaire | `KillEventServiceImpl.java:49-57` | Seul ADMIN voit tous les arrêts ; un gestionnaire ne voit que les siens. À comparer au fichier des RG. |
| A4 | Modification d'une remontée | `ReportServiceImpl.java:86-91` | Le contexte indique « auteur ou gestionnaire » ; le code n'autorise que l'auteur. |
| A5 | Dossier d'installation de l'agent | `install-service.bat:15-17` | Si l'exécutable lancé en SYSTEM est dans un dossier modifiable par le stagiaire, il peut le remplacer. À vérifier sur un poste réel. |

---

### 🟡 Mineur

| # | Sujet | Où | Détail et correction | Effort | Concerné |
|---|---|---|---|---|---|
| m1 | Verrou optimiste inopérant | DTO `*Request.java` (aucun champ `version`) | La version est envoyée au client mais jamais reçue : deux modifications simultanées s'écrasent sans 409. Ajouter `version` aux requêtes et la comparer. | moyen | back, front |
| m2 | Arrêt par PID seul | `AgentWebSocketHandler.java:255-259`, `ProcessManager.cs:371-376` | Si le processus s'est terminé entre l'affichage et le clic, le PID peut désigner un autre programme. Envoyer aussi le nom et l'heure de démarrage, que l'agent compare. | faible | back, agent |
| m3 | Envois simultanés côté agent | `Program.cs:151`, `:206`, `:235-242` | Le battement (toutes les 30 s) et la réponse à une commande peuvent appeler `SendAsync` en même temps, ce que `ClientWebSocket` interdit : le battement s'arrête alors sans bruit. Protéger par un `SemaphoreSlim`. | faible | agent |
| m4 | Filtre par nom contournable | `ProcessManager.cs:163`, `ApplicationServiceImpl.java:194-198` | Un exécutable renommé `dllhost.exe` devient invisible et impossible à arrêter ; renommé autrement, il échappe à la liste noire. Limite à annoncer ; à terme, ne protéger que les binaires du dossier système. | élevé | agent |
| m5 | Icônes sans limite de taille | `ApplicationRequest.java:14`, `Application.java:38`, `AgentWebSocketHandler.java:153` | Colonne `TEXT`, aucune borne ni contrôle de format côté back (le front filtre bien, `app-icon.dto.ts:8`). Borner la taille et valider le préfixe `data:image/…;base64,`. | faible | back |
| m6 | Réponse périmée en supervision | `use-supervision.ts:43-54` | Changer de salle pendant une lecture en cours peut afficher les postes de l'ancienne salle jusqu'au rafraîchissement suivant. ✅ **Corrigé** : vérification de la concordance de salle (`state.roomId !== roomId`) après les promesses asynchrones dans `loadRoom`. | faible | front |
| m7 | Requêtes N+1 sur le matériel | `MaterialController.java:41`, `MaterialRepository.java`, `application.yml:35` | Salle et bâtiment chargés un par un à la sérialisation. ✅ **Corrigé** : ajout des `@EntityGraph(attributePaths = {"room", "room.building"})` dans `MaterialRepository` comme pour `ReportRepository` et `KillEventRepository`. | faible | back |
| m8 | Listes chargées en entier | `services/*.services.ts` (`size=1000`), `kill-events.services.ts:19` | Au-delà de 1000 lignes (500 pour le journal, filtré par salle côté front), les éléments suivants disparaissent sans message. Déjà tracé en `TODO(perf)`. | moyen | back, front |
| m9 | Garde-fous sur les comptes | `UserController.java:70-78`, `UserServiceImpl.java` | Un administrateur peut se désactiver ou se rétrograder, même s'il est le dernier. ✅ **Corrigé** : contrôle dans `UserServiceImpl` bloquant la désactivation, rétrogradation ou suppression du dernier administrateur actif. | faible | back |
| m10 | Suppression salle / bâtiment | `RoomController.java:84`, `BuildingController.java:84`, `01-schema.sql:30`, `:43` | Supprimer un bâtiment supprime ses salles et remet leur matériel en stock, sans règle validée (question RG-31 ouverte dans le front). | faible | back |
| m11 | Injection dans la CI | `backend.yml:150-152`, `frontend.yml:137-139` | `${{ inputs.image_tag }}` est collé dans un script shell sur un runner auto-hébergé. Passer par `env:`. | faible | CI |
| m12 | En-têtes de sécurité Nginx | `materiel-frontend/nginx.conf:30-109` | Ni HSTS, ni `X-Content-Type-Options`, ni `frame-ancestors`. | faible | déploiement |
| m13 | Journalisation | `application.yml:66` | `DEBUG` par défaut pour toute l'application. | faible | back |

---

### 🔵 Suggestions

- **Lien compte ↔ identité :** le rôle est retrouvé par `preferred_username` (`KeycloakJwtConverter.java:75`). C'est correct tant que le realm interdit l'inscription et le changement d'identifiant (c'est le cas). Avec le vrai MinDef Connect, préférer un identifiant stable (`sub`) et vérifier l'audience du jeton.
- **Mode démonstration :** actif par défaut des deux côtés (`application.yml:57`, `login-view.vue:31`, `user-card.vue:30`) et absent de `.env.example`. Décision assumée ; ajouter `DEMO_ACCOUNTS` et `VITE_DEMO_ACCOUNTS` au modèle de `.env` pour que la désactivation soit une ligne à changer. Le contrôle ADMIN de l'en-tête `X-Demo-Account` est correct (`KeycloakJwtConverter.java:86-93`).
- **État des agents en mémoire** (`AgentWebSocketHandler.java:26`) : incompatible avec deux instances du backend et perdu au redémarrage. À présenter comme limite connue, avec la piste (table d'état ou diffusion entre instances).
- **Validation écrite trois fois** (DTO, entité, service) : garder Bean Validation pour le format, les services pour les règles métier.
- **Images non figées :** `nginx:alpine`, `adminer:latest` (`materiel-frontend.dockerfile:33`, `docker-compose.prod.yml:28`).
- **`materiel-agent/config.json`** versionné avec une adresse et un numéro de série réels : le remplacer par un modèle.
- **Confirmations en `window.confirm`** (6 endroits) : fonctionnel, mais non stylable ni cohérent avec les thèmes.

---

## 3. Points forts à garder (utiles pour l'oral)

- **Connexion :** code d'autorisation + PKCE, jeton gardé en mémoire par `keycloak-js`, renouvellement mutualisé, aucun mot de passe vu par l'application.
- **Séparation identité / rôle :** Keycloak prouve qui se connecte, la table `users` décide des droits ; un compte inconnu ou désactivé est refusé à chaque requête (`KeycloakJwtConverter.java:76-77`).
- **Journal des arrêts bien protégé en lecture :** filtrage par rôle dans le service (`KillEventServiceImpl.java:49-91`), modification et suppression interdites. C'est le modèle à recopier pour les remontées.
- **Agent prudent :** refuse d'arrêter les processus système, la session 0 de Windows, les processus des comptes système Linux et lui-même ; fermeture propre avant l'arrêt forcé ; reconnexion automatique.
- **Front :** un seul point d'appel à `fetch`, routes typées à la compilation, icônes venues des postes filtrées par motif strict (`app-icon.dto.ts:8`), redirection après connexion protégée, droits d'affichage alignés sur le backend, build et vérification de types au vert.
- **Remontées et journal sans N+1 :** `@EntityGraph` sur `ReportRepository` et `KillEventRepository`.
- **Déploiement :** HTTPS de bout en bout côté navigateur, certificat conservé dans un volume, image backend sans root, CI complète avec tests, couverture et Sonar.
- **Traçabilité :** références aux RG dans le code et questions ouvertes au client en `TODO(question)`.

---

## 4. Écarts au cadre front et aux règles de gestion

### Cadre front (`CADRE_FRONT_IA.md`)

Résultat du vérificateur, plus quelques recherches ciblées (couleurs en dur, `any`, `fetch`, stockage) :

| Écart | Où | Commentaire |
|---|---|---|
| Fichier `.ts` > 120 lignes | `shared/composables/use-auth.ts` (235 lignes) | À découper : session Keycloak d'un côté, outil de démonstration (`actAs`, stockage) de l'autre. Concerné : Keycloak. |
| Fichier `.ts` > 120 lignes | `router/navigation.ts` (141 lignes) | Sortir le calcul du lien « Retour » dans son propre fichier. |
| Stockage navigateur | `use-auth.ts:50`, `:60-61` | `sessionStorage` pour le compte de démonstration. Lié à l'outil de démonstration assumé ; disparaît avec lui. |
| `console.error` | `use-auth.ts:127` | Le cadre vise `console.log` ; à trancher. Non détecté par le vérificateur. |

Aucun autre écart trouvé : pas de `fetch` hors `http-client.ts`, pas de `any`, pas de couleur en dur dans les composants. Huit composants sont entre 113 et 120 lignes, donc à la limite (`station-tile.vue` : 120).

### Règles de gestion

Le fichier `Suivi_RG_WD12_G4.xlsx` n'était pas dans l'archive : ce tableau repose sur les RG citées dans le code et dans les documents de contexte.

| RG | État constaté dans le code | Constat |
|---|---|---|
| RG-04 (date de fin facultative) | ✅ corrigé côté back (`Material.java:46`, `MaterialRequest.java:19`) | — |
| RG-21 (type de problème) | ❌ aucun champ dans `Report.java` | à faire, back + front |
| RG-27 (liste noire : administrateur) | ❌ front seul | C1 |
| RG-34 (formateur limité à sa salle) | ❌ front seul | C2 |
| RG-38 (formateur : ses remontées) | ❌ front seul | I1 |
| RG-40 (archivage des comptes) | ⚠️ le front archive, l'API supprime encore | I7 |
| RG-31 (suppression salle / bâtiment) | ⚠️ question ouverte | m10 |

---

## 5. Ordre de correction proposé avant la soutenance

| Priorité | Constat | Effort | Qui | État |
|---|---|---|---|---|
| 1 | C1 — annotations sur `ApplicationController` | faible | back | À faire |
| 2 | I2 — retirer `POST /killevents` | faible | back | ✅ **Fait** |
| 3 | I1 — filtrer les remontées par auteur | faible | back | À faire |
| 4 | I4 — retrait de l'agent par session | faible | back | ✅ **Fait (commit `a744c4b`)** |
| 5 | I9 — ports publiés, mots de passe de la VM | faible | déploiement | À faire |
| 6 | I10 — figer le tag déployé sur la VM | faible | CI | À faire |
| 7 | I8 — gestionnaires d'erreurs | faible | back | À faire |
| 8 | C2, ~~C3~~, I3, ~~I5~~, I6 — supervision et agent | moyen | back, agent | C3 ✅ **Fait (commit `a744c4b`)**, I5 ✅ **Fait** |

Les points 1 à 7 tiennent en une demi-journée. Les points du groupe 8 demandent plus de temps : s'ils ne sont pas faits, les présenter à l'oral comme des limites identifiées, avec la correction prévue.
