/**
 * Tableau de bord : compteurs, fins de disponibilité groupées avec un horizon réglable, salles en tuiles et
 * tuile du stock pour l'administrateur ; vue limitée à sa salle, avec l'aperçu des postes, pour le formateur.
 * RG : RG-07 (en stock = sans salle), RG-24 (alertes d'expiration groupées), RG-34 (salle du formateur),
 * RG-44 (vue des salles simple)
 */
import {addMaterial, addRoom} from './support/mock-data.ts';
import {chooseTeachingRoom, expect, test} from './support/fixtures.ts';

/** Date 'AAAA-MM-JJ' dans « days » jours, pour des fins de disponibilité qui ne vieillissent pas. */
function inDays(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() + days);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

test.describe('Administrateur', () => {
    test.beforeEach(async ({page, api}) => {
        const room20 = addRoom(api, 2, 20, 101);
        addMaterial(api, 'ECR-20J', 'SCREEN', api.rooms[0], inDays(20));
        addMaterial(api, 'ECR-70J', 'SCREEN', room20, inDays(70));
        addMaterial(api, 'STOCK-1', 'PERIPHERAL', null);
        await page.goto('/');
        await expect(page.locator('.stats .stat')).toHaveCount(4);
    });

    test('Compteurs du parc, dont le stock (RG-07)', async ({page}) => {
        await expect(page.locator('.stats .stat').nth(0)).toContainText('4');
        await expect(page.locator('.stats .stat').nth(0)).toContainText('Matériels suivis');
        await expect(page.locator('.stats .stat').nth(2)).toContainText('1');
        await expect(page.locator('.stats .stat').nth(2)).toContainText('En stock');
    });

    test('RG-24 : fins de disponibilité groupées, horizon de 30 ou 90 jours', async ({page}) => {
        const expiry = page.locator('.expiry-stat');
        await expect(expiry.locator('strong').first()).toHaveText('1');
        await expiry.getByRole('radio', {name: '90 j'}).click();
        await expect(expiry.locator('strong').first()).toHaveText('2');
    });

    test('RG-44 : une tuile par salle et une tuile « En stock » qui ouvre la liste filtrée', async ({page}) => {
        await expect(page.locator('.overview-tile')).toHaveCount(3);
        await page.locator('.overview-tile', {hasText: 'En stock'}).click();
        await expect(page).toHaveURL(/\/materiel\?emplacement=stock/);
        await expect(page.locator('.materials-card')).toContainText('STOCK-1');
        await expect(page.locator('.materials-card')).not.toContainText('ECR-20J');
    });
});

test.describe('Formateur dans la salle 10', () => {
    test.use({role: 'USER'});

    test.beforeEach(async ({page, api}) => {
        const room20 = addRoom(api, 2, 20, 101);
        addMaterial(api, 'ECR-S10', 'SCREEN', api.rooms[0]);
        addMaterial(api, 'UC-S20', 'UC', room20, inDays(10));
        await chooseTeachingRoom(page, 10);
    });

    test('RG-34 : deux compteurs, ceux de sa salle seulement', async ({page}) => {
        await expect(page.locator('.stats .stat')).toHaveCount(2);
        await expect(page.locator('.stats .stat').nth(0)).toContainText('2');
        await expect(page.locator('.expiry-stat strong').first()).toHaveText('0');
        await expect(page.locator('.overview-tile')).toHaveCount(0);
    });

    test('Aperçu des UC et de la supervision ; un poste cliqué s’ouvre dans la supervision', async ({page}) => {
        const dashboard = page.locator('.room-dashboard');
        await expect(dashboard).toContainText('UC-001');
        await expect(dashboard).not.toContainText('UC-S20');
        await expect(dashboard).toContainText('sch.martin');
        await dashboard.locator('a.preview-row', {hasText: 'sch.martin'}).click();
        await expect(page).toHaveURL(/\/supervision\?.*poste=11/);
        await expect(page.locator('.drawer')).toContainText('sch.martin');
    });

    test('Le logo ramène au tableau de bord sans perdre la salle', async ({page}) => {
        await page.locator('.main-menu .nav').getByText('Matériel', {exact: true}).click();
        await page.locator('.main-menu a.brand').click();
        await expect(page).toHaveURL(/\/\?salle=1$/);
    });
});
