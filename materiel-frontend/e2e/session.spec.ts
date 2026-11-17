/**
 * Session et droits : menu selon le rôle, rôle affiché, outil de démonstration (changer de rôle sans se
 * reconnecter, choix gardé au rechargement), déconnexion.
 * RG : RG-32 (comptes : administrateur), RG-42 (rôle affiché), RG-48 (démonstration : changer de rôle sans se reconnecter)
 */
import {expect, test} from './support/fixtures.ts';

test.describe('Menu selon le rôle', () => {
    for (const [role, accounts] of [['ADMIN', true], ['MANAGER', false]] as const) {
        test.describe(role, () => {
            test.use({role});
            test(`RG-32 : « Comptes » ${accounts ? 'affiché' : 'absent'}`, async ({page}) => {
                await page.goto('/');
                const nav = page.locator('.main-menu .nav');
                await expect(nav.getByText('Matériel', {exact: true})).toBeVisible();
                await expect(nav.getByText('Comptes', {exact: true})).toHaveCount(accounts ? 1 : 0);
            });
        });
    }
});

test.describe('Administrateur', () => {
    test('RG-42 et RG-48 : changer de rôle sans se reconnecter, puis revenir à son compte', async ({page, api}) => {
        await page.goto('/');
        await expect(page.locator('.user-card')).toContainText('admin');
        await expect(page.locator('.user-card')).toContainText('Administrateur');
        await page.locator('#dev-user-switch').click();
        await page.getByRole('option', {name: 'Gestionnaire'}).click();
        await expect(page.locator('.user-card')).toContainText('manager');
        await expect(page.locator('.main-menu .nav').getByText('Comptes', {exact: true})).toHaveCount(0);
        await page.reload();
        await expect(page.locator('.user-card')).toContainText('manager');
        await page.locator('#dev-user-switch').click();
        await page.getByRole('option', {name: 'Administrateur'}).click();
        await expect(page.locator('.user-card')).toContainText('admin');
        expect(await page.evaluate(() => sessionStorage.getItem('gestion-parc.demo-account'))).toBeNull();
        expect(api.requests.filter(request => request.path === '/auth/me/').length).toBeGreaterThanOrEqual(3);
    });

    test('Déconnexion : retour à la page de connexion', async ({page}) => {
        await page.goto('/');
        await page.getByRole('button', {name: 'Se déconnecter'}).click();
        await expect(page).toHaveURL(/\/connexion$/);
    });
});
