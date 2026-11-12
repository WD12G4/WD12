<script setup lang="ts">
/**
 * Menu principal : marque, liens autorisés, utilisateur connecté et déconnexion.
 * Grand écran : barre flottante en haut, pastille glissante sous la rubrique courante et
 * « Signaler un problème » en bouton d'action à droite, suivi, pour le formateur, du bouton de sa
 * salle de cours (teaching-room-switch.vue), et du choix du thème (theme-switch.vue).
 * Mobile : tiroir en verre ouvert par le bouton
 * menu de app-topbar.vue. La mise en page vient de style.css (MENU PRINCIPAL).
 * Composant de présentation : les liens arrivent déjà filtrés selon le rôle, avec leur compteur
 * (pastille rouge, ex. remontées à traiter du gestionnaire) (use-nav-links.ts).
 * Utilisé par : App.vue
 */
import {useTemplateRef} from 'vue';
import {useRoute} from 'vue-router';
import type {RouteNames} from '../../router/route-types.ts';
import type {User} from '../../models/user.model.ts';
import type {NavLink} from '../composables/use-nav-links.ts';
import {useNavIndicator} from '../composables/use-nav-indicator.ts';
import {useTeachingRoom} from '../composables/use-teaching-room.ts';
import AppIcon from './app-icon.vue';
import Link from './Link.vue';
import TeachingRoomSwitch from './teaching-room-switch.vue';
import ThemeSwitch from './theme-switch.vue';
import UserCard from './user-card.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Liens à afficher. */
  links: readonly NavLink[];
  /** Utilisateur connecté. */
  user: User;
  /** Vrai si le tiroir est ouvert (mobile uniquement). */
  open: boolean;
}>();

const emit = defineEmits<{
  /** L'utilisateur demande à se déconnecter. */
  logout: [];
  /** L'utilisateur ferme le tiroir (clic sur le fond, mobile). */
  close: [];
}>();

//------------------ DATAS -----------------------------------
const route = useRoute();
const nav = useTemplateRef<HTMLElement>('nav');
useNavIndicator(nav);
const {isTrainer} = useTeachingRoom();

//------------------ FUNCTIONS -------------------------------
/** Vrai si la page courante appartient à la rubrique du lien (ex. fiche d'une salle → « Salles »). */
function isCurrent(link: NavLink): boolean {
  return link.section.includes(route.name as RouteNames);
}

/** Classes d'un lien : rubrique courante, lien d'action (bouton mis en avant dans la barre du haut). */
function linkClass(link: NavLink): Record<string, boolean> {
  return {current: isCurrent(link), primary: link.primary === true};
}
</script>

<template>
  <div
      id="main-menu"
      class="main-menu"
      :class="{ open }"
  >
    <!-- Marque (thème DSFR : bloc-marque de l'État, affiché par style.css) -->
    <div class="brand">
      <p class="fr-logo brand-marque">République<br>Française</p>
      <span class="brand-logo" aria-hidden="true">GP</span>
      <span class="brand-name">Gestion du parc</span>
    </div>

    <!-- Liens (la pastille ne sert que dans la barre du haut) -->
    <nav
        ref="nav"
        class="nav"
        aria-label="Menu principal"
    >
      <span class="nav-pill" aria-hidden="true" />
      <Link
          v-for="link in links"
          :key="link.to.name"
          :to="link.to"
          :class="linkClass(link)"
      >
        <AppIcon :name="link.icon" />
        <span>{{ link.label }}</span>
        <span v-if="link.count" class="nav-count">
          {{ link.count }}<span class="visually-hidden"> à traiter</span>
        </span>
      </Link>
    </nav>

    <!-- Salle de cours du formateur -->
    <TeachingRoomSwitch v-if="isTrainer" />

    <!-- Choix du thème -->
    <ThemeSwitch />

    <!-- Utilisateur connecté (et, en mode démonstration, changement de compte) -->
    <UserCard :user="user" @logout="emit('logout')" />
  </div>

  <!-- Fond cliquable derrière le tiroir (mobile) -->
  <Transition name="fade">
    <div
        v-if="open"
        class="main-menu-backdrop"
        @click="emit('close')"
    />
  </Transition>
</template>
