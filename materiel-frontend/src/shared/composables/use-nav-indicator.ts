/**
 * Pastille qui glisse d'un lien à l'autre dans la barre du haut : mesure le lien de la rubrique
 * courante (a.current) et écrit sa position dans --pill-x et --pill-w sur le menu ; la transition CSS
 * fait glisser la pastille (style.css, .nav-pill). Masquée dans le tiroir du menu (mobile).
 * Elle se replace au changement de page, mais aussi dès que les liens bougent sans changer de page :
 * compteur des remontées qui arrive après le chargement, liens qui changent avec le rôle, police
 * chargée, fenêtre redimensionnée (ResizeObserver sur les liens, MutationObserver sur la liste).
 * Utilisé par : app-nav.vue
 */
import {nextTick, onBeforeUnmount, onMounted, type ShallowRef, watch} from 'vue';
import {useRoute} from 'vue-router';

/**
 * Mesure le lien courant ; la pastille apparaît sur place (sans glisser) si elle était cachée.
 * @param nav élément <nav> qui contient les liens et la pastille
 */
function placePill(nav: HTMLElement | null): void {
    if (!nav) return;
    const current = nav.querySelector<HTMLElement>('a.current:not(.primary)');
    const wasVisible = nav.classList.contains('has-pill');
    nav.classList.toggle('has-pill', current !== null && wasVisible);
    if (!current) return;
    nav.style.setProperty('--pill-x', `${current.offsetLeft}px`);
    nav.style.setProperty('--pill-w', `${current.offsetWidth}px`);
    if (wasVisible) return;
    void nav.offsetWidth; // applique la position avant d'afficher, pour ne pas glisser depuis la gauche
    nav.classList.add('has-pill');
}

/**
 * Observe la taille de chaque lien (et du menu) : replace la pastille si l'un d'eux change.
 * @param nav élément <nav>
 * @param observer observateur à (re)brancher sur les liens actuels
 */
function observeLinks(nav: HTMLElement, observer: ResizeObserver): void {
    observer.disconnect();
    observer.observe(nav);
    nav.querySelectorAll('a').forEach(link => observer.observe(link));
}

/**
 * Place la pastille sous le lien courant, et la replace à chaque changement de page, de taille ou
 * de liens.
 * @param nav élément <nav> qui contient les liens et la pastille
 */
export function useNavIndicator(nav: Readonly<ShallowRef<HTMLElement | null>>): void {
    const route = useRoute();
    const place = (): void => placePill(nav.value);
    const resizeObserver = new ResizeObserver(place);
    // Liens ajoutés ou retirés (changement de rôle) : on observe les nouveaux, puis on replace.
    const mutationObserver = new MutationObserver(() => {
        if (nav.value) observeLinks(nav.value, resizeObserver);
        place();
    });

    watch(() => route.name, () => void nextTick(place));
    onMounted(() => {
        place();
        if (!nav.value) return;
        observeLinks(nav.value, resizeObserver);
        mutationObserver.observe(nav.value, {childList: true, subtree: true});
        void document.fonts.ready.then(place);
    });
    onBeforeUnmount(() => {
        resizeObserver.disconnect();
        mutationObserver.disconnect();
    });
}
