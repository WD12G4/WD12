#!/usr/bin/env bash
# ========================================================
# Installation et démarrage de l'Agent WD12 en service systemd (Linux)
# ========================================================

set -e

if [ "$EUID" -ne 0 ]; then
    echo "[ERREUR] Ce script doit être exécuté en tant que root (sudo)."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BIN_PATH="$SCRIPT_DIR/MaterielAgent"

if [ ! -f "$BIN_PATH" ]; then
    echo "[ERREUR] Exécutable introuvable : $BIN_PATH"
    exit 1
fi

chmod +x "$BIN_PATH"

echo "[1/3] Configuration du service systemd..."
cat << EOF > /etc/systemd/system/materiel-agent.service
[Unit]
Description=Agent de Supervision WD12
After=network.target network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=$SCRIPT_DIR
ExecStart=$BIN_PATH
Restart=always
RestartSec=5
User=root

[Install]
WantedBy=multi-user.target
EOF

echo "[2/3] Rechargement de systemd..."
systemctl daemon-reload
systemctl enable materiel-agent

echo "[3/3] Démarrage du service..."
systemctl restart materiel-agent

echo "========================================================"
echo "  Agent WD12 installé et démarré avec succès !"
echo "  Statut : systemctl status materiel-agent"
echo "  Logs   : journalctl -u materiel-agent -f"
echo "========================================================"
