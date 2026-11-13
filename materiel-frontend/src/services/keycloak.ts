/**
 * Client Keycloak : le fournisseur d'identité qui joue MinDef Connect (docker-compose.yml, service keycloak).
 * Connexion par redirection vers la page de Keycloak (code d'autorisation + PKCE), jeton d'accès gardé
 * en mémoire par keycloak-js et renouvelé avant son expiration.
 * Adresse de Keycloak : VITE_KEYCLOAK_URL si elle est fournie au build, sinon l'adresse de l'application
 * suivie de /auth : Keycloak est servi par le Nginx du front (nginx.conf) ou le proxy de Vite. Même
 * origine et même port que l'application : rien à ouvrir de plus, et le proxy de l'école laisse passer.
 * Version de keycloak-js fixée à 25.0.6 (package.json) : à partir de la 26, keycloak-js exige l'API Web Crypto
 * (crypto.subtle), que les navigateurs ne fournissent qu'en HTTPS ou sur localhost. La VM est servie en HTTP :
 * la 25 calcule le PKCE avec js-sha256 et fonctionne aussi en HTTP. Passer en 26 une fois l'application en HTTPS.
 * Utilisé par : shared/composables/use-auth.ts
 */
import Keycloak from 'keycloak-js';

/** Adresse de Keycloak vue par le navigateur (doit être égale à KEYCLOAK_URL du fichier .env). */
const KEYCLOAK_URL: string = import.meta.env.VITE_KEYCLOAK_URL ?? `${window.location.origin}/auth`;

/** Realm et client déclarés dans docker/keycloak/gestion-parc-realm.json. */
export const keycloak = new Keycloak({
    url: KEYCLOAK_URL,
    realm: 'gestion-parc',
    clientId: 'materiel-frontend',
});

/**
 * Adresse complète de l'application pour une page donnée : Keycloak y renvoie après la connexion
 * ou la déconnexion (adresses autorisées dans le realm : redirectUris).
 * @param path chemin de la page (ex. '/materiel')
 */
export function appUrl(path: string): string {
    return `${window.location.origin}${path}`;
}
