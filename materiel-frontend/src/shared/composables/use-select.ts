/**
 * Logique d'une liste déroulante (app-select.vue) : ouvrir, choisir, clavier, souris, et ce
 * qu'affiche le bouton. Assemble use-dropdown (le panneau) et use-listbox (la navigation).
 * Clavier, liste fermée : flèches, Entrée ou Espace l'ouvrent ; une lettre l'ouvre sur l'option
 * qui commence par cette lettre. Liste ouverte : flèches, Début, Fin, Entrée ou Espace pour
 * choisir, Échap ou Tab pour fermer.
 * Utilisé par : shared/components/app-select.vue
 */
import {computed, type ComputedRef, type Ref, useTemplateRef} from 'vue';
import {useDropdown} from './use-dropdown.ts';
import {listRows, type SelectOption, useListbox} from './use-listbox.ts';

/** Paramètres reçus du composant. Les accesseurs suivent les props, qui sont réactives. */
interface SelectSource<T> {
    id: string;
    options: () => SelectOption<T>[];
    placeholder: () => string | undefined;
    disabled: () => boolean;
    model: Ref<T>;
}

/** État partagé par les fonctions ci-dessous. */
interface SelectState<T> extends SelectSource<T> {
    dropdown: ReturnType<typeof useDropdown>;
    listbox: ReturnType<typeof useListbox<T>>;
    selectedIndex: ComputedRef<number>;
    optionId: (index: number) => string;
}

/** Touches qui ouvrent la liste fermée. */
const OPENING_KEYS = new Set(['ArrowDown', 'ArrowUp', 'Enter', ' ']);

/** Ouvre la liste sur l'option choisie, ou sur la première. */
function openList<T>(state: SelectState<T>): void {
    if (state.disabled()) return;
    state.dropdown.open();
    if (state.selectedIndex.value >= 0) state.listbox.setActive(state.selectedIndex.value);
    else state.listbox.first();
}

/** Choisit une option, puis ferme la liste. */
function choose<T>(state: SelectState<T>, index: number): void {
    const option = state.options()[index];
    if (!option || option.disabled) return;
    state.model.value = option.value;
    state.dropdown.close();
}

/** Action d'une touche quand la liste est ouverte, ou undefined. */
function openListAction<T>(state: SelectState<T>, key: string): (() => void) | undefined {
    const actions: Record<string, () => void> = {
        ArrowDown: () => state.listbox.move(1),
        ArrowUp: () => state.listbox.move(-1),
        Home: state.listbox.first,
        End: state.listbox.last,
        Enter: () => choose(state, state.listbox.activeIndex.value),
        ' ': () => choose(state, state.listbox.activeIndex.value),
        Escape: state.dropdown.close,
    };
    return actions[key];
}

/** Clavier, selon que la liste est ouverte ou fermée. Tab ferme sans bloquer le passage au champ suivant. */
function onKeydown<T>(state: SelectState<T>, event: KeyboardEvent): void {
    const isOpen = state.dropdown.isOpen.value;
    if (event.key === 'Tab') return state.dropdown.close();
    const action = isOpen ? openListAction(state, event.key) : undefined;
    if (action || (!isOpen && OPENING_KEYS.has(event.key))) {
        event.preventDefault();
        return action ? action() : openList(state);
    }
    if (event.key.length !== 1) return;
    if (!isOpen) openList(state);
    state.listbox.typeahead(event.key);
}

/** Clic à la souris ou au doigt. Le clic simulé par Espace ou Entrée (detail 0) est déjà géré par onKeydown. */
function onClick<T>(state: SelectState<T>, event: MouseEvent): void {
    if (event.detail === 0) return;
    if (state.dropdown.isOpen.value) state.dropdown.close();
    else openList(state);
}

/** Ce qu'affichent le bouton et la liste. */
function display<T>(state: SelectState<T>) {
    const {dropdown, listbox, selectedIndex} = state;
    return {
        rows: computed(() => listRows(state.options())),
        selectedLabel: computed(() => state.options()[selectedIndex.value]?.label ?? state.placeholder() ?? ''),
        triggerClass: computed(() => ({open: dropdown.isOpen.value, placeholder: selectedIndex.value < 0})),
        activeDescendant: computed(() => (dropdown.isOpen.value && listbox.activeIndex.value >= 0
            ? state.optionId(listbox.activeIndex.value) : undefined)),
        optionClass: (index: number) => ({active: index === listbox.activeIndex.value, selected: index === selectedIndex.value}),
        isSelected: (index: number): boolean => index === selectedIndex.value,
    };
}

/**
 * Crée une liste déroulante. À appeler dans le setup d'un composant qui a les refs « trigger » et « panel ».
 * @param source id, options, placeholder, disabled (accesseurs) et valeur liée (v-model)
 */
export function useSelect<T>(source: SelectSource<T>) {
    const optionId = (index: number): string => `${source.id}-option-${index}`;
    const dropdown = useDropdown(useTemplateRef<HTMLElement>('trigger'), useTemplateRef<HTMLElement>('panel'));
    const listbox = useListbox(computed(source.options), optionId);
    const selectedIndex = computed(() => source.options().findIndex(option => option.value === source.model.value));
    const state: SelectState<T> = {...source, dropdown, listbox, selectedIndex, optionId};
    return {
        ...display(state),
        isOpen: dropdown.isOpen,
        position: dropdown.position,
        listId: `${source.id}-liste`,
        optionId,
        setActive: listbox.setActive,
        choose: (index: number) => choose(state, index),
        onClick: (event: MouseEvent) => onClick(state, event),
        onKeydown: (event: KeyboardEvent) => onKeydown(state, event),
    };
}
