/**
 * État de la liaison : réseau du téléphone (navigator.onLine) et joignabilité de l'API.
 * navigator.onLine reste vrai sur un Wi-Fi sans accès au serveur : apiReachable est mis à jour
 * par chaque appel (succès = joignable, absence de réponse = injoignable).
 * Utilisé par : status-bar.vue, features/inventory
 */
import {computed, ref} from 'vue';

//------------------ DATAS -----------------------------------
const deviceOnline = ref(navigator.onLine);
const apiReachable = ref(true);

window.addEventListener('online', () => {
    deviceOnline.value = true;
});
window.addEventListener('offline', () => {
    deviceOnline.value = false;
});

//------------------ COMPUTED --------------------------------
/** Vrai si l'API peut a priori être jointe : sinon les actions sont mises en attente. */
const online = computed(() => deviceOnline.value && apiReachable.value);

//------------------ FUNCTIONS -------------------------------
/**
 * Note le résultat du dernier appel à l'API.
 * @param reachable vrai si l'API a répondu (même en erreur), faux si elle n'a pas pu être jointe
 */
function markReachable(reachable: boolean): void {
    apiReachable.value = reachable;
}

/**
 * Accès à l'état de la liaison.
 * @returns online, deviceOnline, et markReachable()
 */
export function useNetwork() {
    return {online, deviceOnline, markReachable};
}
