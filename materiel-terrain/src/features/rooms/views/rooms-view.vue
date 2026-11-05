<script setup lang="ts">
/**
 * Situation des salles : pour chaque salle, matériel théoriquement sur place (parc) et réellement sur place
 * (scans), et l'état qui en découle. Recherche d'une salle (« 101 10 ») ou d'un matériel par son numéro,
 * pour savoir où il est déclaré et où il a été vu. Consultation seule : rien n'est vérifié ni modifié ici.
 * Utilisé par : router/routes.ts (route « rooms »)
 */
import {computed, ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {formatDate, formatTime} from '../../../shared/composables/use-dates.ts';
import {roomLabel} from '../../../shared/composables/use-room-label.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';
import {useSightings} from '../../inventory/composables/use-sightings.ts';
import {groupRoomsByBuilding} from '../../location/lib/room-groups.ts';
import {serialKey} from '../../scan/lib/identify.ts';
import ReportLine from '../components/report-line.vue';
import {buildRoomReport, locateMaterial, type RoomReport, roomState, type Sighting} from '../lib/room-report.ts';

//------------------ DATAS -----------------------------------
const {materials, rooms, roomById} = useInventory();
const {seenInRoom} = useSightings();
const search = ref('');
/** Nombre de caractères à partir duquel la recherche porte aussi sur les numéros de série. */
const SERIAL_SEARCH_MIN = 3;

//------------------ COMPUTED --------------------------------
const reports = computed(() => new Map(rooms.value.map((room) =>
    [room.id, buildRoomReport(room.id, materials.value, seenInRoom(room.id))])));
const words = computed(() => search.value.trim().split(/\s+/).filter(Boolean));
/** Salles dont chaque mot cherché commence le numéro de bâtiment ou de salle. */
const groups = computed(() => groupRoomsByBuilding(rooms.value.filter((room) => words.value.every((word) =>
    String(room.building.number).startsWith(word) || String(room.number).startsWith(word)))));
/** Matériels dont le numéro contient le texte cherché. */
const found = computed(() => {
  const query = serialKey(search.value);
  if (query.length < SERIAL_SEARCH_MIN) return [];
  return materials.value.filter((material) => serialKey(material.serialNumber).includes(query)).slice(0, 20).map(locate);
});
const stock = computed(() => materials.value.filter((material) => !material.archived && material.room === null).map(locate));

//------------------ FUNCTIONS -------------------------------
/** Situation d'un matériel d'après ses scans dans toutes les salles. */
function locate(material: Material) {
  const sightings: Sighting[] = rooms.value.flatMap((room) => {
    const seen = seenInRoom(room.id)[serialKey(material.serialNumber)];
    return seen ? [{roomId: room.id, at: new Date(seen.at), by: seen.by}] : [];
  });
  const line = locateMaterial(material, sightings);
  const room = line.lastRoomId === null ? null : roomById(line.lastRoomId);
  const by = line.seenBy ? ` par ${line.seenBy}` : '';
  const seenLabel = line.seenAt && room
    ? `Constaté en ${roomLabel(room)} le ${formatDate(line.seenAt)} à ${formatTime(line.seenAt)}${by}.`
    : 'Jamais constaté.';
  return {line, seenLabel};
}

/** Badge d'état d'une salle. */
function stateBadge(report: RoomReport | undefined): {kind: string; text: string} {
  const state = report ? roomState(report) : 'unchecked';
  if (state === 'unchecked') return {kind: 'muted', text: 'non reconnue'};
  if (state === 'conform') return {kind: 'ok', text: 'conforme'};
  const gaps = report!.missing + report!.extra;
  return {kind: 'warn', text: `${gaps} écart${gaps > 1 ? 's' : ''}`};
}

/** Nombres de matériels théoriquement et réellement sur place. */
function counts(room: Room): string {
  const report = reports.value.get(room.id);
  const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`;
  return `${plural(report?.declared.length ?? 0, 'théorique')} · ${plural(report?.observed.length ?? 0, 'constaté')}`;
}
</script>

<template>
  <section class="stack">
    <div>
      <p class="mono small">SITUATION DES SALLES</p>
      <h1>Salles</h1>
      <p class="muted small">Ce que le parc déclare face à ce que les agents ont constaté sur place. Consultation seule.</p>
    </div>
    <input v-model="search" type="search" placeholder="Salle (ex. 101 10) ou n° de série"
           aria-label="Rechercher une salle ou un matériel">

    <section v-if="found.length > 0" class="panel">
      <h2 class="mono small">MATÉRIELS TROUVÉS</h2>
      <ul class="plain-list report-list">
        <ReportLine v-for="item in found" :key="item.line.serialNumber" :line="item.line" :seen-label="item.seenLabel" />
      </ul>
    </section>

    <p v-if="rooms.length === 0" class="notice">Aucune salle en cache : connectez-vous au réseau puis synchronisez.</p>
    <p v-else-if="groups.length === 0 && found.length === 0" class="notice">Aucune salle ni aucun matériel ne correspond.</p>

    <section v-for="group in groups" :key="group.building" class="panel">
      <h2 class="mono small">BÂTIMENT {{ group.building }}</h2>
      <ul class="plain-list room-list">
        <li v-for="room in group.rooms" :key="room.id">
          <RouterLink :to="{name: 'room', params: {id: room.id}}" class="room-link">
            <span><strong>Salle {{ room.number }}</strong><br><span class="muted small">{{ counts(room) }}</span></span>
            <span class="badge" :class="stateBadge(reports.get(room.id)).kind">{{ stateBadge(reports.get(room.id)).text }}</span>
          </RouterLink>
        </li>
      </ul>
    </section>

    <details v-if="words.length === 0" class="panel">
      <summary><strong>En stock</strong> <span class="muted small">({{ stock.length }}, sans salle)</span></summary>
      <p v-if="stock.length === 0" class="small">Aucun matériel en stock.</p>
      <ul v-else class="plain-list report-list">
        <ReportLine v-for="item in stock" :key="item.line.serialNumber" :line="item.line" :seen-label="item.seenLabel" />
      </ul>
    </details>
  </section>
</template>
