/**
 * Liste d'options au clavier (rôle ARIA listbox) : option active, flèches, Début et Fin,
 * recherche par les premières lettres. Le focus reste sur le bouton de la liste déroulante ;
 * l'option active lui est signalée par aria-activedescendant.
 * Utilisé par : shared/components/app-select.vue
 */
import {nextTick, ref, type Ref} from 'vue';

/** Option d'une liste déroulante. */
export interface SelectOption<T> {
    value: T;
    label: string;
    /** Intitulé du groupe (ex. « Bâtiment 101 »), affiché au-dessus de la première option du groupe. */
    group?: string;
    disabled?: boolean;
}

/** Ligne affichée dans la liste : un intitulé de groupe, ou une option. */
export interface ListRow {
    key: string;
    isGroup: boolean;
    label: string;
    /** Index de l'option dans le tableau d'options (-1 pour un intitulé de groupe). */
    index: number;
    disabled: boolean;
}

/** Délai au-delà duquel une nouvelle lettre recommence la recherche. */
const TYPEAHEAD_RESET_MS = 600;

/** État partagé par les fonctions de navigation. */
interface ListboxState<T> {
    options: Readonly<Ref<SelectOption<T>[]>>;
    activeIndex: Ref<number>;
    optionId: (index: number) => string;
    typed: {text: string; at: number};
}

/**
 * Construit les lignes de la liste : un intitulé avant la première option de chaque groupe.
 * @param options options de la liste
 */
export function listRows<T>(options: SelectOption<T>[]): ListRow[] {
    const rows: ListRow[] = [];
    let currentGroup: string | undefined;
    options.forEach((option, index) => {
        if (option.group && option.group !== currentGroup) {
            rows.push({key: `groupe-${index}`, isGroup: true, label: option.group, index: -1, disabled: true});
        }
        currentGroup = option.group;
        rows.push({key: `option-${index}`, isGroup: false, label: option.label, index, disabled: option.disabled === true});
    });
    return rows;
}

/** Index de la prochaine option activable à partir de « from », dans le sens donné (-1 si aucune). */
function nextEnabled<T>(options: SelectOption<T>[], from: number, step: 1 | -1): number {
    for (let index = from + step; index >= 0 && index < options.length; index += step) {
        const option = options[index];
        if (option && !option.disabled) return index;
    }
    return -1;
}

/** Rend une option active et la fait défiler dans la liste si elle est cachée. */
function setActive<T>(state: ListboxState<T>, index: number): void {
    if (index < 0) return;
    state.activeIndex.value = index;
    void nextTick(() => document.getElementById(state.optionId(index))?.scrollIntoView({block: 'nearest'}));
}

/** Active la première option dont le libellé commence par les lettres tapées. */
function typeahead<T>(state: ListboxState<T>, key: string): void {
    const now = Date.now();
    state.typed.text = now - state.typed.at > TYPEAHEAD_RESET_MS ? key : state.typed.text + key;
    state.typed.at = now;
    const search = state.typed.text.toLowerCase();
    setActive(state, state.options.value.findIndex(option => !option.disabled && option.label.toLowerCase().startsWith(search)));
}

/**
 * Crée la navigation d'une liste d'options.
 * @param options options de la liste
 * @param optionId id HTML d'une option à partir de son index
 */
export function useListbox<T>(options: Readonly<Ref<SelectOption<T>[]>>, optionId: (index: number) => string) {
    const state: ListboxState<T> = {options, activeIndex: ref(-1), optionId, typed: {text: '', at: 0}};
    return {
        activeIndex: state.activeIndex,
        setActive: (index: number) => setActive(state, index),
        move: (step: 1 | -1) => setActive(state, nextEnabled(options.value, state.activeIndex.value, step)),
        first: () => setActive(state, nextEnabled(options.value, -1, 1)),
        last: () => setActive(state, nextEnabled(options.value, options.value.length, -1)),
        typeahead: (key: string) => typeahead(state, key),
    };
}
