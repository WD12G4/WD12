/**
 * Résultat de la lecture en cours et actions de l'agent dessus : changement de salle, prise en compte.
 * Un matériel conforme ne demande rien : le panneau se ferme seul et la lecture continue (cadence),
 * sauf si l'agent commence à agir dessus (hold).
 * Utilisé par : features/scan/views/scan-view.vue
 */
import {computed, onBeforeUnmount, ref} from 'vue';
import type {Material, MaterialInput} from '../../../models/material.model.ts';
import type {ScanOutcome, ScanSource} from '../../../models/scan.model.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {roomLabel} from '../../../shared/composables/use-room-label.ts';
import {
    type FieldActionResult, MaterialConflictError, useFieldActions,
} from '../../inventory/composables/use-field-actions.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';
import {useCurrentRoom} from '../../location/composables/use-current-room.ts';
import {identify} from '../lib/identify.ts';
import {parseScan} from '../lib/parse-scan.ts';
import {playTone} from '../lib/feedback.ts';
import {useRoomCheck} from './use-room-check.ts';
import {useScanHistory} from './use-scan-history.ts';

/** Durée d'affichage d'un résultat conforme avant fermeture automatique. */
const CONFORM_DISPLAY_MS = 1800;

/**
 * État du résultat affiché et ses actions, propre à la vue de scan.
 * @returns le résultat, son état et les actions de l'agent
 */
export function useScanResult() {
    const {show} = useFlashMessage();
    const {findBySerial, roomById} = useInventory();
    const {relocate, register} = useFieldActions();
    const {currentRoom} = useCurrentRoom();
    const {markSeen} = useRoomCheck(currentRoom);
    const {record, annotate} = useScanHistory();
    const {loading: busy, errorMessage, run} = useAsyncState();
    const outcome = ref<ScanOutcome | null>(null);
    /** État du serveur quand il contredit ce que l'agent voyait. */
    const conflict = ref<Material | null>(null);
    const holding = ref(false);
    let historyId = 0;
    let closeTimer: ReturnType<typeof setTimeout> | undefined;

    /** La lecture est suspendue tant qu'un résultat attend une décision. */
    const paused = computed(() => outcome.value !== null && (outcome.value.verdict !== 'conform' || holding.value));

    function close(): void {
        clearTimeout(closeTimer);
        outcome.value = null;
        conflict.value = null;
        holding.value = false;
        errorMessage.value = null;
    }

    /** L'agent agit sur le résultat : il reste affiché. */
    function hold(): void {
        holding.value = true;
        clearTimeout(closeTimer);
    }

    /** Analyse un code lu ou saisi et affiche le verdict. */
    function handleCode(raw: string, source: ScanSource): void {
        const reading = parseScan(raw, source);
        if (reading.serialNumber === '') return;
        const {verdict, material} = identify(reading, findBySerial, currentRoom.value);
        close();
        outcome.value = {reading, verdict, material, at: new Date()};
        historyId = record(outcome.value);
        if (currentRoom.value && material) markSeen(currentRoom.value.id, material.serialNumber);
        playTone(verdict === 'conform' ? 'ok' : 'alert');
        if (verdict === 'conform') closeTimer = setTimeout(close, CONFORM_DISPLAY_MS);
    }

    /** Message de fin d'action, selon que l'API a enregistré ou que l'action attend le réseau. */
    function confirm(result: FieldActionResult, what: string): void {
        show(result === 'saved' ? `${what}.` : `${what} sur le téléphone : envoi au retour du réseau.`,
            result === 'saved' ? 'success' : 'info');
    }

    /**
     * Change la salle du matériel affiché. En cas de conflit, le panneau montre l'état du serveur
     * et le prochain essai part de cet état (l'agent confirme en connaissance de cause).
     * @param roomId nouvelle salle, ou null pour mettre en stock
     */
    async function changeRoom(roomId: number | null): Promise<void> {
        const material = outcome.value?.material;
        if (!material) return;
        const room = roomId === null ? null : roomById(roomId);
        let result = 'saved' as FieldActionResult | 'conflict';
        const ok = await run(async () => {
            try {
                result = await relocate(material, room);
            } catch (error) {
                if (!(error instanceof MaterialConflictError)) throw error;
                result = 'conflict';
                hold();
                conflict.value = error.server;
                if (outcome.value) outcome.value = {...outcome.value, material: error.server};
            }
        });
        if (!ok || result === 'conflict') return;
        const pending = result === 'saved' ? '' : ' (en attente)';
        annotate(historyId, `${room ? 'Relocalisé' : 'Mis en stock'}${pending}`);
        confirm(result, `${material.serialNumber} ${room ? `relocalisé en ${roomLabel(room)}` : 'mis en stock'}`);
        close();
    }

    /** Prend en compte le matériel inconnu affiché, dans la salle courante. */
    async function registerMaterial(input: MaterialInput): Promise<void> {
        const room = currentRoom.value;
        const serialNumber = input.serialNumber.trim();
        let result = 'saved' as FieldActionResult;
        if (await run(async () => { result = await register(input, room); })) {
            annotate(historyId, result === 'saved' ? 'Pris en compte' : 'Pris en compte (en attente)', serialNumber);
            if (room) markSeen(room.id, serialNumber);
            confirm(result, `${serialNumber} pris en compte`);
            close();
        }
    }

    onBeforeUnmount(() => clearTimeout(closeTimer));

    return {outcome, conflict, paused, busy, errorMessage, close, hold, handleCode, changeRoom, registerMaterial};
}
