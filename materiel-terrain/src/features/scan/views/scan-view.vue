<script setup lang="ts">
/**
 * Écran principal de reconnaissance : position, viseur, saisie manuelle, résultat et historique.
 * Chaque lecture est analysée (parse-scan), comparée au parc en cache (identify), puis l'agent agit :
 * relocaliser un matériel déplacé, prendre en compte un matériel inconnu.
 * Un matériel conforme ne demande rien : le panneau se referme seul et la lecture continue (cadence).
 */
import {computed, ref} from 'vue';
import {useRouter} from 'vue-router';
import type {MaterialInput} from '../../../models/material.model.ts';
import type {ScanOutcome, ScanSource} from '../../../models/scan.model.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {roomLabel} from '../../../shared/composables/use-room-label.ts';
import {type FieldActionResult, useFieldActions} from '../../inventory/composables/use-field-actions.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';
import {useCurrentRoom} from '../../location/composables/use-current-room.ts';
import {useRoomCheck} from '../composables/use-room-check.ts';
import {useScanHistory} from '../composables/use-scan-history.ts';
import {identify} from '../lib/identify.ts';
import {parseScan} from '../lib/parse-scan.ts';
import {playTone} from '../lib/feedback.ts';
import CameraViewfinder from '../components/camera-viewfinder.vue';
import ManualEntry from '../components/manual-entry.vue';
import RoomCheckPanel from '../components/room-check-panel.vue';
import ScanHistory from '../components/scan-history.vue';
import ScanResultSheet from '../components/scan-result-sheet.vue';

/** Durée d'affichage d'un résultat conforme avant fermeture automatique. */
const CONFORM_DISPLAY_MS = 1800;

//------------------ DATAS -----------------------------------
const router = useRouter();
const {hasRole} = useAuth();
const {show} = useFlashMessage();
const {findBySerial} = useInventory();
const {relocate, register} = useFieldActions();
const {currentRoom} = useCurrentRoom();
const {expected, missing, seenCount, markSeen, resetRoom} = useRoomCheck(currentRoom);
const {entries, record, annotate} = useScanHistory();
const {loading: busy, errorMessage, run} = useAsyncState();
const outcome = ref<ScanOutcome | null>(null);
let historyId = 0;
let closeTimer: ReturnType<typeof setTimeout> | undefined;

//------------------ COMPUTED --------------------------------
const canEdit = computed(() => hasRole('MANAGER'));
/** La lecture est suspendue tant qu'un résultat attend une décision. */
const paused = computed(() => outcome.value !== null && outcome.value.verdict !== 'conform');

//------------------ FUNCTIONS -------------------------------
function close(): void {
  clearTimeout(closeTimer);
  outcome.value = null;
  errorMessage.value = null;
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

async function onRelocate(): Promise<void> {
  const material = outcome.value?.material;
  const room = currentRoom.value;
  if (!material || !room) return;
  let result: FieldActionResult = 'saved';
  if (await run(async () => { result = await relocate(material, room); })) {
    annotate(historyId, result === 'saved' ? 'Relocalisé' : 'Relocalisé (en attente)');
    confirm(result, `${material.serialNumber} relocalisé en ${roomLabel(room)}`);
    close();
  }
}

async function onRegister(input: MaterialInput): Promise<void> {
  const room = currentRoom.value;
  const serialNumber = input.serialNumber.trim();
  let result: FieldActionResult = 'saved';
  if (await run(async () => { result = await register(input, room); })) {
    annotate(historyId, result === 'saved' ? 'Pris en compte' : 'Pris en compte (en attente)', serialNumber);
    if (room) markSeen(room.id, serialNumber);
    confirm(result, `${serialNumber} pris en compte`);
    close();
  }
}

function pickRoom(): void {
  close();
  void router.push({name: 'position'});
}
</script>

<template>
  <div class="scan-view">
    <RoomCheckPanel
        :room="currentRoom" :expected="expected" :missing="missing" :seen-count="seenCount"
        @change="pickRoom" @reset="currentRoom && resetRoom(currentRoom.id)"
    />
    <CameraViewfinder :paused="paused" @code="handleCode" />
    <ManualEntry @submit="handleCode($event, 'manual')" />
    <ScanHistory :entries="entries" />

    <Transition name="sheet">
      <ScanResultSheet
          v-if="outcome"
          :outcome="outcome" :current-room="currentRoom" :can-edit="canEdit"
          :busy="busy" :error-message="errorMessage"
          @relocate="onRelocate" @register="onRegister" @pick-room="pickRoom" @close="close"
      />
    </Transition>
  </div>
</template>
