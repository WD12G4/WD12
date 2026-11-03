<script setup lang="ts">
/**
 * Affectation d'un matériel à une salle, ou remise en stock (MANAGER et ADMIN).
 * Composant de présentation : il émet la salle choisie, la vue appelle le service.
 * Utilisé par : material-detail-view.vue
 * RG : RG-06 (pas de salle pour un archivé), RG-07 (en stock = sans salle), RG-17 (droit du gestionnaire)
 */
import {computed, ref, watch} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import FormError from '../../../shared/components/form-error.vue';
import RoomSelect from '../../../shared/components/room-select.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel à affecter. */
  material: Material;
  /** Salles proposées. */
  rooms: Room[];
  /** Vrai pendant l'envoi. */
  submitting: boolean;
  /** Erreur renvoyée par l'API, ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Nouvelle salle (null = remettre en stock). */
  assign: [roomId: number | null];
}>();

//------------------ DATAS -----------------------------------
const selectedRoomId = ref<number | null>(null);

//------------------ COMPUTED --------------------------------
/** Rien à envoyer si la salle choisie est déjà celle du matériel. */
const unchanged = computed(() => selectedRoomId.value === (props.material.room?.id ?? null));
const canSubmit = computed(() => !unchanged.value && !props.submitting);

//------------------ LIFECYCLE -------------------------------
// La liste part de la salle actuelle, et se recale après chaque affectation réussie.
watch(() => props.material, material => {
  selectedRoomId.value = material.room?.id ?? null;
}, {immediate: true});
</script>

<template>
  <!-- RG-06 : un matériel archivé ne peut pas être affecté -->
  <p v-if="material.archived" class="muted">
    Ce matériel est archivé : il ne peut pas être affecté à une salle.
  </p>
  <form
      v-else
      class="form-grid"
      @submit.prevent="emit('assign', selectedRoomId)"
  >
    <div class="field">
      <label for="assign-room">Nouvelle salle</label>
      <RoomSelect
          id="assign-room"
          v-model="selectedRoomId"
          :rooms="rooms"
          empty-label="En stock (aucune salle)"
      />
    </div>
    <div class="full-width">
      <FormError :message="errorMessage" />
      <button type="submit" :disabled="!canSubmit">Valider l'emplacement</button>
    </div>
  </form>
</template>
