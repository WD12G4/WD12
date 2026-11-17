/**
 * Apparence : contenu en pleine largeur par défaut, choix du thème, panneau latéral qui ne recouvre pas la page.
 * RG : RG-47 (thème d'affichage au choix de l'utilisateur)
 */
import {expect, test} from './support/fixtures.ts';

test('Pleine largeur par défaut, dès le chargement', async ({page}) => {
    await page.goto('/');
    await expect(page.locator('html')).toHaveAttribute('data-frame', 'open');
});

test('RG-47 : choisir le thème DSFR dans le menu d’apparence', async ({page}) => {
    await page.goto('/');
    await page.locator('.theme-switch button').click();
    await page.getByRole('radio', {name: /DSFR/}).click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dsfr');
});

test('Le panneau d’un poste ne recouvre pas la supervision', async ({page}) => {
    await page.goto('/supervision');
    await page.locator('.station-tile .station-open').first().click();
    await expect(page.locator('.drawer')).toBeVisible();
    await expect.poll(async () => {
        const card = await page.locator('.supervision-card').boundingBox();
        const drawer = await page.locator('.drawer').boundingBox();
        return card && drawer ? Math.round(card.x + card.width - drawer.x) : 1;
    }).toBeLessThanOrEqual(0);
});
