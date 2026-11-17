/**
 * Matériel : création sans date de fin, numéro de série unique, archivage, et droits selon le rôle.
 * RG : RG-01 (numéro de série unique), RG-04 (date de fin facultative), RG-10 (supprimer = archiver),
 * RG-16 (création et archivage : gestionnaire et administrateur)
 */
import {expect, test} from './support/fixtures.ts';

test.describe('Administrateur', () => {
    test('RG-04 : créer un matériel sans date de fin', async ({page, api}) => {
        await page.goto('/materiel/nouveau');
        await page.locator('#material-serial').fill('UC-NEUVE-01');
        await page.getByRole('button', {name: 'Créer le matériel'}).click();
        await expect(page.getByRole('heading', {name: 'Matériel UC-NEUVE-01'})).toBeVisible();
        const sent = api.requests.find(request => request.method === 'POST' && request.path === '/materials');
        expect(sent?.body).toMatchObject({serialNumber: 'UC-NEUVE-01', materialType: 'UC', endAvailabilityDate: null, roomId: null});
    });

    test('RG-01 : un numéro de série déjà enregistré est refusé, avec le message du serveur', async ({page}) => {
        await page.goto('/materiel/nouveau');
        await page.locator('#material-serial').fill('UC-001');
        await page.getByRole('button', {name: 'Créer le matériel'}).click();
        await expect(page.locator('.content')).toContainText('Un matériel avec ce numéro de série existe déjà.');
        await expect(page).toHaveURL(/\/materiel\/nouveau$/);
    });

    test('RG-10 : archiver après confirmation ; le matériel ne s’affiche plus que parmi les archivés', async ({page, api}) => {
        await page.goto('/materiel');
        const row = page.locator('.data-table tbody tr', {hasText: 'UC-001'});
        page.once('dialog', dialog => dialog.accept());
        await row.getByRole('button', {name: 'Archiver'}).click();
        await expect(row).toHaveCount(0);
        expect(api.materials[0]).toMatchObject({archived: true, room: null});
        await page.getByText('Afficher les archivés').click();
        await expect(page.locator('.data-table tbody tr', {hasText: 'UC-001'})).toBeVisible();
    });
});

test.describe('RG-16 : droits sur le matériel', () => {
    for (const [role, canManage] of [['MANAGER', true], ['USER', false]] as const) {
        test.describe(role, () => {
            test.use({role});
            test(`création ${canManage ? 'possible' : 'impossible'}`, async ({page}) => {
                await page.goto(role === 'USER' ? '/materiel?salle=1' : '/materiel');
                await expect(page.locator('.materials-card')).toBeVisible();
                await expect(page.getByRole('link', {name: '+ Nouveau matériel'})).toHaveCount(canManage ? 1 : 0);
                await expect(page.getByRole('button', {name: 'Archiver'})).toHaveCount(canManage ? 1 : 0);
            });
        });
    }
});
