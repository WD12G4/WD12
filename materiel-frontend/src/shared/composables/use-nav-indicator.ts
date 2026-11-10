/**
 * Pastille qui glisse d'un lien à l'autre dans la barre du haut (thème « Verre ») : mesure le lien
 * de la rubrique courante (a.current) et écrit sa position dans --pill-x et --pill-w sur le menu ;
 * la transition CSS fait glisser la pastille (style.css, .nav-pill). Sans effet visible dans le thème
 * classique, où la pastille est masquée.
 * Utilisé par : app-nav.vue
 */
import {nextTick, onBeforeUnmount, onMounted, type ShallowRef, watch} from 'vue';
import {useRoute} from 'vue-router';
import {useTheme} from './use-theme.ts';

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
 * Place la pastille sous le lien courant, et la replace à chaque changement de page, de thème ou de taille.
 * Changement de thème : la mise en page du menu change, la pastille réapparaît sur place.
 * @param nav élément <nav> qui contient les liens et la pastille
 */
export function useNavIndicator(nav: Readonly<ShallowRef<HTMLElement | null>>): void {
    const route = useRoute();
    const {theme} = useTheme();
    const place = (): void => placePill(nav.value);

    watch(() => route.name, () => void nextTick(place));
    watch(theme, () => {
        nav.value?.classList.remove('has-pill');
        void nextTick(place);
    });
    onMounted(() => {
        place();
        window.addEventListener('resize', place);
    });
    onBeforeUnmount(() => window.removeEventListener('resize', place));
}
