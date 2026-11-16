// MAQUETTE : composition des postes simulés de chaque salle.
/**
 * Chaque salle a ses propres postes simulés : leur nombre (2 à 5), leurs noms, leurs stagiaires et
 * leur système varient d'une salle à l'autre. Le tirage dépend seulement de la salle, donc une même
 * salle retrouve les mêmes postes à chaque chargement de la page.
 * Utilisé par : simulation.services.mock.ts
 */

/** Stagiaires fictifs (identifiants de session) ; un stagiaire n'apparaît qu'une fois par salle. */
const TRAINEE_POOL = [
    'j.martin', 'l.bernard', 'a.petit', 'm.durand', 'c.moreau', 't.laurent', 's.simon', 'n.michel', 'e.lefebvre',
    'h.leroy', 'p.roux', 'r.david', 'g.bertrand', 'f.morel', 'k.fournier', 'o.girard', 'b.andre', 'v.mercier',
];
/** Système et image possibles d'un poste simulé (identifiants de simulation-catalog.mock.ts). */
const CONFIG_POOL: [osId: string, profileId: string][] = [
    ['windows-11', 'dev'], ['windows-11', 'office'], ['windows-11', 'network'], ['ubuntu-24', 'dev'],
    ['ubuntu-24', 'office'], ['debian-12', 'network'], ['debian-12', 'dev'],
];
const MIN_STATIONS = 2;
const MAX_STATIONS = 5;

/** Poste simulé prévu pour une salle. */
interface StationPlan {
    /** Nom affiché, ex. « PC-1201 » (salle 12, poste 01). */
    name: string;
    hostname: string;
    trainee: string;
    osId: string;
    profileId: string;
}

/**
 * Générateur pseudo-aléatoire reproductible (mulberry32) : même graine, même suite de nombres.
 * @param seed graine (ici, l'identifiant de la salle)
 * @returns une fonction qui renvoie un nombre entre 0 (inclus) et 1 (exclu)
 */
function seededRandom(seed: number): () => number {
    let state = seed >>> 0;
    return () => {
        state = (state + 0x6d2b79f5) >>> 0;
        let t = Math.imul(state ^ (state >>> 15), 1 | state);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

/**
 * Postes simulés d'une salle : nombre, noms, stagiaires (tous différents) et configurations.
 * @param roomId identifiant de la salle
 */
export function stationPlans(roomId: number): StationPlan[] {
    const random = seededRandom(roomId * 7919);
    const count = MIN_STATIONS + Math.floor(random() * (MAX_STATIONS - MIN_STATIONS + 1));
    const trainees = [...TRAINEE_POOL].sort(() => random() - 0.5);
    return Array.from({length: count}, (_, index) => {
        const n = String(index + 1).padStart(2, '0');
        const [osId, profileId] = CONFIG_POOL[Math.floor(random() * CONFIG_POOL.length)] ?? CONFIG_POOL[0];
        return {name: `PC-${roomId}${n}`, hostname: `pc-${roomId}${n}`, trainee: trainees[index] ?? 'stagiaire', osId, profileId};
    });
}
