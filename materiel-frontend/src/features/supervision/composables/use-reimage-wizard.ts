/**
 * État de l'assistant de masterisation (postes simulés) : étape, système, image de base,
 * applications cochées et postes visés. Changer de système ou d'image remet les applications
 * pré-cochées de l'image ; on peut ensuite en ajouter ou en retirer.
 * Utilisé par : reimage-wizard.vue
 * RG : RG-28
 */
import {computed, ref, type Ref} from 'vue';
import type {ReimageConfig} from '../../../models/reimage.model.ts';
import {appsForOs, configFor, configSummary, imageProfiles, osOptions} from '../../../services/simulation-catalog.mock.ts';

/** Étapes de l'assistant, dans l'ordre. */
const STEPS = ['Système', 'Image de base', 'Applications', 'Postes et lancement'] as const;
/** Durée simulée (secondes) : étapes fixes, puis par application et par poste (décalage). */
const BASE_SECONDS = 24;
const SECONDS_PER_APP = 1.5;
const SECONDS_PER_STATION = 1.8;

/** Choix proposé sous forme de carte (système, image de base). */
interface ChoiceOption {
    value: string;
    label: string;
    detail: string;
}

/** Ajoute ou retire une valeur d'une liste (case cochée ou décochée). */
function toggle<T>(list: T[], value: T): T[] {
    return list.includes(value) ? list.filter(item => item !== value) : [...list, value];
}

/** Choix de l'assistant (refs partagées par les valeurs calculées et les actions). */
interface WizardState {
    step: Ref<number>;
    osId: Ref<string>;
    profileId: Ref<string>;
    appIds: Ref<string[]>;
    targetIds: Ref<number[]>;
}

/** Cartes des systèmes et des images de base. */
const osChoices: ChoiceOption[] = osOptions.map(os => ({value: os.id, label: `${os.label} ${os.version}`, detail: `Image de ${os.imageSize}`}));
const profileChoices: ChoiceOption[] = imageProfiles.map(profile => ({value: profile.id, label: profile.label, detail: profile.description}));

/** Valeurs calculées : applications proposées, configuration, résumé, durée annoncée. */
function derived(state: WizardState) {
    const config = computed<ReimageConfig>(() => ({osId: state.osId.value, profileId: state.profileId.value, appIds: state.appIds.value}));
    /** Durée annoncée, arrondie à 5 secondes (« environ 35 s »). */
    const estimate = computed(() => {
        const stations = Math.max(state.targetIds.value.length - 1, 0);
        const seconds = BASE_SECONDS + state.appIds.value.length * SECONDS_PER_APP + stations * SECONDS_PER_STATION;
        return `environ ${Math.ceil(seconds / 5) * 5} s`;
    });
    return {
        config, estimate,
        availableApps: computed(() => appsForOs(state.osId.value)),
        summary: computed(() => configSummary(config.value)),
        isLast: computed(() => state.step.value === STEPS.length - 1),
        canLaunch: computed(() => state.targetIds.value.length > 0),
    };
}

/** Actions : choix du système et de l'image (applications remises à celles de l'image), cases, étapes. */
function actions(state: WizardState) {
    const resetApps = (): void => void (state.appIds.value = configFor(state.osId.value, state.profileId.value).appIds);
    return {
        chooseOs: (id: string) => { state.osId.value = id; resetApps(); },
        chooseProfile: (id: string) => { state.profileId.value = id; resetApps(); },
        toggleApp: (id: string) => void (state.appIds.value = toggle(state.appIds.value, id)),
        toggleTarget: (id: number) => void (state.targetIds.value = toggle(state.targetIds.value, id)),
        next: () => void (state.step.value = Math.min(state.step.value + 1, STEPS.length - 1)),
        back: () => void (state.step.value = Math.max(state.step.value - 1, 0)),
    };
}

/**
 * Crée l'état de l'assistant (système par défaut : le premier, image : Développement).
 * @param preselected postes cochés à l'ouverture
 * @returns étape, choix, listes proposées, résumé et actions de l'assistant
 */
export function useReimageWizard(preselected: number[]) {
    const osId = ref(osOptions[0].id);
    const profileId = ref('dev');
    const state: WizardState = {
        step: ref(0), osId, profileId, targetIds: ref([...preselected]),
        appIds: ref(configFor(osId.value, profileId.value).appIds),
    };
    return {steps: STEPS, osChoices, profileChoices, ...state, ...derived(state), ...actions(state)};
}

export type {ChoiceOption};
