<script setup lang="ts">
/**
 * Choix de la salle où se trouve l'agent. Les salles viennent du parc en cache : le choix marche sans réseau.
 * Recherche par numéro : « 101 10 » garde la salle 10 du bâtiment 101.
 */
import {computed, ref} from 'vue';
import {useRouter} from 'vue-router';
import type {Room} from '../../../models/room.model.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';
import {useCurrentRoom} from '../composables/use-current-room.ts';

/** Salles d'un même bâtiment. */
interface BuildingGroup {
  building: number;
  rooms: Room[];
}

//------------------ DATAS -----------------------------------
const router = useRouter();
const {rooms} = useInventory();
const {currentRoom, selectRoom} = useCurrentRoom();
const search = ref('');

//------------------ COMPUTED --------------------------------
/** Salles filtrées (chaque mot cherché doit commencer un numéro de bâtiment ou de salle), par bâtiment. */
const groups = computed<BuildingGroup[]>(() => {
  const words = search.value.trim().split(/\s+/).filter(Boolean);
  const matches = (room: Room) => words.every((word) =>
      String(room.building.number).startsWith(word) || String(room.number).startsWith(word));
  const byBuilding = new Map<number, Room[]>();
  for (const room of rooms.value.filter(matches)) {
    byBuilding.set(room.building.number, [...(byBuilding.get(room.building.number) ?? []), room]);
  }
  return [...byBuilding.entries()]
      .sort(([a], [b]) => a - b)
      .map(([building, list]) => ({building, rooms: list.sort((a, b) => a.number - b.number)}));
});

//------------------ FUNCTIONS -------------------------------
/** Enregistre la salle et revient au scan. */
function choose(id: number | null): void {
  selectRoom(id);
  void router.push({name: 'scan'});
}
</script>

<template>
  <section class="stack">
    <div>
      <p class="mono small">POSITION</p>
      <h1>Où êtes-vous ?</h1>
      <p>Les matériels scannés ensuite seront comparés à cette salle.</p>
    </div>
    <input v-model="search" type="search" inputmode="numeric" placeholder="Bâtiment ou salle (ex. 101 10)"
           aria-label="Rechercher une salle">
    <p v-if="rooms.length === 0" class="notice">Aucune salle en cache : connectez-vous au réseau puis synchronisez.</p>
    <p v-else-if="groups.length === 0" class="notice">Aucune salle ne correspond.</p>

    <section v-for="group in groups" :key="group.building" class="panel">
      <h2 class="mono small">BÂTIMENT {{ group.building }}</h2>
      <div class="room-grid">
        <button v-for="room in group.rooms" :key="room.id" type="button"
                :class="room.id === currentRoom?.id ? 'primary' : 'secondary'" @click="choose(room.id)">
          Salle {{ room.number }}
        </button>
      </div>
    </section>

    <button type="button" class="ghost" @click="choose(null)">Aucune salle (identification seule)</button>
  </section>
</template>
