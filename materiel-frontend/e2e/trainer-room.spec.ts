/**
 * Formateur : choix obligatoire de sa salle de cours après la connexion, puis accès limité à cette salle
 * dans tout le front. L'administrateur, lui, voit tout le parc.
 * RG : RG-33 (choix de la salle), RG-34 (accès limité à la salle choisie), RG-46 (salle imposée dans une remontée)
 */
import {addMaterial, addRoom, type MockState} from './support/mock-data.ts';
import {chooseTeachingRoom, expect, test} from './support/fixtures.ts';

/** Deux salles dans deux bâtiments : salle 10 (bât. 101) et salle 20 (bât. 202), plus du stock. */
function seedTwoRooms(api: MockState): void {
    const other = addRoom(api, 2, 20, 202);
    addMaterial(api, 'ECRAN-S10', 'SCREEN', api.rooms[0]);
    addMaterial(api, 'UC-S20', 'UC', other);
    addMaterial(api, 'STOCK-1', 'PERIPHERAL', null);
}

test.describe('Formateur dans la salle 10', () => {
    test.use({role: 'USER'});

    test.beforeEach(async ({page, api}) => {
        seedTwoRooms(api);
        await chooseTeachingRoom(page, 10);
    });

    test('RG-33 : la salle choisie reste dans l’adresse de page en page', async ({page}) => {
        await page.locator('.main-menu .nav').getByText('Matériel', {exact: true}).click();
        await expect(page).toHaveURL(/\/materiel\?salle=1/);
        await expect(page.locator('.room-switch')).toContainText('Salle 10');
    });

    test('RG-34 : matériel, salles et bâtiments limités à sa salle', async ({page}) => {
        await page.locator('.main-menu .nav').getByText('Matériel', {exact: true}).click();
        const materials = page.locator('.materials-card');
        await expect(materials).toContainText('ECRAN-S10');
        await expect(materials).not.toContainText('UC-S20');
        await expect(materials).not.toContainText('STOCK-1');
        await page.locator('.main-menu .nav').getByText('Salles', {exact: true}).click();
        await expect(page.locator('.data-table tbody tr')).toHaveCount(1);
        await expect(page.locator('.content')).toContainText('Votre salle de cours');
        await page.locator('.main-menu .nav').getByText('Bâtiments', {exact: true}).click();
        await expect(page.locator('.content')).toContainText('Bâtiment 101');
        await expect(page.locator('.content')).not.toContainText('Bâtiment 202');
    });

    test('RG-34 : la fiche d’une autre salle ou d’un autre matériel est refusée, même par l’adresse', async ({page, api}) => {
        await page.goto('/salles/2?salle=1');
        await expect(page.getByRole('heading', {name: 'Salle non accessible'})).toBeVisible();
        expect(api.requests.some(request => request.path === '/rooms/2')).toBe(false);
        await page.goto('/materiel/3?salle=1');
        await expect(page.getByRole('heading', {name: 'Matériel non accessible'})).toBeVisible();
        await expect(page.locator('.content')).not.toContainText('UC-S20');
        await page.getByText('Le matériel de ma salle').click();
        await expect(page).toHaveURL(/\/materiel\?salle=1/);
    });

    test('RG-46 : la salle est imposée dans « Signaler un problème »', async ({page, api}) => {
        await page.goto('/remontees/nouvelle?salle=1');
        await expect(page.locator('#report-room')).toBeDisabled();
        await expect(page.locator('#report-room')).toContainText('Salle 10');
        await page.locator('#report-material').click();
        await expect(page.getByRole('option')).toHaveText(['Unité centrale – UC-001', 'Écran – ECRAN-S10']);
        await page.getByRole('option', {name: /ECRAN-S10/}).click();
        await page.getByRole('button', {name: 'Envoyer la remontée'}).click();
        await expect(page).toHaveURL(/\/remontees\?salle=1/);
        expect(api.reports[0]?.material.serialNumber).toBe('ECRAN-S10');
    });

    test('Page réservée à un autre rôle : retour au tableau de bord sans perdre sa salle', async ({page}) => {
        await page.goto('/comptes?salle=1');
        await expect(page).toHaveURL(/\/\?salle=1$/);
        await expect(page.locator('body')).toContainText('Vous n\'avez pas accès à cette page.');
    });
});

test.describe('Administrateur', () => {
    test('RG-34 ne s’applique pas : tout le parc, sans choix de salle', async ({page, api}) => {
        seedTwoRooms(api);
        await page.goto('/salles');
        await expect(page.locator('.data-table tbody tr')).toHaveCount(2);
        await page.goto('/materiel/3');
        await expect(page.getByRole('heading', {name: 'Matériel UC-S20'})).toBeVisible();
        await page.goto('/remontees/nouvelle');
        await expect(page.locator('#report-room')).toBeEnabled();
    });
});
