<script setup lang="ts">
/**
 * Liste déroulante de salles, regroupées par bâtiment.
 * La valeur liée (v-model) est l'identifiant de la salle, ou null pour l'option vide.
 * Utilisé par : formulaire et affectation du matériel, formulaire de remontée.
 */
import {computed} from 'vue';
import type {Room} from '../../models/room.model.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** id de la balise <select>, relié au <label for> du parent. */
  id: string;
  /** Salles proposées. */
  rooms: Room[];
  /** Texte de l'option vide (ex. « En stock »). */
  emptyLabel: string;
  /** Désactive la liste (ex. matériel archivé, RG-06). */
  disabled?: boolean;
}>();

/** Identifiant de la salle choisie, ou null. */
const roomId = defineModel<number | null>({required: true});

//------------------ COMPUTED --------------------------------
/** Salles regroupées par numéro de bâtiment, pour des <optgroup> triés. */
const groups = computed(() => {
  const byBuilding = new Map<number, Room[]>();
  for (const room of props.rooms) {
    const list = byBuilding.get(room.building.number) ?? [];
    list.push(room);
    byBuilding.set(room.building.number, list);
  }
  return [...byBuilding.entries()].sort(([a], [b]) => a - b);
});
</script>

<template>
  <select
      :id="id"
      v-model="roomId"
      :disabled="disabled"
  >
    <option :value="null">{{ emptyLabel }}</option>
    <optgroup
        v-for="[buildingNumber, buildingRooms] in groups"
        :key="buildingNumber"
        :label="`Bâtiment ${buildingNumber}`"
    >
      <option
          v-for="room in buildingRooms"
          :key="room.id"
          :value="room.id"
      >
        Salle {{ room.number }}
      </option>
    </optgroup>
  </select>
</template>
