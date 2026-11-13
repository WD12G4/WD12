#!/bin/sh
set -e

CERT_DIR="/etc/nginx/certs"
mkdir -p "$CERT_DIR"

# Si aucun certificat n'est fourni ou monté en volume, on génère un certificat auto-signé
if [ ! -f "$CERT_DIR/server.crt" ] || [ ! -f "$CERT_DIR/server.key" ]; then
    echo "=========================================================="
    echo "[HTTPS] Aucun certificat trouvé dans $CERT_DIR"
    echo "[HTTPS] Génération d'un certificat SSL auto-signé valide 10 ans..."
    echo "=========================================================="

    openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
        -keyout "$CERT_DIR/server.key" \
        -out "$CERT_DIR/server.crt" \
        -subj "/C=FR/ST=France/L=Rennes/O=WD12/OU=Supervision/CN=localhost" 2>/dev/null || \
    openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
        -keyout "$CERT_DIR/server.key" \
        -out "$CERT_DIR/server.crt" \
        -subj "/C=FR/ST=France/L=Rennes/O=WD12/OU=Supervision/CN=localhost"

    chmod 600 "$CERT_DIR/server.key"
    chmod 644 "$CERT_DIR/server.crt"
    echo "[HTTPS] Certificat SSL auto-signé généré avec succès dans $CERT_DIR."
fi

exec "$@"
