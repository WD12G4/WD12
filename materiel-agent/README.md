# Agent de Supervision WD12 (Windows & Linux)

Agent autonome et léger développé en **Go** pour superviser les postes clients (PC stagiaires) depuis l'application WD12.

Il implémente les règles de gestion :
* **RG-25 (Processus actifs)** : Remonte la liste des processus lancés par l'utilisateur du poste (hors processus système).
* **RG-26 (Arrêt en un clic & KillEvent)** : Permet d'arrêter un processus distant à la demande du formateur, confirme l'arrêt effectif et permet l'enregistrement d'un `KillEvent` (réussi ou échoué) en base de données.
* **Sécurité stricte** : Refus absolu d'arrêter les processus système indispensables à l'intégrité de la machine (PIDs critiques, démons racine / SYSTEM, liste noire de sécurité).

---

## 1. Prérequis et Compilation

L'agent est écrit en Go 1.21+. Il se compile en un **binaire unique autonome** d'environ 9 Mo, sans aucune dépendance ni runtime externe requis sur les PC cibles.

### Télécharger les dépendances Go (une seule fois sur la machine de dev) :
```bash
cd materiel-agent
go mod tidy
```

### A. Compiler pour Windows (`agent-supervision.exe`)
Depuis n'importe quelle machine (Linux, macOS ou Windows) :
```bash
# Compilation optimisée (retrait des symboles de debug pour réduire la taille)
GOOS=windows GOARCH=amd64 go build -ldflags="-s -w" -o agent-supervision.exe .
```

### B. Compiler pour Linux (`agent-supervision`)
Depuis n'importe quelle machine :
```bash
GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o agent-supervision .
```

---

## 2. Configuration de l'Agent

L'agent recherche sa configuration selon cet ordre de priorité (le premier trouvé l'emporte) :
1. **Arguments en ligne de commande** (`-server`, `-serial`, `-config`, `-reconnect`).
2. **Variables d'environnement** (`WD12_SERVER_URL`, `WD12_SERIAL_NUMBER`).
3. **Fichier JSON** (`config.json` placé à côté de l'exécutable).
4. **Valeurs par défaut** (`ws://127.0.0.1:8080/ws/agent`, `UC-<HOSTNAME>`).

### Exemple de fichier `config.json` :
```json
{
  "server_url": "ws://192.168.1.100:8080/ws/agent",
  "serial_number": "UC-SALLE101-POSTE01",
  "reconnect_delay_sec": 5
}
```

> **Important :** `serial_number` doit correspondre exactement au numéro de série de l'unité centrale renseigné dans l'inventaire WD12 (`Material.serialNumber`).

---

## 3. Installation sur Windows

### Méthode 1 : En tant que Service Windows avec NSSM (Recommandé)

NSSM (*Non-Sucking Service Manager*) est l'outil standard pour exécuter un binaire en service d'arrière-plan avec relance automatique en cas de crash ou redémarrage du PC.

1. Créez un dossier d'installation, par exemple : `C:\WD12\Agent\`
2. Copiez-y `agent-supervision.exe` et votre fichier `config.json`.
3. Ouvrez un terminal **PowerShell en tant qu'Administrateur** :

```powershell
# Installer le service
nssm install WD12Agent "C:\WD12\Agent\agent-supervision.exe"
nssm set WD12Agent AppDirectory "C:\WD12\Agent"
nssm set WD12Agent Start SERVICE_AUTO_START
nssm set WD12Agent DisplayName "WD12 Supervision Agent"
nssm set WD12Agent Description "Agent de supervision pour le parc WD12"

# Démarrer le service immédiatement
nssm start WD12Agent
```

Pour vérifier le statut ou arrêter le service :
```powershell
nssm status WD12Agent
nssm stop WD12Agent
```

### Méthode 2 : En tant que tâche planifiée Windows (`schtasks`)
Sans outil tiers, exécuté avec les privilèges les plus élevés au démarrage du système :

```powershell
schtasks /create /tn "WD12Agent" /tr "C:\WD12\Agent\agent-supervision.exe" /sc onstart /ru "SYSTEM" /rl highest
schtasks /run /tn "WD12Agent"
```

### Méthode 3 : Mode Test / Console
Pour tester manuellement et voir les logs en direct :
```cmd
agent-supervision.exe -server ws://192.168.1.100:8080/ws/agent -serial UC-SALLE101-POSTE01
```

---

## 4. Installation sur Linux (Debian, Ubuntu, RedHat, etc.)

### Étape 1 : Copie du binaire
1. Copiez l'exécutable compilé sur le poste cible :
```bash
sudo mkdir -p /opt/wd12/
sudo cp agent-supervision /opt/wd12/
sudo chmod +x /opt/wd12/agent-supervision
```

2. Créez la configuration dans `/opt/wd12/config.json` :
```json
{
  "server_url": "ws://192.168.1.100:8080/ws/agent",
  "serial_number": "UC-SALLE101-POSTE02",
  "reconnect_delay_sec": 5
}
```

### Étape 2 : Création du service `systemd`
Créez le fichier `/etc/systemd/system/wd12-agent.service` :

```ini
[Unit]
Description=Agent de supervision WD12
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/wd12
ExecStart=/opt/wd12/agent-supervision
Restart=always
RestartSec=5
# Exécution en root pour avoir les droits d'arrêter les processus utilisateurs
User=root

[Install]
WantedBy=multi-user.target
```

### Étape 3 : Activation et démarrage du service
```bash
# Recharger la configuration systemd
sudo systemctl daemon-reload

# Activer au démarrage de la machine et lancer immédiatement
sudo systemctl enable --now wd12-agent

# Vérifier le statut
sudo systemctl status wd12-agent
```

### Étape 4 : Consultation des logs en direct
```bash
sudo journalctl -u wd12-agent -f
```

---

## 5. Protocole WebSocket et Enregistrement des KillEvents

### Communication bidirectionnelle sortante
L'agent initie une connexion WebSocket sortante (`ws://` ou `wss://`) vers l'endpoint `/ws/agent` du backend WD12.
* **Aucun port ouvert en écoute sur le poste client** : compatible avec les pare-feux stricts et le NAT.
* Reconnexion automatique infinie en cas de perte réseau ou redémarrage du backend.

### Format des échanges WebSocket

#### 1. Enregistrement initial (Agent -> Serveur)
Dès l'ouverture de la connexion, l'agent s'enregistre auprès du backend avec ses métadonnées :
```json
{
  "type": "REGISTER",
  "serialNumber": "UC-SALLE101-POSTE01",
  "hostname": "PC-STAGIAIRE-01",
  "macAddress": "00:1A:2B:3C:4D:5E",
  "os": "windows"
}
```

#### 2. Demande des processus actifs (Serveur -> Agent) & Réponse (RG-25)
* **Serveur -> Agent :**
```json
{
  "action": "GET_PROCESSES",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```
* **Agent -> Serveur :**
```json
{
  "type": "PROCESS_LIST",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "serialNumber": "UC-SALLE101-POSTE01",
  "processes": [
    { "pid": 4512, "name": "firefox.exe", "startedAt": 1716301234567 },
    { "pid": 8920, "name": "notepad.exe", "startedAt": 1716302000000 }
  ]
}
```

#### 3. Demande d'arrêt d'un processus (Serveur -> Agent) & Confirmation (RG-26)
* **Serveur -> Agent :**
```json
{
  "action": "KILL_PROCESS",
  "requestId": "9c8a7b6a-4d3e-2f1a-0b9c-8d7e6f5a4b3c",
  "pid": 4512
}
```

* **Agent -> Serveur (Cas réussi : processus bien arrêté et confirmé) :**
```json
{
  "type": "KILL_RESULT",
  "requestId": "9c8a7b6a-4d3e-2f1a-0b9c-8d7e6f5a4b3c",
  "serialNumber": "UC-SALLE101-POSTE01",
  "result": {
    "success": true,
    "pid": 4512,
    "processName": "firefox.exe"
  }
}
```

* **Agent -> Serveur (Cas refusé ou échoué) :**
```json
{
  "type": "KILL_RESULT",
  "requestId": "9c8a7b6a-4d3e-2f1a-0b9c-8d7e6f5a4b3c",
  "serialNumber": "UC-SALLE101-POSTE01",
  "result": {
    "success": false,
    "pid": 1,
    "processName": "systemd",
    "error": "Action refusée : ce processus est un composant système ou protégé"
  }
}
```

### Vérification active de la terminaison et enregistrement en base

1. **Vérification active de l'arrêt côté agent** :
   * L'agent envoie d'abord une demande d'arrêt gracieux (`Terminate`).
   * Il scrute activement la disparition du PID dans les tables de processus du système d'exploitation (`waitForProcessExit`).
   * Si le processus ne s'arrête pas au terme du délai de grâce (1,5 s), l'agent bascule automatiquement sur un arrêt forcé (`Kill`).
   * L'agent confirme ensuite formellement que le processus n'est plus actif avant de renvoyer `success: true`.

2. **Enregistrement systématique du `KillEvent` en BDD** :
   * Dès réception de la confirmation (`KILL_RESULT`) ou en cas d'échec de communication, le backend Spring Boot enregistre un enregistrement dans l'entité `KillEvent` avec son booléen de résultat :
     * `result = true` : le processus a été correctement arrêté et sa disparition a été vérifiée par l'agent.
     * `result = false` : la tentative d'arrêt a échoué (processus protégé, refusé, timeout ou agent indisponible).
   * Cet enregistrement garantit une traçabilité complète de toutes les demandes d'arrêt formulées par les formateurs.

---

## 6. Règles de Sécurité et Processus Protégés

L'agent n'affiche et ne permet de tuer **aucun processus critique** :
1. **Sous Windows :**
   * PIDs $\le 4$ et PID de l'agent lui-même.
   * Processus appartenant aux comptes `NT AUTHORITY\SYSTEM`, `LOCAL SERVICE`, `NETWORK SERVICE`.
   * Liste noire de sécurité : `csrss.exe`, `lsass.exe`, `services.exe`, `smss.exe`, `wininit.exe`, `winlogon.exe`, `svchost.exe`, `explorer.exe`, `dwm.exe`, `spoolsv.exe`, etc.
2. **Sous Linux :**
   * PIDs $\le 2$ (PID 1 `systemd`/`init`, PID 2 `kthreadd`).
   * Tous les processus ayant un **UID < 1000** (services système, démons racine).
   * Processus essentiels : `systemd`, `init`, `sshd`, `dbus-daemon`, `NetworkManager`, `Xorg`, `wayland`, `gnome-shell`, etc.

Toute commande de fermeture reçue du serveur est **re-validée côté agent** avant toute tentative de terminaison. Si le processus est protégé, l'agent renvoie un `KILL_RESULT` avec `success: false`, ce qui permet au backend d'enregistrer le `KillEvent` à l'état échoué.
