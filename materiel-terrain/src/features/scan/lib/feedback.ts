/**
 * Retour sonore après une lecture, pour enchaîner les scans sans regarder l'écran.
 * Web Audio plutôt que des fichiers son ; Safari iOS ne connaît pas navigator.vibrate.
 * Le contexte audio doit être débloqué par un geste de l'utilisateur (bouton « Démarrer le scan »).
 */

/** Tonalité courte : aiguë = conforme, double grave = action attendue. */
type Tone = 'ok' | 'alert';

//------------------ DATAS -----------------------------------
let context: AudioContext | null = null;

//------------------ FUNCTIONS -------------------------------
/** Débloque le son. À appeler dans un gestionnaire de clic. */
export function unlockAudio(): void {
    try {
        context ??= new AudioContext();
        void context.resume();
    } catch {
        context = null;
    }
}

/**
 * Joue un bip.
 * @param frequency fréquence en hertz
 * @param start décalage en secondes
 */
function beep(frequency: number, start: number): void {
    if (context === null) return;
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    const at = context.currentTime + start;
    oscillator.frequency.value = frequency;
    gain.gain.setValueAtTime(0.2, at);
    gain.gain.exponentialRampToValueAtTime(0.001, at + 0.12);
    oscillator.connect(gain).connect(context.destination);
    oscillator.start(at);
    oscillator.stop(at + 0.13);
}

/**
 * Signale le résultat d'une lecture.
 * @param tone 'ok' si rien à faire, 'alert' si l'agent doit agir
 */
export function playTone(tone: Tone): void {
    if (tone === 'ok') {
        beep(1320, 0);
    } else {
        beep(440, 0);
        beep(440, 0.18);
    }
}
