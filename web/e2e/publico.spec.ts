import { expect, test } from '@playwright/test';

test.describe('justificación pública', () => {
	test('el enlace de la semilla abre sin login', async ({ page }) => {
		await page.goto('/j/just-abc');

		await expect(page.getByRole('heading', { name: 'Justificar ausencia' })).toBeVisible();
		await expect(page.getByText('Mateo Silva Cordova')).toBeVisible();
		await expect(page.getByText('Ya registramos una justificación para esta ausencia.')).toBeVisible();
	});

	test('un token inexistente muestra un enlace no disponible', async ({ page }) => {
		await page.goto('/j/token-inexistente');

		await expect(page.getByRole('heading', { name: 'Enlace no disponible' })).toBeVisible();
	});
});
