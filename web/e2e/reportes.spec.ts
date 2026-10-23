import { expect, test } from '@playwright/test';
import { iniciarSesion } from './helpers';

test.describe('reportes, auditoría y configuración', () => {
	test('reportes muestra R01/R02 y descarga el CSV', async ({ page }) => {
		await iniciarSesion(page, 'admin1');
		await page.getByRole('link', { name: 'Reportes' }).click();

		await expect(page.getByRole('heading', { name: 'Reportes' })).toBeVisible();
		await expect(page.getByRole('heading', { name: 'Composición del día' })).toBeVisible();
		await expect(page.getByRole('heading', { name: 'Asistencia mensual' })).toBeVisible();
		await expect(page.getByRole('heading', { name: 'Detalle por estudiante' })).toBeVisible();
		await expect(page.locator('canvas')).toHaveCount(2);

		const descarga = page.waitForEvent('download');
		await page.getByRole('button', { name: 'Descargar CSV' }).click();
		const archivo = await descarga;
		expect(archivo.suggestedFilename()).toMatch(/^asistencia-\d{4}-\d{2}\.csv$/);
	});

	test('auditoría lista acciones y filtra por entidad', async ({ page }) => {
		await iniciarSesion(page, 'admin1');
		await page.getByRole('link', { name: 'Auditoría' }).click();

		await expect(page.getByRole('heading', { name: 'Auditoría' })).toBeVisible();
		await expect(page.locator('table tbody tr').first()).toBeVisible();

		await page.getByLabel('Entidad').selectOption('reporte');
		await page.getByRole('button', { name: 'Filtrar' }).click();
		const filas = page.locator('table tbody tr');
		await expect(filas.first()).toContainText('reporte');
	});

	test('configuración muestra reglas, secciones y calendario', async ({ page }) => {
		await iniciarSesion(page, 'admin1');
		await page.getByRole('link', { name: 'Configuración' }).click();

		await expect(page.getByRole('heading', { name: 'Configuración' })).toBeVisible();
		await expect(page.getByRole('heading', { name: 'Reglas de alerta' })).toBeVisible();
		await expect(page.getByRole('heading', { name: 'Secciones y horarios' })).toBeVisible();
		await expect(page.getByRole('heading', { name: 'Calendario escolar' })).toBeVisible();
		await expect(page.getByLabel('Umbral de reincidencia')).toHaveValue('3');
	});

	test('auxiliar no puede abrir el CSV desde la interfaz', async ({ page }) => {
		await iniciarSesion(page, 'auxiliar1');
		await page.getByRole('link', { name: 'Reportes' }).click();

		await expect(page.getByRole('heading', { name: 'Reportes' })).toBeVisible();
		await expect(page.getByRole('button', { name: 'Descargar CSV' })).toHaveCount(0);
	});
});
