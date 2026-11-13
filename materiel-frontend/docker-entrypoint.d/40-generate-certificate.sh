#!/bin/sh
# ------------------------------------------------------------------------------
# Certificat HTTPS de Nginx (nginx.conf, port 443), lancé par l'image nginx avant le démarrage.
# S'il manque, un certificat auto-signé est créé pour l'adresse de APP_URL (IP ou nom) :
# le navigateur l'accepte après une confirmation (« Accepter le risque et poursuivre »).
# Il est gardé dans le volume frontend_certs : la confirmation n'est à faire qu'une fois.
# Pour un certificat signé par une autorité (ex. de l'école), déposer server.crt et
# server.key dans le volume : ils sont alors utilisés tels quels.
# ------------------------------------------------------------------------------
set -eu

CERT_DIR=/etc/nginx/certs
CRT="$CERT_DIR/server.crt"
KEY="$CERT_DIR/server.key"

# Hôte de APP_URL : https://172.16.131.149/... -> 172.16.131.149
HOST=$(echo "${APP_URL:-https://localhost}" | sed -e 's~^[a-z]*://~~' -e 's~[/:].*$~~')
SAN="DNS:localhost,IP:127.0.0.1"
if echo "$HOST" | grep -Eq '^[0-9]+(\.[0-9]+){3}$'; then
    SAN="$SAN,IP:$HOST"
elif [ "$HOST" != "localhost" ]; then
    SAN="$SAN,DNS:$HOST"
fi

mkdir -p "$CERT_DIR"
if [ -s "$CRT" ] && [ -s "$KEY" ]; then
    # Certificat déposé à la main (pas créé par ce script) : on le garde tel quel.
    if ! openssl x509 -in "$CRT" -noout -subject | grep -q "Gestion du parc"; then
        echo "Certificat HTTPS fourni : utilisé tel quel."
        exit 0
    fi
    # Certificat auto-signé déjà créé pour cet hôte : on le garde (le navigateur l'a peut-être accepté).
    if openssl x509 -in "$CRT" -noout -text | grep -Fq "$HOST"; then
        echo "Certificat HTTPS auto-signé présent pour $HOST."
        exit 0
    fi
fi

echo "Création d'un certificat HTTPS auto-signé pour $HOST ($SAN)."
openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
    -keyout "$KEY" -out "$CRT" \
    -subj "/CN=$HOST/O=Gestion du parc" \
    -addext "subjectAltName=$SAN" \
    -addext "extendedKeyUsage=serverAuth" 2>/dev/null
chmod 600 "$KEY"
