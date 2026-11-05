/**
 * Caméra arrière et boucle de lecture : une trame toutes les FRAME_INTERVAL_MS, recadrée sur le viseur.
 * La lecture se fait dans le navigateur (ZXing en WebAssembly) : rien n'est envoyé au serveur.
 * Utilisé par : features/scan/components/camera-viewfinder.vue
 */
import {onBeforeUnmount, ref, type Ref} from 'vue';
import {cameraErrorMessage, INSECURE_CONTEXT_MESSAGE} from '../lib/camera-errors.ts';

/** Intervalle entre deux trames analysées. */
const FRAME_INTERVAL_MS = 150;
/** Un code resté dans le champ n'est relu qu'après en être sorti pendant ce délai. */
const REPEAT_DELAY_MS = 3000;
/** Côté du carré analysé au centre de l'image, en proportion du petit côté (= cadre affiché). */
export const VIEWFINDER_RATIO = 0.7;

/** Module de lecture, chargé au premier usage. */
type Reader = typeof import('../lib/barcode-reader.ts');

/**
 * Pilote la caméra et appelle onCode à chaque nouveau code lu.
 * @param video élément <video> du viseur
 * @param onCode traitement d'un code lu
 */
export function useCameraScanner(video: Ref<HTMLVideoElement | null>, onCode: (raw: string) => void) {
    const active = ref(false);
    const starting = ref(false);
    const errorMessage = ref<string | null>(null);
    const canvas = document.createElement('canvas');
    let paused = false;
    let stream: MediaStream | null = null;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let reader: Reader | null = null;
    let last = {code: '', at: 0};

    /** Copie le carré central de la trame dans le canvas, à pleine résolution (petits codes). */
    function cropFrame(source: HTMLVideoElement): HTMLCanvasElement {
        const side = Math.round(Math.min(source.videoWidth, source.videoHeight) * VIEWFINDER_RATIO);
        canvas.width = side;
        canvas.height = side;
        canvas.getContext('2d', {willReadFrequently: true})?.drawImage(source,
            (source.videoWidth - side) / 2, (source.videoHeight - side) / 2, side, side, 0, 0, side, side);
        return canvas;
    }

    /** Analyse une trame et signale un code nouveau (ou revenu dans le champ après REPEAT_DELAY_MS). */
    async function readFrame(): Promise<void> {
        const source = video.value;
        if (paused || !reader || !source || source.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) return;
        const raw = await reader.readCode(cropFrame(source));
        if (raw === null) return;
        const isNew = raw !== last.code || Date.now() - last.at > REPEAT_DELAY_MS;
        last = {code: raw, at: Date.now()};
        if (isNew && !paused) onCode(raw);
    }

    function loop(): void {
        timer = setTimeout(async () => {
            try {
                await readFrame();
            } catch {
                // Trame illisible : on passe à la suivante.
            }
            if (active.value) loop();
        }, FRAME_INTERVAL_MS);
    }

    function stop(): void {
        clearTimeout(timer);
        stream?.getTracks().forEach((track) => track.stop());
        stream = null;
        if (video.value) video.value.srcObject = null;
        active.value = false;
    }

    /** Démarre la caméra arrière en haute définition (les Data Matrix d'étiquette sont petits). */
    async function start(): Promise<void> {
        errorMessage.value = null;
        if (!navigator.mediaDevices?.getUserMedia) {
            errorMessage.value = INSECURE_CONTEXT_MESSAGE;
            return;
        }
        starting.value = true;
        try {
            reader ??= await import('../lib/barcode-reader.ts');
            stream = await navigator.mediaDevices.getUserMedia({
                audio: false,
                video: {facingMode: {ideal: 'environment'}, width: {ideal: 1920}, height: {ideal: 1080}},
            });
            if (!video.value) throw new Error('viseur absent');
            video.value.srcObject = stream;
            await video.value.play();
            active.value = true;
            loop();
        } catch (error) {
            stop();
            errorMessage.value = cameraErrorMessage(error);
        } finally {
            starting.value = false;
        }
    }

    /** Suspend la lecture (résultat affiché) sans couper la caméra. */
    function setPaused(value: boolean): void {
        paused = value;
        // À la reprise, le code encore dans le champ n'est pas relu tout de suite.
        if (!value) last = {...last, at: Date.now()};
    }

    /** Lit un code sur une photo de l'appareil photo (meilleure mise au point sur les très petits codes). */
    async function readPhoto(file: Blob): Promise<string | null> {
        reader ??= await import('../lib/barcode-reader.ts');
        return reader.readCode(file);
    }

    // iOS coupe la caméra quand l'application passe en arrière-plan : on la libère proprement.
    const onVisibilityChange = () => {
        if (document.hidden) stop();
    };
    document.addEventListener('visibilitychange', onVisibilityChange);
    onBeforeUnmount(() => {
        stop();
        document.removeEventListener('visibilitychange', onVisibilityChange);
    });

    return {active, starting, errorMessage, start, stop, setPaused, readPhoto};
}
