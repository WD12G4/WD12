<script setup lang="ts">
/**
 * Changement de salle d'un matériel identifié : vers la salle de l'agent (bouton direct),
 * ou vers n'importe quelle salle, ou en stock (liste). Si le serveur a été modifié entre-temps
 * par quelqu'un d'autre, son état est affiché et l'agent confirme avant d'écraser.
 * Utilisé par : scan-result-sheet.vue
 * RG : RG-07 (sans salle = en stock)
 */
import {computed, ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {locationLabel, roomLabel} from '../../../shared/composables/use-room-label.ts';
import {groupRoomsByBuilding} from '../../location/lib/room-groups.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  material: Material;
  currentRoom: Room | null;
  rooms: Room[];
  /** Vrai pour un matériel déplacé ou en stock : bouton direct vers la salle de l'agent. */
  quick: boolean;
  busy: boolean;
  /** État du serveur quand il contredit ce que l'agent voyait, ou null. */
  conflict: Material | null;
}>();

const emit = defineEmits<{
  /** Salle choisie, ou null pour mettre en stock. */
  relocate: [roomId: number | null];
}>();

//------------------ DATAS -----------------------------------
/** Valeur de la liste pour « En stock ». */
const STOCK = 'stock';
const listOpen = ref(false);
const target = ref<number | typeof STOCK>(props.currentRoom?.id ?? props.material.room?.id ?? STOCK);

//------------------ COMPUTED --------------------------------
const groups = computed(() => groupRoomsByBuilding(props.rooms));
const targetRoomId = computed(() => (target.value === STOCK ? null : target.value));
/** Rien à valider si la cible est déjà la salle déclarée. */
const canValidate = computed(() => !props.busy && targetRoomId.value !== (props.material.room?.id ?? null));
const force = computed(() => (props.conflict ? ' quand même' : ''));
</script>

<template>
  <div class="relocation">
    <p v-if="conflict" class="notice">
      Modifié entre-temps par quelqu'un d'autre : il est maintenant
      <strong>{{ conflict.archived ? 'archivé' : `déclaré en ${locationLabel(conflict.room)}` }}</strong>.
      <template v-if="!conflict.archived">Confirmez pour le déplacer quand même.</template>
    </p>

    <template v-if="!conflict?.archived">
      <button v-if="quick && currentRoom" type="button" class="primary" :disabled="busy"
              @click="emit('relocate', currentRoom.id)">
        {{ busy ? 'Enregistrement…' : `Relocaliser${force} en ${roomLabel(currentRoom)}` }}
      </button>

      <button v-if="!listOpen" type="button" class="secondary" @click="listOpen = true">Changer de salle…</button>
      <div v-else class="row">
        <select v-model="target" aria-label="Nouvelle salle">
          <option :value="STOCK">En stock (aucune salle)</option>
          <optgroup v-for="group in groups" :key="group.building" :label="`Bâtiment ${group.building}`">
            <option v-for="room in group.rooms" :key="room.id" :value="room.id">{{ roomLabel(room) }}</option>
          </optgroup>
        </select>
        <button type="button" class="primary" :disabled="!canValidate" @click="emit('relocate', targetRoomId)">
          Valider{{ force }}
        </button>
      </div>
    </template>
  </div>
</template>
