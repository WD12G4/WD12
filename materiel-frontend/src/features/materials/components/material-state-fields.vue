<script setup lang="ts">
/**
 * Champs d'état du matériel : date de fin de disponibilité, salle et archivage.
 * Relié au brouillon du formulaire par v-model (defineModel).
 * Utilisé par : material-form.vue
 * RG : RG-04, RG-05 (date), RG-06 (archivé ⇒ pas de salle), RG-07 (sans salle = en stock)
 */
import {computed, watch} from 'vue';
import type {MaterialInput} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {todayIsoDate} from '../../../shared/composables/use-dates.ts';
import RoomSelect from '../../../shared/components/room-select.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Salles proposées pour l'affectation initiale. */
  rooms: Room[];
}>();

/** Brouillon du formulaire, partagé avec material-form.vue. */
const draft = defineModel<MaterialInput>({required: true});

//------------------ COMPUTED --------------------------------
/** RG-05 : la date ne peut pas être passée, sauf pour un matériel archivé. */
const minDate = computed(() => (draft.value.archived ? undefined : todayIsoDate()));

//------------------ LIFECYCLE -------------------------------
// RG-06 : archiver un matériel le retire de sa salle.
watch(() => draft.value.archived, archived => {
  if (archived) draft.value.roomId = null;
});
</script>

<template>
  <div class="field">
    <label for="material-end-date">Fin de disponibilité <span class="field-hint">(facultative)</span></label>
    <input
        id="material-end-date"
        v-model="draft.endAvailabilityDate"
        type="date"
        :min="minDate"
    >
  </div>
  <div class="field">
    <label for="material-room">Salle</label>
    <RoomSelect
        id="material-room"
        v-model="draft.roomId"
        :rooms="rooms"
        empty-label="En stock (aucune salle)"
        :disabled="draft.archived"
    />
  </div>
  <label class="checkbox-field full-width">
    <input v-model="draft.archived" type="checkbox">
    Matériel archivé (il ne peut alors pas être affecté à une salle)
  </label>
</template>
