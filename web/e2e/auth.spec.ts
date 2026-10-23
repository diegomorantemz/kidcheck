import { expect, test } from '@playwright/test';
import { cerrarSesion, iniciarSesion } from './helpers';

test.describe('autenticación y navegación por rol', () => {
	test('admin inicia sesión y ve todas las secciones', async ({ page }) => {
		await iniciarSesion(page, 'admin1');

		await expect(page.getByRole('link', { name: 'Reportes' })).toBeVisible();
		await expect(page.getByRole('link', { name: 'Configuración' })).toBeVisible();
		await expect(page.getByRole('link', { name: 'Auditoría' })).toBeVisible();
		await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
	});

	test('auxiliar ve reportes pero no configuración ni auditoría', async ({ page }) => {
		await iniciarSesion(page, 'auxiliar1');

		await expect(page.getByRole('link', { name: 'Reportes' })).toBeVisible();
		await expect(page.getByRole('link', { name: 'Configuración' })).toHaveCount(0);
		await expect(page.getByRole('link', { name: 'Auditoría' })).toHaveCount(0);
	});

	test('vigilante no ve reportes ni administración', async ({ page }) => {
		await iniciarSesion(page, 'vigilante1');

		await expect(page.getByRole('link', { name: 'Reportes' })).toHaveCount(0);
		await expect(page.getByRole('link', { name: 'Configuración' })).toHaveCount(0);
		await expect(page.getByRole('link', { name: 'Auditoría' })).toHaveCount(0);
	});

	test('cerrar sesión vuelve al login', async ({ page }) => {
		await iniciarSesion(page, 'admin1');
		await cerrarSesion(page);
		await expect(page.getByRole('heading', { name: 'Ingresa al panel' })).toBeVisible();
	});

	test('credenciales inválidas muestran un error', async ({ page }) => {
		await page.goto('/login');
		await page.getByLabel('Usuario').fill('admin1');
		await page.getByLabel('Contraseña').fill('clave-incorrecta');
		await page.getByRole('button', { name: 'Ingresar' }).click();

		await expect(page.getByRole('alert')).toBeVisible();
		await expect(page).toHaveURL(/\/login/);
	});
});
