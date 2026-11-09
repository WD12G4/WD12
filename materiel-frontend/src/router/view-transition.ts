/**
 * Transitions entre les pages avec l'API View Transitions (Safari 18 et plus, Chrome 111 et plus).
 * - Toute navigation : le contenu de la page passe en fondu, la barre latérale ne bouge pas.
 * - Tableau de bord ↔ fiche d'une salle : la tuile s'agrandit jusqu'à la carte de la fiche et
 *   son titre glisse jusqu'à celui de la page ; au retour, l'inverse. Seules la tuile et la carte
 *   concernées portent la classe vt-target, donc un nom de transition (style.css).
 * Navigateur sans l'API : App.vue garde le fondu de Vue (<Transition>). Les deux ne sont jamais
 * mélangés : pendant une View Transition, le navigateur suspend requestAnimationFrame, dont
 * <Transition> a besoin. Moins d'animations demandées : changement de page sans animation.
 * Utilisé par : router/index.ts, App.vue, room-overview.vue, room-detail-view.vue
 */
import {nextTick, ref} from 'vue';
import type {RouteLocationNormalized, Router} from 'vue-router';
import {prefersReducedMotion} from '../shared/composables/use-motion.ts';

/** Vrai si le navigateur connaît l'API (décidé une fois, au chargement). */
export const viewTransitionsSupported = typeof document.startViewTransition === 'function';

/** Salle dont la tuile et la fiche sont reliées pendant la transition, ou null. */
export const sharedRoomId = ref<number | null>(null);

/** Attente maximale de la page d'arrivée (le temps que ses données arrivent), avant un simple fondu. */
const WAIT_MAX_MS = 500;
/** Intervalle d'attente : setTimeout, car requestAnimationFrame est suspendu pendant la transition. */
const POLL_MS = 16;

/** Tableau de bord → fiche d'une salle, ou l'inverse : la salle et l'élément à attendre à l'arrivée. */
function sharedNavigation(to: RouteLocationNormalized, from: RouteLocationNormalized): {roomId: number; target: string} | null {
    if (from.name === 'home' && to.name === 'room-detail') return {roomId: Number(to.params.id), target: '.room-hero'};
    if (from.name === 'room-detail' && to.name === 'home') return {roomId: Number(from.params.id), target: '.overview-tile.vt-target'};
    return null;
}

/** Attend qu'un élément apparaisse dans la page, ou que le délai maximal soit écoulé. */
function waitForElement(selector: string): Promise<void> {
    const start = performance.now();
    return new Promise(resolve => {
        const poll = () => {
            if (document.querySelector(selector) || performance.now() - start > WAIT_MAX_MS) resolve();
            else setTimeout(poll, POLL_MS);
        };
        poll();
    });
}

/** Promesse résolue quand la navigation en cours est terminée (réussie ou non). */
function nextNavigation(router: Router): Promise<void> {
    return new Promise(resolve => {
        const stop = router.afterEach(() => {
            stop();
            resolve();
        });
    });
}

/**
 * Lance la transition : l'état de départ est capturé, puis la navigation reprend (resolve) et le
 * navigateur attend la nouvelle page (et, pour une salle, sa carte) avant d'animer.
 */
function runTransition(router: Router, target: string | null): Promise<void> {
    return new Promise(resolve => {
        const navigated = nextNavigation(router);
        const transition = document.startViewTransition(async () => {
            resolve();
            await navigated;
            await nextTick();
            if (target) await waitForElement(target);
        });
        void transition.finished.finally(() => { sharedRoomId.value = null; });
    });
}

/**
 * Branche les transitions sur le routeur (pas au premier affichage de l'application).
 * @param router routeur de l'application
 */
export function installViewTransitions(router: Router): void {
    router.beforeResolve(async (to, from) => {
        if (!viewTransitionsSupported || prefersReducedMotion() || from.matched.length === 0) return;
        const shared = sharedNavigation(to, from);
        sharedRoomId.value = shared?.roomId ?? null;
        await nextTick(); // la tuile reçoit son nom de transition avant la capture de l'état de départ
        await runTransition(router, shared?.target ?? null);
    });
}
