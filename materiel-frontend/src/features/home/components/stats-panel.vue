<script setup lang="ts">
/**
 * Compteurs du tableau de bord, chacun menant à la liste du matériel, déjà filtrée quand c'est possible.
 * Composant de présentation.
 * Utilisé par : home-view.vue
 */
import type {DashboardStats} from '../composables/use-dashboard-stats.ts';
import Link from '../../../shared/components/Link.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Compteurs à afficher. */
  stats: DashboardStats;
}>();
</script>

<template>
  <div class="stats">
    <Link class="stat" :to="{ name: 'materials' }">
      <strong>{{ stats.active }}</strong><span>Matériels suivis</span>
    </Link>
    <Link class="stat" :to="{ name: 'materials', query: { emplacement: 'salle' } }">
      <strong>{{ stats.assigned }}</strong><span>Affectés à une salle</span>
    </Link>
    <Link class="stat" :to="{ name: 'materials', query: { emplacement: 'stock' } }">
      <strong>{{ stats.stock }}</strong><span>En stock</span>
    </Link>
    <Link class="stat" :to="{ name: 'materials' }">
      <strong>{{ stats.expiring }}</strong><span>Bientôt en fin de disponibilité</span>
    </Link>
  </div>
</template>
