#!/bin/sh
# ------------------------------------------------------------------------------
# Certificats HTTPS pour servir la PWA depuis le Mac vers le téléphone.
#
# La caméra (getUserMedia) et le service worker exigent HTTPS dès qu'on quitte localhost.
# Ce script crée, avec openssl uniquement :
#   1. une autorité locale « RECON Dev CA » (certs/recon-ca.crt), à installer UNE fois sur l'iPhone ;
#   2. un certificat serveur signé par elle, valable pour <nom-du-mac>.local, localhost et les IP passées.
#
# Usage : sh scripts/generate-certs.sh               (nom Bonjour du Mac + localhost, sans Node)
#         sh scripts/generate-certs.sh 192.168.1.20  (ajoute une ou plusieurs IP)
# Les certificats servent à docker-compose.projet-terrain.yml (port 443) et à npm run dev (port 5174).
# Relancer le script ne recrée pas l'autorité : l'iPhone n'a pas à la réinstaller.
# ------------------------------------------------------------------------------
set -eu

DIR="$(cd "$(dirname "$0")/.." && pwd)/certs"
HOST="$(scutil --get LocalHostName 2>/dev/null || hostname -s).local"
mkdir -p "$DIR"
cd "$DIR"

# 1. Autorité locale (gardée si elle existe déjà)
if [ ! -f recon-ca.key ]; then
  openssl genrsa -out recon-ca.key 2048 2>/dev/null
  openssl req -x509 -new -key recon-ca.key -sha256 -days 825 -subj "/CN=RECON Dev CA" \
    -addext "basicConstraints=critical,CA:TRUE" -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -out recon-ca.crt
  echo "Autorité créée : $DIR/recon-ca.crt"
fi

# 2. Noms et adresses couverts par le certificat serveur
SAN="DNS:$HOST,DNS:localhost,IP:127.0.0.1,IP:::1"
for ip in "$@"; do SAN="$SAN,IP:$ip"; done

cat > server.ext <<EXT
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=$SAN
EXT

# iOS refuse un certificat serveur valable plus de 825 jours : 397 jours suffisent.
openssl req -new -newkey rsa:2048 -nodes -keyout server.key -subj "/CN=$HOST" -out server.csr 2>/dev/null
openssl x509 -req -in server.csr -CA recon-ca.crt -CAkey recon-ca.key -CAcreateserial \
  -days 397 -sha256 -extfile server.ext -out server.crt 2>/dev/null
rm -f server.csr server.ext

echo "Certificat serveur : $DIR/server.crt ($SAN)"
echo "Adresse à ouvrir sur le téléphone : https://$HOST (docker compose) ou https://$HOST:5174 (npm run dev)"
