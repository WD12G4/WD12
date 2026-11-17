/**
 * Remontées : le formateur signale un problème et suit ses remontées, le gestionnaire est prévenu par le
 * compteur du menu et les traite.
 * RG : RG-21 (contenu d'une remontée), RG-23 (gestionnaire prévenu dans l'application), RG-36 (traitement),
 * RG-37 (le formateur voit que sa remontée est traitée), RG-38 (le formateur ne voit que ses remontées)
 */
import type {MockState, UserJson} from './support/mock-data.ts';
import {chooseTeachingRoom, expect, test} from './support/fixtures.ts';

/** Ajoute une remontée faite par « author » sur l'UC de la salle 10. */
function addReport(api: MockState, author: UserJson, description: string, handled = false): void {
    api.reports.push({id: api.reports.length + 1, version: 0, description, reportDateTime: '2026-11-15T09:00:00', user: author,
        material: api.materials[0], handleDateTime: handled ? '2026-11-15T10:00:00' : null, handled});
}

const userAccount = (api: MockState, username: string): UserJson => api.users.find(user => user.username === username) ?? api.me;

test.describe('Formateur', () => {
    test.use({role: 'USER'});

    test('RG-21 : signaler un problème avec son type et une description', async ({page, api}) => {
        await chooseTeachingRoom(page, 10);
        await page.locator('.main-menu .nav').getByText('Signaler un problème').click();
        await page.locator('#report-material').click();
        await page.getByRole('option', {name: /UC-001/}).click();
        await page.locator('#report-description').fill('Écran noir au démarrage');
        await page.getByRole('button', {name: 'Envoyer la remontée'}).click();
        await expect(page.getByRole('heading', {name: 'Mes remontées'})).toBeVisible();
        expect(api.reports[0]?.description).toBe('Hors service — Écran noir au démarrage');
        await expect(page.locator('.data-table tbody tr')).toHaveCount(1);
        await expect(page.locator('.data-table tbody tr')).toContainText('À traiter');
    });

    test('RG-37 et RG-38 : seulement ses remontées, avec leur état', async ({page, api}) => {
        addReport(api, userAccount(api, 'user'), 'Hors service', true);
        addReport(api, userAccount(api, 'manager'), 'Fonctionne mal');
        await chooseTeachingRoom(page, 10);
        await page.locator('.main-menu .nav').getByText('Remontées', {exact: true}).click();
        await expect(page.locator('.data-table tbody tr')).toHaveCount(1);
        await expect(page.locator('.data-table tbody tr')).toContainText('Traitée');
        expect(api.requests.some(request => request.path === '/reports' && request.query.includes('userId=2'))).toBe(true);
        await expect(page.getByRole('button', {name: 'Marquer comme traitée'})).toHaveCount(0);
    });
});

test.describe('Gestionnaire', () => {
    test.use({role: 'MANAGER'});

    test('RG-23 et RG-36 : prévenu par le compteur du menu, il traite la remontée', async ({page, api}) => {
        addReport(api, userAccount(api, 'user'), 'Hors service');
        await page.goto('/');
        const counter = page.locator('.main-menu .nav-count');
        await expect(counter).toHaveText(/^1\b/);
        await page.locator('.main-menu .nav').getByText('Remontées', {exact: true}).click();
        await expect(page.locator('.data-table tbody tr')).toContainText('user');
        await page.getByRole('button', {name: 'Marquer comme traitée'}).click();
        await expect(page.locator('.data-table tbody tr')).toContainText('Traitée');
        await expect(counter).toHaveCount(0);
        expect(api.reports[0]?.handled).toBe(true);
    });
});
