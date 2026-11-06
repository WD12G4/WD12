<script setup lang="ts">
/**
 * Informations connues d'un matériel identifié : type, emplacement déclaré, fin de disponibilité.
 * Utilisé par : scan-result-sheet.vue
 * RG : RG-04 (fin de disponibilité), RG-07 (en stock), RG-10 (archivé)
 */
import {computed} from 'vue';
import {isProvisional, type Material, materialTypeLabels} from '../../../models/material.model.ts';
import {daysUntil, EXPIRY_WARNING_DAYS, formatDate} from '../../../shared/composables/use-dates.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  material: Material;
}>();

//------------------ COMPUTED --------------------------------
/** Alerte sur la fin de disponibilité : dépassée, proche, ou rien. */
const expiry = computed(() => {
  const date = props.material.endAvailabilityDate;
  if (date === null) return null;
  const days = daysUntil(date);
  if (days < 0) return {kind: 'danger', text: 'dépassée'};
  return days <= EXPIRY_WARNING_DAYS ? {kind: 'warn', text: `dans ${days} j`} : null;
});
</script>

<template>
  <dl class="facts">
    <div>
      <dt>Type</dt>
      <dd>{{ materialTypeLabels[material.materialType] }}</dd>
    </div>
    <div>
      <dt>Déclaré en</dt>
      <dd>{{ locationLabel(material.room) }}</dd>
    </div>
    <div>
      <dt>Fin de dispo.</dt>
      <dd>
        {{ formatDate(material.endAvailabilityDate) }}
        <span v-if="expiry" class="badge" :class="expiry.kind">{{ expiry.text }}</span>
      </dd>
    </div>
    <div v-if="isProvisional(material)">
      <dt>Statut</dt>
      <dd><span class="badge warn">en attente d'envoi</span></dd>
    </div>
  </dl>
</template>
