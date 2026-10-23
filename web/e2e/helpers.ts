import { expect, type Page } from '@playwright/test';

export const DEMO_PASSWORD = 'KidCheck2026!';

export async function iniciarSesion(page: Page, usuario = 'admin1') {
	await page.goto('/login');
	await page.getByLabel('Usuario').fill(usuario);
	await page.getByLabel('Contraseña').fill(DEMO_PASSWORD);
	await page.getByRole('button', { name: 'Ingresar' }).click();
	await expect(page).toHaveURL(/\/dashboard/);
	await expect(page.getByRole('heading', { name: 'Panel del día' })).toBeVisible();
}

export async function cerrarSesion(page: Page) {
	await page.getByRole('button', { name: 'Salir' }).click();
	await expect(page).toHaveURL(/\/login/);
}
