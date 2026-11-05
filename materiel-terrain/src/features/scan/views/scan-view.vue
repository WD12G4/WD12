<script setup lang="ts">
/**
 * Écran principal de reconnaissance : position, viseur, saisie manuelle, résultat et historique.
 * Chaque lecture est analysée (parse-scan), comparée au parc en cache (identify), puis l'agent agit :
 * changer la salle d'un matériel (ou le mettre en stock), prendre en compte un matériel inconnu.
 * La logique du résultat est dans use-scan-result.ts.
 */
import {computed} from 'vue';
import {useRouter} from 'vue-router';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';
import {useCurrentRoom} from '../../location/composables/use-current-room.ts';
import {useRoomCheck} from '../composables/use-room-check.ts';
import {useScanHistory} from '../composables/use-scan-history.ts';
import {useScanResult} from '../composables/use-scan-result.ts';
import CameraViewfinder from '../components/camera-viewfinder.vue';
import ManualEntry from '../components/manual-entry.vue';
import RoomCheckPanel from '../components/room-check-panel.vue';
import ScanHistory from '../components/scan-history.vue';
import ScanResultSheet from '../components/scan-result-sheet.vue';

//------------------ DATAS -----------------------------------
const router = useRouter();
const {hasRole} = useAuth();
const {rooms} = useInventory();
const {currentRoom} = useCurrentRoom();
const {expected, missing, seenCount, resetRoom} = useRoomCheck(currentRoom);
const {entries} = useScanHistory();
const {
  outcome, conflict, paused, busy, errorMessage, close, hold, handleCode, changeRoom, registerMaterial,
} = useScanResult();

//------------------ COMPUTED --------------------------------
const canEdit = computed(() => hasRole('MANAGER'));

//------------------ FUNCTIONS -------------------------------
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
          :outcome="outcome" :current-room="currentRoom" :rooms="rooms" :conflict="conflict"
          :can-edit="canEdit" :busy="busy" :error-message="errorMessage"
          @relocate="changeRoom" @hold="hold" @register="registerMaterial" @pick-room="pickRoom" @close="close"
      />
    </Transition>
  </div>
</template>
