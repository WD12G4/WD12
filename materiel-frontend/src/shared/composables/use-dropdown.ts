/**
 * Panneau déroulant attaché à un bouton : position, ouverture, fermeture au clic à l'extérieur.
 * Le panneau est rendu dans <body> (Teleport) en position fixe : les cartes ont overflow et
 * container-type, qui couperaient un panneau placé à l'intérieur. Il s'ouvre sous le bouton,
 * ou au-dessus s'il manque de place, et suit le bouton quand la page défile.
 * Utilisé par : shared/components/app-select.vue
 */
import {type CSSProperties, onBeforeUnmount, ref, type Ref} from 'vue';

/** Espace entre le bouton et le panneau. */
const GAP_PX = 6;
/** Hauteur maximale du panneau (18rem dans style.css), pour choisir entre dessous et dessus. */
const PANEL_MAX_HEIGHT_PX = 288;

/**
 * Calcule la position fixe du panneau d'après celle du bouton.
 * @param trigger bouton de la liste déroulante
 */
function panelPosition(trigger: HTMLElement): CSSProperties {
    const rect = trigger.getBoundingClientRect();
    const spaceBelow = window.innerHeight - rect.bottom;
    const openUp = spaceBelow < PANEL_MAX_HEIGHT_PX && rect.top > spaceBelow;
    const vertical = openUp
        ? {bottom: `${window.innerHeight - rect.top + GAP_PX}px`}
        : {top: `${rect.bottom + GAP_PX}px`};
    return {...vertical, left: `${rect.left}px`, width: `${rect.width}px`};
}

/** Vrai si l'élément cliqué est hors du bouton et hors du panneau. */
function isOutside(event: Event, trigger: HTMLElement | null, panel: HTMLElement | null): boolean {
    const target = event.target as Node;
    return !trigger?.contains(target) && !panel?.contains(target);
}

/**
 * Crée l'état d'un panneau déroulant.
 * @param trigger référence du bouton
 * @param panel référence du panneau
 * @returns isOpen, position (style du panneau), open, close
 */
export function useDropdown(trigger: Readonly<Ref<HTMLElement | null>>, panel: Readonly<Ref<HTMLElement | null>>) {
    const isOpen = ref(false);
    const position = ref<CSSProperties>({});
    const reposition = () => { if (trigger.value) position.value = panelPosition(trigger.value); };
    const onPointerDown = (event: Event) => { if (isOutside(event, trigger.value, panel.value)) close(); };

    function open(): void {
        reposition();
        isOpen.value = true;
        window.addEventListener('scroll', reposition, true);
        window.addEventListener('resize', reposition);
        document.addEventListener('pointerdown', onPointerDown, true);
    }

    function close(): void {
        isOpen.value = false;
        window.removeEventListener('scroll', reposition, true);
        window.removeEventListener('resize', reposition);
        document.removeEventListener('pointerdown', onPointerDown, true);
    }

    onBeforeUnmount(close);
    return {isOpen, position, open, close};
}
