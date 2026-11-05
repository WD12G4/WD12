<script setup lang="ts">
/**
 * Viseur : flux de la caméra arrière avec le cadre de visée, démarrage / arrêt, et lecture sur photo.
 * Émet le texte de chaque code lu ; la vue décide quoi en faire.
 * Utilisé par : features/scan/views/scan-view.vue
 */
import {ref, watch} from 'vue';
import type {ScanSource} from '../../../models/scan.model.ts';
import {useCameraScanner, VIEWFINDER_RATIO} from '../composables/use-camera-scanner.ts';
import {unlockAudio} from '../lib/feedback.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Vrai quand un résultat attend une action : la lecture est suspendue, la caméra reste allumée. */
  paused: boolean;
}>();

const emit = defineEmits<{
  /** Texte brut d'un code lu. */
  code: [raw: string, source: ScanSource];
}>();

//------------------ DATAS -----------------------------------
const video = ref<HTMLVideoElement | null>(null);
const readingPhoto = ref(false);
const photoError = ref<string | null>(null);
const {active, starting, errorMessage, start, stop, setPaused, readPhoto} =
    useCameraScanner(video, (raw) => emit('code', raw, 'camera'));
/** Marge du cadre de visée, cohérente avec la zone réellement analysée. */
const aimInset = `${((1 - VIEWFINDER_RATIO) / 2) * 100}%`;

//------------------ FUNCTIONS -------------------------------
/** Démarre la caméra ; le clic débloque aussi le son (exigence de Safari). */
function startCamera(): void {
  unlockAudio();
  void start();
}

/** Lit le code sur la photo choisie ou prise par l'agent. */
async function onPhoto(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  unlockAudio();
  readingPhoto.value = true;
  photoError.value = null;
  try {
    const raw = await readPhoto(file);
    if (raw) emit('code', raw, 'photo');
    else photoError.value = 'Aucun code lisible sur la photo. Rapprochez-vous, évitez les reflets.';
  } catch {
    photoError.value = 'Lecture de la photo impossible.';
  } finally {
    readingPhoto.value = false;
  }
}

//------------------ LIFECYCLE -------------------------------
watch(() => props.paused, setPaused, {immediate: true});
</script>

<template>
  <section class="viewfinder">
    <div class="viewport" :class="{live: active}">
      <video ref="video" playsinline muted autoplay />
      <div v-if="active" class="aim" :class="{held: paused}" :style="{inset: aimInset}" aria-hidden="true" />
      <div v-if="!active" class="viewport-idle">
        <button type="button" class="primary big" :disabled="starting" @click="startCamera">
          {{ starting ? 'Démarrage…' : 'Démarrer le scan' }}
        </button>
        <p class="mono small">DATA MATRIX · QR CODE · CODE-BARRES</p>
      </div>
    </div>

    <p v-if="errorMessage || photoError" class="form-error" role="alert">{{ errorMessage ?? photoError }}</p>

    <div class="row">
      <button v-if="active" type="button" class="secondary" @click="stop">Arrêter la caméra</button>
      <label class="button secondary" :class="{disabled: readingPhoto}">
        {{ readingPhoto ? 'Lecture…' : 'Photo du code' }}
        <input class="visually-hidden" type="file" accept="image/*" capture="environment" @change="onPhoto">
      </label>
    </div>
  </section>
</template>
