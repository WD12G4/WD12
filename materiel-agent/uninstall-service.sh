#!/usr/bin/env bash
# ========================================================
# Arrêt et désinstallation du service Agent WD12 (Linux)
# ========================================================

set -e

if [ "$EUID" -ne 0 ]; then
    echo "[ERREUR] Ce script doit être exécuté en tant que root (sudo)."
    exit 1
fi

echo "[1/2] Arrêt du service..."
systemctl stop materiel-agent 2>/dev/null || true
systemctl disable materiel-agent 2>/dev/null || true

echo "[2/2] Suppression du fichier systemd..."
rm -f /etc/systemd/system/materiel-agent.service
systemctl daemon-reload

echo "========================================================"
echo "  Agent WD12 arrêté et désinstallé avec succès !"
echo "========================================================"
