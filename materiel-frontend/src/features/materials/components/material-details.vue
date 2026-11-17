<script setup lang="ts">
/**
 * Fiche d'un matériel : type, emplacement, fin de disponibilité et statut.
 * Composant de présentation.
 * Utilisé par : material-detail-view.vue
 */
import {computed} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {formatDate} from '../../../shared/composables/use-dates.ts';
import {useMaterialTypes} from '../../../shared/composables/use-material-types.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';
import MaterialStatusBadge from '../../../shared/components/material-status-badge.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel à décrire. */
  material: Material;
}>();

//------------------ DATAS -----------------------------------
const {typeLabel} = useMaterialTypes();

//------------------ COMPUTED --------------------------------
const location = computed(() => locationLabel(props.material.room));
const endDate = computed(() => formatDate(props.material.endAvailabilityDate));
</script>

<template>
  <dl class="detail-grid">
    <div>
      <dt>Type</dt>
      <dd>{{ typeLabel(material.materialType) }}</dd>
    </div>
    <div>
      <dt>Emplacement</dt>
      <dd>{{ location }}</dd>
    </div>
    <div>
      <dt>Fin de disponibilité</dt>
      <dd>{{ endDate }}</dd>
    </div>
    <div>
      <dt>Statut</dt>
      <dd><MaterialStatusBadge :material="material" /></dd>
    </div>
  </dl>
</template>
