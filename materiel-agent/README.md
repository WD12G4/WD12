# Agent de Supervision WD12 (.NET 10 C#)

L'agent client est une application console légère écrite en **C# (.NET 10)**, 100% multiplateforme (**Windows**, **Linux**, **macOS**).
Il est destiné à être installé et exécuté sur chaque poste étudiant (unité centrale - UC).

---

## 1. Multiplateforme (Windows, Linux, macOS)

L'agent utilise uniquement des APIs standard `.NET 10` portables :
- **Réseau & WebSocket** : `System.Net.WebSockets.ClientWebSocket` et `System.Net.NetworkInformation` (détection multiplateforme de la vraie adresse MAC hors adaptateurs virtuels).
- **Processus & Sécurité** : `System.Diagnostics.Process` :
  - **Windows** : filtrage des services/processus critiques (`explorer.exe`, `svchost.exe`, etc.).
  - **Linux** : filtrage des processus démons/système via `/proc/{pid}/status` (UID < 1000) et liste blanche/noire (`systemd`, `dbus`, etc.).
  - **macOS** : détection des applications dans `.app/Contents/MacOS/`, exclusion des helpers internes et démons Apple (`/System/Library/`, `launchd`, etc.).

---

## 2. Configuration (`config.json`)

Un fichier `config.json` peut être placé à côté du binaire :
```json
{
  "server_url": "ws://127.0.0.1:8080/ws/agent",
  "serial_number": "UC-DELL-001",
  "reconnect_delay_sec": 5
}
```

### Priorité de prise en compte :
1. Arguments en ligne de commande (`--server`, `--serial`, `--reconnect`, `--config`)
2. Variables d'environnement (`WD12_SERVER_URL`, `WD12_SERIAL_NUMBER`)
3. Fichier `config.json`
4. Valeurs par défaut (adresse MAC sans séparateurs comme numéro de série automatique)

---

## 3. Compilation et Publication

### Prérequis
- [.NET 10 SDK](https://dotnet.microsoft.com/download/dotnet/10.0) installé sur votre machine.

### Compilation classique
```bash
dotnet build
```

### Publication en exécutable autonome sans dépendances (Self-Contained)

Grâce à .NET 10, vous pouvez compiler depuis une seule machine vers n'importe quelle plateforme cible :

#### 1. Windows x64 (fichier `.exe` unique) :
```bash
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -o ./dist/windows
```

#### 2. Linux x64 (binaire ELF unique) :
```bash
dotnet publish -c Release -r linux-x64 --self-contained true -p:PublishSingleFile=true -o ./dist/linux
```

#### 3. Linux ARM64 (Raspberry Pi, mini-PC ARM) :
```bash
dotnet publish -c Release -r linux-arm64 --self-contained true -p:PublishSingleFile=true -o ./dist/linux-arm64
```

#### 4. macOS Apple Silicon (Mac M1/M2/M3/M4) :
```bash
dotnet publish -c Release -r osx-arm64 --self-contained true -p:PublishSingleFile=true -o ./dist/osx-arm64
```

#### 5. macOS Intel :
```bash
dotnet publish -c Release -r osx-x64 --self-contained true -p:PublishSingleFile=true -o ./dist/osx-x64
```

---

## 4. Lancement

```bash
# Exemple Windows
MaterielAgent.exe --server ws://192.168.1.50:8080/ws/agent --serial UC-DELL-001

# Exemple Linux / macOS
chmod +x MaterielAgent
./MaterielAgent --server ws://192.168.1.50:8080/ws/agent --serial UC-DELL-001
```
