/**
 * Supervision : postes de la salle avec la session ouverte, applications ouvertes, arrêt après confirmation,
 * journal avec l'auteur, liste noire réservée à l'administrateur, postes simulés propres à chaque salle.
 * RG : RG-25 (applications des postes), RG-26 (arrêt), RG-27 (liste noire : administrateur), RG-28
 * (postes simulés pour la masterisation), RG-30 (session ouverte et nom d'hôte), RG-39 (journal des arrêts)
 */
import {addRoom} from './support/mock-data.ts';
import {chooseTeachingRoom, expect, test} from './support/fixtures.ts';

test.describe('Administrateur', () => {
    test.beforeEach(async ({page}) => {
        await page.goto('/supervision');
        await expect(page.locator('.station-tile').first()).toBeVisible();
    });

    test('RG-25 et RG-30 : postes avec la session ouverte, le nom d’hôte et les applications', async ({page}) => {
        const tile = page.locator('.station-tile', {hasText: 'sch.martin'});
        await expect(tile).toContainText('win-01');
        await tile.locator('.station-open').click();
        await expect(page.locator('.drawer .process-list li')).toHaveCount(2);
        await expect(page.locator('.drawer')).toContainText('Chrome');
    });

    test('RG-26 : arrêt seulement après confirmation', async ({page, api}) => {
        await page.locator('.station-tile', {hasText: 'sch.martin'}).locator('.station-open').click();
        const firstApp = page.locator('.drawer .process-list li').first();
        page.once('dialog', dialog => dialog.dismiss());
        await firstApp.getByRole('button', {name: 'Arrêter'}).click();
        await expect(page.locator('.drawer .process-list li')).toHaveCount(2);
        expect(api.requests.some(request => request.path === '/supervision/kill')).toBe(false);
        page.once('dialog', dialog => dialog.accept());
        await firstApp.getByRole('button', {name: 'Arrêter'}).click();
        await expect(page.locator('.drawer .process-list li')).toHaveCount(1);
        expect(api.requests.some(request => request.path === '/supervision/kill')).toBe(true);
    });

    test('RG-39 : le journal indique qui a arrêté l’application, sur un vrai poste et un poste simulé', async ({page}) => {
        page.on('dialog', dialog => dialog.accept());
        await page.locator('.station-tile', {hasText: 'sch.martin'}).locator('.station-open').click();
        await page.locator('.drawer .process-list li').first().getByRole('button', {name: 'Arrêter'}).click();
        await page.keyboard.press('Escape');
        await page.locator('.station-tile:has(.sim-badge)').first().locator('.station-open').click();
        await page.locator('.drawer .process-list li').first().getByRole('button', {name: 'Arrêter'}).click();
        await page.keyboard.press('Escape');
        await page.locator('.segmented').getByText('Journal', {exact: true}).click();
        await expect(page.locator('.journal-item', {hasText: 'Application arrêtée par admin sur SN-WIN-01'})).toHaveCount(1);
        await expect(page.locator('.journal-item', {hasText: /Application arrêtée par admin sur PC-/})).toHaveCount(1);
    });

    test('RG-27 : l’administrateur peut interdire une application', async ({page}) => {
        await page.locator('.station-tile', {hasText: 'sch.martin'}).locator('.station-open').click();
        await expect(page.locator('.drawer .process-list li').first().getByRole('button', {name: 'Interdire'})).toBeVisible();
        await expect(page.locator('.segmented').getByText('Liste noire', {exact: true})).toBeVisible();
    });

    test('RG-28 : des postes simulés différents dans chaque salle, identiques au rechargement', async ({page, api}) => {
        addRoom(api, 2, 20, 101);
        await page.reload();
        // Le stagiaire et le nom du poste ; les applications ouvertes, elles, changent à chaque chargement.
        const simulated = async () => (await page.locator('.station-tile:has(.sim-badge)').allInnerTexts())
            .map(text => text.split('\n').filter(Boolean).slice(0, 3).join(' '));
        await page.locator('.supervision-toolbar .segmented').getByText('Salle 10').click();
        await expect(page.locator('.station-tile:has(.sim-badge)').first()).toBeVisible();
        const room10 = await simulated();
        await page.locator('.supervision-toolbar .segmented').getByText('Salle 20').click();
        await expect.poll(simulated).not.toEqual(room10);
        const room20 = await simulated();
        await page.reload();
        await page.locator('.supervision-toolbar .segmented').getByText('Salle 20').click();
        await expect.poll(simulated).toEqual(room20);
    });
});

test.describe('Formateur dans la salle 10', () => {
    test.use({role: 'USER'});

    test('RG-27 : le formateur arrête une application mais ne peut pas l’interdire', async ({page}) => {
        await chooseTeachingRoom(page, 10);
        await page.locator('.main-menu .nav').getByText('Supervision', {exact: true}).click();
        await page.locator('.station-tile', {hasText: 'sch.martin'}).locator('.station-open').click();
        const firstApp = page.locator('.drawer .process-list li').first();
        await expect(firstApp.getByRole('button', {name: 'Arrêter'})).toBeVisible();
        await expect(firstApp.getByRole('button', {name: 'Interdire'})).toHaveCount(0);
        await expect(page.locator('.segmented').getByText('Liste noire', {exact: true})).toHaveCount(0);
    });
});
