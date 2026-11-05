/**
 * Lecture de codes dans une image (trame vidéo, photo) avec ZXing-C++ compilé en WebAssembly.
 * Safari iOS ne fournit pas l'API BarcodeDetector : on utilise l'implémentation du paquet barcode-detector.
 * Le fichier .wasm est servi par l'application elle-même (et mis en cache par le service worker) :
 * pas d'appel au CDN par défaut, le scan fonctionne hors réseau.
 * Module chargé à la demande (import dynamique) : ~1 Mo qu'on ne télécharge qu'au premier scan.
 */
import {BarcodeDetector, type BarcodeFormat, prepareZXingModule} from 'barcode-detector/ponyfill';
import wasmUrl from 'zxing-wasm/reader/zxing_reader.wasm?url';

/** Formats recherchés : Data Matrix (étiquettes du parc), QR code et codes-barres linéaires courants. */
const FORMATS: BarcodeFormat[] = ['data_matrix', 'qr_code', 'code_128', 'code_39'];

prepareZXingModule({
    overrides: {
        locateFile: (path: string, prefix: string) => (path.endsWith('.wasm') ? wasmUrl : prefix + path),
    },
});

//------------------ DATAS -----------------------------------
let detector: BarcodeDetector | null = null;

//------------------ FUNCTIONS -------------------------------
/**
 * Lit le premier code trouvé dans l'image.
 * @param image canvas (trame vidéo recadrée) ou fichier image (photo)
 * @returns le texte du code, ou null si aucun code n'est lisible
 */
export async function readCode(image: HTMLCanvasElement | Blob): Promise<string | null> {
    detector ??= new BarcodeDetector({formats: FORMATS});
    const codes = await detector.detect(image);
    return codes[0]?.rawValue ?? null;
}
