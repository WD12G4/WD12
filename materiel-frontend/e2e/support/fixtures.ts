/**
 * Base des tests navigateur : chaque test reçoit une page déjà branchée sur la fausse API (mock-api.ts),
 * avec le rôle choisi par `test.use({role: …})` (administrateur par défaut). Le test échoue si la page
 * lève une erreur JavaScript.
 * Utilisé par : tous les fichiers *.spec.ts
 */
import {expect, type Page, test as base} from '@playwright/test';
import {installMockBackend} from './mock-api.ts';
import type {MockState, Role} from './mock-data.ts';

export const test = base.extend<{role: Role; api: MockState}>({
    role: ['ADMIN', {option: true}],
    api: [async ({page, role}, use) => {
        const errors: string[] = [];
        page.on('pageerror', error => errors.push(error.message));
        await use(await installMockBackend(page, role));
        expect(errors, 'erreurs JavaScript dans la page').toEqual([]);
    }, {auto: true}],
});

export {expect};

/**
 * Formateur : ouvre l'application et choisit sa salle de cours, choix obligatoire après la connexion (RG-33).
 * @param page page du test
 * @param roomNumber numéro de la salle choisie
 */
export async function chooseTeachingRoom(page: Page, roomNumber: number): Promise<void> {
    await page.goto('/');
    await expect(page).toHaveURL(/\/choix-salle/);
    await page.locator('.room-tile', {hasText: String(roomNumber)}).click();
    await expect(page).toHaveURL(/salle=\d+/);
}
