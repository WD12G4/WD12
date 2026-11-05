<script setup lang="ts">
/**
 * Compte rendu d'une salle : matériel théoriquement sur place (déclaré dans le parc) et matériel réellement
 * sur place (scanné dans la salle), avec les écarts. Consultation seule : rien n'est vérifié ni modifié ici.
 * Utilisé par : router/routes.ts (route « room »)
 */
import {computed} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {formatDate, formatTime} from '../../../shared/composables/use-dates.ts';
import {roomLabel} from '../../../shared/composables/use-room-label.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';
import {useCurrentRoom} from '../../location/composables/use-current-room.ts';
import {useSeenRecords} from '../../scan/composables/use-room-check.ts';
import ReportLine from '../components/report-line.vue';
import {buildRoomReport} from '../lib/room-report.ts';

//------------------ DATAS -----------------------------------
const route = useRoute();
const router = useRouter();
const {materials, roomById} = useInventory();
const {selectRoom} = useCurrentRoom();
const {seenInRoom} = useSeenRecords();

//------------------ COMPUTED --------------------------------
const room = computed(() => roomById(Number(route.params.id)));
const report = computed(() => (room.value ? buildRoomReport(room.value.id, materials.value, seenInRoom(room.value.id)) : null));
const lastScan = computed(() => {
  const at = report.value?.lastSeenAt;
  return at ? `Dernier scan ici le ${formatDate(at)} à ${formatTime(at)}.` : 'Pas encore reconnue sur ce téléphone.';
});

//------------------ FUNCTIONS -------------------------------
/** Se positionne dans cette salle et part scanner. */
function reconnoitre(): void {
  if (!room.value) return;
  selectRoom(room.value.id);
  void router.push({name: 'scan'});
}
</script>

<template>
  <section class="stack">
    <RouterLink :to="{name: 'rooms'}" class="back small">← Toutes les salles</RouterLink>

    <p v-if="!room || !report" class="notice">Salle inconnue dans le parc en cache : synchronisez puis réessayez.</p>
    <template v-else>
      <div>
        <p class="mono small">COMPTE RENDU DE SALLE</p>
        <h1>{{ roomLabel(room) }}</h1>
        <p class="muted small">{{ lastScan }}</p>
      </div>

      <div class="row">
        <span class="counter">Théoriques <strong>{{ report.declared.length }}</strong></span>
        <span class="counter">Constatés <strong>{{ report.observed.length }}</strong></span>
        <span class="counter" :class="{alert: report.missing > 0}">Non constatés <strong>{{ report.missing }}</strong></span>
        <span class="counter" :class="{alert: report.extra > 0}">Écarts sur place <strong>{{ report.extra }}</strong></span>
      </div>

      <section class="panel">
        <h2>Théoriquement sur place</h2>
        <p class="muted small">Déclaré dans cette salle selon le parc.</p>
        <p v-if="report.declared.length === 0" class="small">Aucun matériel déclaré ici.</p>
        <ul v-else class="plain-list report-list">
          <ReportLine v-for="line in report.declared" :key="line.serialNumber" :line="line" />
        </ul>
      </section>

      <section class="panel">
        <h2>Réellement sur place</h2>
        <p class="muted small">Scanné dans cette salle avec ce téléphone, recensé ou non.</p>
        <p v-if="report.observed.length === 0" class="small">Rien n'a encore été scanné ici.</p>
        <ul v-else class="plain-list report-list">
          <ReportLine v-for="line in report.observed" :key="line.serialNumber" :line="line" />
        </ul>
      </section>

      <button type="button" class="secondary" @click="reconnoitre">Reconnaître cette salle</button>
    </template>
  </section>
</template>
