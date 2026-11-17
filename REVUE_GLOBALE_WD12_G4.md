# BASTION (WD12 G4) : revue globale et travaux du 16 novembre 2026

- **Dépôt :** `WD12G4/WD12`, branche `proto-backend-frontend`, commit de départ `7cb3ba0`.
- **Objet :** cohérence entre le code et les documents, puis corrections demandées (points 1 à 5 de la
  revue, sauf le dessin du diagramme des cas d'utilisation).
- **État :** les modifications sont dans le dossier de travail, **ni commitées ni poussées**.
- **Document associé :** `DCU_MODIFICATIONS_WD12_G4.md` (ce qu'il faut changer dans le diagramme des cas
  d'utilisation).

## 1. En bref

Le code est en bon état : 331 tests du backend au vert, front qui compile et respecte son cadre. La
documentation, elle, avait décroché : presque tous les documents décrivaient le projet du 13 novembre ou
d'avant, et le lien entre les règles de gestion (RG) et le code était cassé.

Après les travaux du 16 novembre :

- le dépôt a un vrai README, et celui du front décrit l'application actuelle ;
- le suivi des RG est à jour (19 règles corrigées) et le front cite les bonnes règles ;
- le formateur est limité à sa salle partout dans l'interface, y compris dans « Signaler un problème » ;
- la revue de code ne contient plus de constat périmé.

Ce qui reste ouvert est surtout côté backend et déploiement : la limite du formateur n'existe toujours que
dans l'interface, pas dans l'API.

## 2. Ce qui a été vérifié

| Vérification | Résultat |
|---|---|
| Backend : `./gradlew test` sur `7cb3ba0` (le backend n'a pas été modifié depuis) | 331 tests, 0 échec |
| Front : types (`vue-tsc`) après les modifications | passe |
| Front : construction de production (`vite build`, lightningcss) | passe |
| Front : vérificateur du cadre (`check_rules.py`) | 3 écarts, tous connus et antérieurs (`use-auth.ts` : taille et stockage ; `navigation.ts` : taille) |
| Front : tests navigateur avec fausse API (6 scripts, dont le nouveau `trainer-scope.js`) | 46 contrôles, 0 échec |
| Suivi des RG : recalcul des formules | 67 formules, 0 erreur |
| Agent C# : compilation | non vérifiée (pas de SDK .NET dans l'environnement) |
| Application déployée sur la VM | non testée |

## 3. Constats de la revue

### 3.1 Documents du dépôt (avant travaux)

| Document | État constaté | Écarts principaux |
|---|---|---|
| `Readme.md` (racine) | Vide (« Hello World ») | Ni description, ni architecture, ni lancement. |
| `materiel-frontend/README.md` | Périmé | Remontées et supervision annoncées en « maquette » ; bandeau « Données de démonstration » disparu ; RG-04 annoncée non corrigée ; Node 22 au lieu de 24 ; renvoi à `CADRE_FRONT_IA.md`, absent du dépôt. |
| `REVUE_DE_CODE.md` | Presque à jour | Constat I10 périmé (plus de job de déploiement) ; en-tête « exclu de Git » alors que le fichier est commité ; titre « 2. Constats » répété 5 fois ; délais de I5 différents du code. |
| `materiel-agent/README.md` | Incomplet | Ne dit pas que l'UC doit être déclarée dans le matériel avant d'installer l'agent ; scripts d'installation en service et options `--insecure` / `--strict-ssl` non documentés. |
| `anasi/` diagramme de classes | À jour | Conforme aux 7 entités. |
| `anasi/` cas d'utilisation (02/11) | Périmé | Voir `DCU_MODIFICATIONS_WD12_G4.md`. |
| `.env.example` | Correct | Il manque `DEMO_ACCOUNTS` et `VITE_DEMO_ACCOUNTS`. |

Il n'y a ni README pour le backend, ni documentation de l'API. La note de la release de l'agent annonce
5 archives ; la CI n'en construit que 3.

### 3.2 Documents hors dépôt (avant travaux)

| Document | État constaté |
|---|---|
| `Suivi_RG_WD12_G4.xlsx` (13/11 matin) | Une vingtaine de lignes en retard sur le code : RG-04 « non conforme » alors que corrigée, RG-20 parlant d'un mot de passe qui n'existe plus, RG-23 et RG-25 en « maquette », RG-27 « non commencée », RG-29 décrivant un ajout automatique des postes qui a été retiré, RG-50 prouvée par l'ancienne connexion par mot de passe. |
| `CADRE_FRONT_IA.md` (03/11) | L'audit du 14/11 n'a jamais été appliqué : connexion par mot de passe, remontées et supervision en maquette. |
| `CONTEXTE_WD12.md` (13/11) | Liste « reste à faire » dépassée (HTTPS et RG-04 faits). |
| Kit de déploiement des agents | Ne transmet pas le secret partagé : ne fonctionne que tant que le secret par défaut reste en place sur le serveur. |

### 3.3 Le lien entre les RG et le code

- Le front citait des RG environ 290 fois, mais seulement RG-01 à RG-32 : aucune des règles ajoutées après
  les entretiens 3 à 5 (RG-33 à RG-50).
- Il citait encore trois règles rejetées : RG-17, RG-18 (« le formateur voit toutes les salles ») et RG-22.
- Le backend ne cite que 4 RG.
- Le fichier des RG n'est pas dans le dépôt (choix de l'équipe) : pour qui n'a que le dépôt, les numéros
  ne renvoyaient à rien, et aucun document ne le disait.

### 3.4 Incohérences entre les règles et l'application

| Règle | Constat |
|---|---|
| RG-34 (formateur limité à sa salle) | Appliquée à moitié dans le front : « Salles », « Bâtiments », la fiche d'une autre salle et « Signaler un problème » restaient ouverts. Côté API, rien. |
| RG-46 (salle imposée dans une remontée) | Annoncée au client, pas faite. |
| RG-23 (notification du gestionnaire) | Validée comme « pop-up à la connexion », réalisée comme un compteur dans le menu. |
| RG-27 (liste noire) | Liste globale ; la règle parle d'une salle ou d'un bâtiment. |
| RG-21 (type de problème) | Écrit au début de la description, faute de champ côté backend. |
| Schéma de la base | ✅ **Corrigé** : `01-schema.sql` aligné sur l'entité JPA `KillEvent` (`user_id BIGINT NOT NULL`, unicité 4 colonnes, clés `ON DELETE RESTRICT`), et `application.yml` configuré avec `ddl-auto: validate`. |

### 3.5 Sécurité encore ouverte (confirmé dans le code)

| Constat (identifiant de `REVUE_DE_CODE.md`) | Effet |
|---|---|
| Supervision sans contrôle de salle (C2) | Tout compte connecté peut arrêter une application sur n'importe quel poste. |
| Remontées lisibles par tous (I1) | Un formateur peut lire celles des autres en appelant l'API. |
| Suppression physique des comptes (I7, RG-40) | ✅ **Corrigé** : `UserServiceImpl.deleteById` effectue un archivage logique (`enabled=false`), et les clés étrangères SQL empêchent les suppressions en cascade (`RESTRICT`). |
| Agent (I3) | Accepte n'importe quel certificat et tourne en root ou SYSTEM. |
| Liste noire (I6) | Ne s'applique que si une page de supervision est ouverte ; l'arrêt est attribué au spectateur. |
| Erreurs de l'API (I8) | ✅ **Corrigé** : gestionnaire `DataIntegrityViolationException` (409) et messages de validation propres dans `GlobalExceptionHandler` ; contrôle d'unicité en `update` dans les services. |
| Déploiement (I9) | Base, Adminer, backend et console Keycloak (`admin / admin`) publiés sur la VM. |
| Secret des agents | Valeur par défaut écrite dans 5 fichiers versionnés (`application.yml`, `.env.example`, `Config.cs` et `config.json` de l'agent, et son README). |

### 3.6 Hygiène du dépôt

- `main` contient un seul commit, du 20 octobre. Tout le projet est sur `proto-backend-frontend`
  (143 commits d'avance), plus 6 autres branches.
- Aucun test automatisé du front dans le dépôt.
- Restes : dépendance `rsql-jpa` inutilisée côté backend, `.gitignore` qui parle de binaires Go,
  `config.json` de l'agent versionné avec une adresse et un numéro de série réels.
- Historique : 3 commits du 12/11 sont signés « Claude », dont 2 avec `Co-Authored-By`. Ils sont déjà
  poussés ; les retirer demanderait de réécrire l'historique partagé.

### 3.7 Dossier d'architecture (`DAT_BASTION_V0.4.docx`, reçu le 17/11)

Le DAT n'est pas dans le dépôt. Il a été relu le 17 novembre et confronté au code ; il n'a pas été modifié.

**Ce qui est juste.** Les 16 versions du tableau correspondent exactement au dépôt (fichiers de verrouillage,
`build.gradle`, wrapper Gradle, images Docker). La description de l'authentification est exacte : Keycloak,
code d'autorisation et PKCE, jeton d'accès de 5 minutes, aucun mot de passe ni jeton dans PostgreSQL, rôles lus
dans la base. Le canal des agents (wss par Nginx), le PDF par l'impression du navigateur et l'agent C# .NET 10
sont bien décrits.

| Type | Constat | Preuve dans le dépôt |
|---|---|---|
| Faux | « L'API n'est joignable de l'extérieur que par Nginx. » | Les deux fichiers compose publient aussi le backend (8090), PostgreSQL (5432), Adminer (8082) et Keycloak (8180) sur la machine. |
| Faux | « L'API est le seul composant qui contacte la base. » | Le conteneur Adminer s'y connecte aussi ; il est absent du texte, du schéma et du tableau des versions. |
| Faux | « Nginx redirige le port 80 vers le port 443. » | Vrai sauf pour `/ws`, que `nginx.conf` relaie en clair sur le port 80. |
| Inexact | Interface faite des « composants du DSFR, thème clair ou sombre ». | Les composants sont ceux du projet. Le DSFR est un des six thèmes ; seul son bloc-marque vient du paquet officiel. |
| Inexact | L'agent « accepte le certificat auto-signé ». | Il accepte n'importe quel certificat (`RemoteCertificateValidationCallback` renvoie toujours vrai) : le serveur n'est pas authentifié. |
| Juste (après correction) | Schéma créé par les scripts SQL. | Corrigé : `application.yml` utilise maintenant `ddl-auto: validate`, garantissant que le script SQL est la source unique de vérité. |
| Inexact | CI sur exécuteurs auto-hébergés, chaque modification « testée ». | L'agent est construit sur des exécuteurs de GitHub ; le front n'a aucun test ; la mise à jour de la VM est manuelle. |
| Manquant | Authentification des agents. | Secret partagé (`X-Agent-Secret`) vérifié à la connexion, et UC obligatoirement déclarée dans le matériel. |
| Manquant | Keycloak tourne en mode développement. | `start-dev`, sans volume : un compte créé dans la console disparaît quand le conteneur est recréé. |
| Manquant | Mode démonstration. | Changement de rôle sans se reconnecter, actif par défaut (`DEMO_ACCOUNTS`, `VITE_DEMO_ACCOUNTS`). |
| Inachevé | Section « Rapprochement au réel ». | Les 7 lignes du tableau sont vides. |
| Inachevé | Ligne PostgreSQL du tableau des versions. | La raison du choix est « À définir ». |
| Inachevé | « La redondance fera l'objet d'un autre chapitre. » | Ce chapitre n'existe pas. |
| À confirmer | Les librairies sont récupérées « sur Medusa ». | Rien dans le dépôt ne configure Medusa : Maven Central, registre npm et proxy de l'école. |
| Nommage | Le nom BASTION n'apparaît que dans le logo ; « MindefConnect Internet ». | Le reste du projet écrit « BASTION » et « MinDef Connect ». |

Le premier constat est le plus important : le DAT décrit l'architecture voulue, le compose déploie autre
chose. Il faut soit corriger le compose (ne publier que 80 et 443, retirer Adminer), soit le dire dans le DAT.

## 4. Ce qui a été fait le 16 novembre

### Point 1 : README racine

`Readme.md` réécrit : ce qu'est BASTION, les trois rôles, l'architecture (schéma et tableau des dossiers),
le lancement par Docker, les comptes de démonstration, les vérifications, les limites connues, la liste
des documents. Il dit aussi où vivent les documents hors dépôt (liste des RG, cadre du front).

### Point 2 : suivi des RG et numéros cités dans le front

**Fichier de suivi** (`Suivi_RG_WD12_G4.xlsx` et `.ods`, version 4) : 19 règles mises à jour, avec les
tests du backend comme preuves.

| Règle | Changement |
|---|---|
| RG-04 | Back « Testé », front « Codé » (date facultative, corrigée le 13/11). |
| RG-15, RG-19, RG-48 | Back « Testé » (tests des rôles, du compte désactivé, du changement de rôle de démonstration). |
| RG-20, RG-32 | Reformulées : plus de mot de passe dans l'application depuis Keycloak. |
| RG-23 | Commentaire : API réelle, écart « compteur au lieu de pop-up » signalé. |
| RG-24 | Back « Sans objet » (calculé par le front). |
| RG-25 | Commentaire : postes réels lus par l'API, plus de « maquette ». |
| RG-27 | Back et front « En cours » : liste noire faite et testée, mais globale, pas par salle. |
| RG-29 | Reformulée : un poste inconnu est refusé (il n'est plus ajouté automatiquement) ; back « Testé ». |
| RG-31 | Question levée : suppression refusée si la salle ou le bâtiment n'est pas vide. |
| RG-33, RG-34, RG-35 | Preuves côté front ajoutées ; RG-34 décrit ce qui est limité à la salle et ce qui ne l'est pas. |
| RG-39, RG-40 | Commentaires complétés (auteur dans le journal, dernier administrateur protégé). |
| RG-46 | Front « Codé ». |
| RG-50 | Reformulée : Keycloak joue MinDef Connect ; back « Testé » ; l'ancienne preuve n'existait plus. |

La version précédente est conservée à côté (`Suivi_RG_WD12_G4.v3_2026-11-13.xlsx` et `.ods`).

**Front** : commentaires réalignés dans 32 fichiers, en plus de ceux que touchent les points 3 et 4.

- Les règles rejetées ne sont plus citées : RG-17 est remplacée par RG-16, RG-18 par RG-34, RG-22 par RG-35.
- Le traitement des remontées cite RG-36 (et non plus RG-23, qui reste la notification).
- Les règles RG-33 à RG-50 sont citées là où elles sont appliquées : choix de salle, journal, archivage
  des comptes, logo, thèmes, changement de rôle de démonstration, MinDef Connect.

### Point 3 : README du front, code mort, TODO périmés

- `materiel-frontend/README.md` : tableau des écrans refait (tous appellent l'API), ce qui reste simulé,
  la limite du formateur, l'apparence et le mode démonstration, les points ouverts à jour, Node 24.
- Code mort retiré : `shared/components/demo-banner.vue` et ses styles, classe `.plain-list`.
- TODO périmés retirés : PDF des remontées (décision prise, RG-35), suppression d'une salle ou d'un
  bâtiment (tranchée côté backend). Les messages de confirmation annoncent maintenant le refus du backend.

### Point 4 : formateur limité à sa salle dans tout le front (RG-34, RG-46)

- **Cause :** la limite n'était posée que dans trois vues (tableau de bord, Matériel, Supervision), chacune
  recalculant la salle du formateur de son côté.
- **Correction :** la salle qui limite l'affichage (`trainerRoomId`) est maintenant fournie une fois, par
  `use-teaching-room.ts`, et utilisée partout.

| Écran | Avant | Après |
|---|---|---|
| Salles | Toutes les salles | Sa salle seulement, titre « Votre salle de cours » |
| Bâtiments | Tous les bâtiments | Le bâtiment de sa salle |
| Fiche d'une autre salle | Affichée, avec son matériel | Refusée (« Salle non accessible »), avec un bouton « Ouvrir ma salle » ; son matériel n'est pas lu |
| Signaler un problème | Toutes les salles, tout le matériel | Salle imposée et non modifiable, matériel de cette salle seulement |

Le gestionnaire et l'administrateur gardent tout le parc et le choix de la salle.

**Limites qui restent :** la fiche d'un matériel d'une autre salle s'ouvre encore par son adresse, et
surtout l'API ne limite toujours pas le formateur. Le contrôle réel est à faire côté backend.

### Point 5 : `REVUE_DE_CODE.md`

- Constat I10 requalifié : il n'y a plus de job de déploiement, la CI publie `latest` et le tag du commit
  à chaque push. Le problème restant est que la VM suit `latest` ; la correction est d'écrire le tag du
  commit validé dans le `.env` de la VM.
- En-tête corrigé (le fichier est versionné), note de mise à jour du 16/11 ajoutée.
- Titres « 2. Constats » en double retirés ; délais de I5 alignés sur le code (5 s par poste, 6 s au total).
- Lignes RG-34 et RG-46 du tableau des règles mises à jour.
- Le diagramme des cas d'utilisation n'a pas été redessiné : voir `DCU_MODIFICATIONS_WD12_G4.md`.

## 5. État des documents après travaux

| Document | État |
|---|---|
| `Readme.md` (racine) | À jour |
| `materiel-frontend/README.md` | À jour |
| `REVUE_DE_CODE.md` | À jour sur les points recontrôlés (C1, C2, C3, I1, I2, I3, I6, I7, I8, I9, I10, m9, m10) |
| `Suivi_RG_WD12_G4.xlsx` / `.ods` (hors dépôt) | À jour (version 4) |
| `anasi/` diagramme de classes | À jour |
| `DAT_BASTION_V0.4.docx` (hors dépôt) | **À corriger** : 3 affirmations fausses, 4 inexactes, 3 manques, 3 parties inachevées (§ 3.7) |
| `anasi/` cas d'utilisation | **À refaire**, d'après `DCU_MODIFICATIONS_WD12_G4.md` |
| `materiel-agent/README.md` | **À compléter** (non touché) |
| `.env.example` | **À compléter** : `DEMO_ACCOUNTS`, `VITE_DEMO_ACCOUNTS` (non touché) |
| `CADRE_FRONT_IA.md` (hors dépôt) | **Périmé** : appliquer l'audit du 14/11 |
| `CONTEXTE_WD12.md` (hors dépôt) | **Périmé** : refaire la liste « reste à faire » |

## 6. Ce qui reste à faire

| Priorité | Sujet | Qui | Effort |
|---|---|---|---|
| 1 | Pousser les modifications du 16/11 (rien n'est commité) | Paul | faible |
| 2 | Filtrer les remontées par auteur dans l'API (I1, RG-38) | backend | faible |
| 3 | Vérifier la salle dans la supervision côté API (C2, RG-34) | backend | moyen |
| 4 | Désactivation des comptes et alignement schéma SQL/JPA (I7, RG-40) | backend | faible |
| 5 | Ne publier que les ports 80 et 443, changer les mots de passe de la VM, figer le tag déployé (I9, I10) | déploiement | faible |
| 6 | Redessiner le diagramme des cas d'utilisation | analyse | moyen |
| 6 bis | Corriger le DAT (§ 3.7) et rédiger « Rapprochement au réel » | analyse | moyen |
| 7 | Compléter le README de l'agent et `.env.example` | agent, déploiement | faible |
| 8 | Mettre les tests navigateur dans le dépôt et dans la CI | front | moyen |
| 9 | Appliquer l'audit du cadre front, mettre à jour le contexte local | Paul | faible |
| 10 | Kit de déploiement des agents : transmettre le secret partagé | Paul | faible |

À trancher avec le client ou en équipe : RG-23 (pop-up ou compteur), RG-27 (liste noire par salle ou
globale), fusion vers `main` avant la soutenance.

## 7. Fichiers

### Dans le dépôt (46 fichiers modifiés, non commités)

- Documents : `Readme.md`, `materiel-frontend/README.md`, `REVUE_DE_CODE.md`.
- Point 4 (comportement) : `shared/composables/use-teaching-room.ts`,
  `features/rooms/views/room-view.vue`, `features/rooms/views/room-detail-view.vue`,
  `features/buildings/views/building-view.vue`, `features/reports/components/report-form.vue`,
  `features/reports/views/report-create-view.vue`, `features/home/views/home-view.vue`,
  `features/materials/views/material-view.vue`.
- Point 3 (nettoyage) : `shared/components/demo-banner.vue` (supprimé), `style.css`,
  `features/reports/views/report-document-view.vue`.
- Point 2 (commentaires seulement) : les 32 autres fichiers du front.

### Hors dépôt (dossier de travail de la session)

| Fichier | Contenu |
|---|---|
| `REVUE_GLOBALE_WD12_G4.md` | Ce document |
| `DCU_MODIFICATIONS_WD12_G4.md` | Ce qu'il faut changer dans le diagramme des cas d'utilisation |
| `Suivi_RG_WD12_G4.xlsx`, `Suivi_RG_WD12_G4.ods` | Suivi des RG, version 4 |
| `Suivi_RG_WD12_G4.v3_2026-11-13.xlsx`, `.ods` | Version précédente, conservée |
| `xlsx/changes_v4.py`, `xlsx/build_v4.py` | Scripts qui reconstruisent le suivi |
| `e2e/trainer-scope.js`, `e2e/mock-api.js` | Test navigateur du point 4 et fausse API (complétée) |
