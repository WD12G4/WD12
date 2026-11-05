#!/bin/sh
# ------------------------------------------------------------------------------
# Active le HTTPS de la PWA terrain si les certificats sont montés dans /etc/nginx/certs
# (créés sur le Mac par materiel-terrain/scripts/generate-certs.sh).
# Sans certificats, le site reste servi en HTTP seul : le conteneur démarre toujours.
# Lancé par l'entrypoint de l'image nginx avant le démarrage (dossier /docker-entrypoint.d/).
# ------------------------------------------------------------------------------
set -eu

CERTS=/etc/nginx/certs

if [ -f "$CERTS/server.crt" ] && [ -f "$CERTS/server.key" ]; then
  cp /etc/nginx/recon/nginx-https.conf /etc/nginx/conf.d/https.conf
  echo "RECON : HTTPS activé (port 443, certificat $CERTS/server.crt)"
else
  rm -f /etc/nginx/conf.d/https.conf
  echo "RECON : pas de certificat dans $CERTS, HTTPS désactivé (HTTP seul)"
fi
