<script lang="ts">
	import { onDestroy, onMount } from 'svelte';
	import { tick } from 'svelte';
	import Chart from 'chart.js/auto';
	import { api, mensajeDeError } from '$lib/api';
	import type { Me, ReporteDiario, ReporteMensual, Seccion } from '$lib/types';

	let me = $state<Me | null>(null);
	let fecha = $state(fechaLocalLima());
	let periodo = $state(periodoLocalLima());
	let seccionId = $state<number | ''>('');
	let secciones = $state<Seccion[]>([]);

	let diario = $state<ReporteDiario | null>(null);
	let mensual = $state<ReporteMensual | null>(null);
	let cargandoDiario = $state(true);
	let cargandoMensual = $state(true);
	let error = $state('');

	let canvasDiario = $state<HTMLCanvasElement | null>(null);
	let canvasMensual = $state<HTMLCanvasElement | null>(null);
	let graficoDiario: Chart | null = null;
	let graficoMensual: Chart | null = null;

	function fechaLocalLima() {
		return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(new Date());
	}

	function periodoLocalLima() {
		return fechaLocalLima().slice(0, 7);
	}

	onMount(async () => {
		try {
			me = await api<Me>('/auth/me');
		} catch {
			// el layout ya redirige si no hay sesión
		}
		try {
			secciones = await api<Seccion[]>('/secciones');
		} catch {
			// el filtro es opcional
		}
		await Promise.all([cargarDiario(), cargarMensual()]);
	});

	onDestroy(() => {
		graficoDiario?.destroy();
		graficoMensual?.destroy();
	});

	async function cargarDiario() {
		cargandoDiario = true;
		error = '';
		try {
			const parametros = new URLSearchParams({ fecha });
			if (seccionId !== '') parametros.set('seccionId', String(seccionId));
			diario = await api<ReporteDiario>(`/reportes/diario?${parametros}`);
			cargandoDiario = false;
			await tick();
			dibujarDiario();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargandoDiario = false;
		}
	}

	async function cargarMensual() {
		cargandoMensual = true;
		error = '';
		try {
			const parametros = new URLSearchParams({ periodo });
			if (seccionId !== '') parametros.set('seccionId', String(seccionId));
			mensual = await api<ReporteMensual>(`/reportes/mensual?${parametros}`);
			cargandoMensual = false;
			await tick();
			dibujarMensual();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargandoMensual = false;
		}
	}

	function dibujarDiario() {
		if (!canvasDiario || !diario) return;
		graficoDiario?.destroy();

		const resumen = diario.resumen;
		const aTiempo = Math.max(resumen.presentes - resumen.tarde, 0);
		graficoDiario = new Chart(canvasDiario, {
			type: 'doughnut',
			data: {
				labels: ['A tiempo', 'Tarde', 'Ausentes', 'Justificados', 'Pendientes'],
				datasets: [
					{
						data: [aTiempo, resumen.tarde, resumen.ausentes, resumen.justificados, resumen.pendientes],
						backgroundColor: ['#198754', '#e0a800', '#b42318', '#1f4fb8', '#c7d0ca'],
						borderWidth: 0
					}
				]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				plugins: {
					legend: { position: 'bottom', labels: { boxWidth: 12, usePointStyle: true } }
				},
				cutout: '64%'
			}
		});
	}

	function dibujarMensual() {
		if (!canvasMensual || !mensual) return;
		graficoMensual?.destroy();

		graficoMensual = new Chart(canvasMensual, {
			type: 'bar',
			data: {
				labels: mensual.estudiantes.map((fila) => fila.nombre),
				datasets: [
					{
						label: '% de asistencia',
						data: mensual.estudiantes.map((fila) => fila.porcentajeAsistencia),
						backgroundColor: mensual.estudiantes.map((fila) =>
							fila.porcentajeAsistencia >= 90
								? '#198754'
								: fila.porcentajeAsistencia >= 75
									? '#e0a800'
									: '#b42318'
						),
						borderRadius: 8,
						maxBarThickness: 46
					}
				]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				scales: {
					y: {
						beginAtZero: true,
						max: 100,
						ticks: { callback: (valor) => `${valor}%` },
						grid: { color: '#eef2ef' }
					},
					x: {
						grid: { display: false },
						ticks: { maxRotation: 30, minRotation: 0 }
					}
				},
				plugins: {
					legend: { display: false },
					tooltip: {
						callbacks: {
							label: (contexto) => `${(contexto.parsed.y ?? 0).toFixed(2)}% de asistencia`
						}
					}
				}
			}
		});
	}

	function descargarCsv() {
		const parametros = new URLSearchParams({ periodo });
		if (seccionId !== '') parametros.set('seccionId', String(seccionId));
		window.location.href = `/api/v1/reportes/asistencia.csv?${parametros}`;
	}
</script>

<svelte:head>
	<title>Reportes · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Reportes</h1>
		<p>R01 diario, R02 mensual y R03 reincidencias por estudiante.</p>
	</div>
	{#if me?.rol === 'ADMINISTRADOR'}
		<button class="btn btn-primary" type="button" onclick={descargarCsv}>
			Descargar CSV
		</button>
	{/if}
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}

<form
	class="card filtros"
	onsubmit={(evento) => {
		evento.preventDefault();
		cargarDiario();
		cargarMensual();
	}}
>
	<div class="field">
		<label class="label" for="fecha">Día (R01)</label>
		<input class="input" id="fecha" type="date" bind:value={fecha} />
	</div>
	<div class="field">
		<label class="label" for="periodo">Mes (R02/R03)</label>
		<input class="input" id="periodo" type="month" bind:value={periodo} />
	</div>
	<div class="field">
		<label class="label" for="seccion">Sección</label>
		<select class="select" id="seccion" bind:value={seccionId}>
			<option value="">Todas</option>
			{#each secciones as seccion (seccion.id)}
				<option value={seccion.id}>{seccion.nombre}</option>
			{/each}
		</select>
	</div>
	<button class="btn btn-ghost" type="submit">Actualizar</button>
</form>

<section class="kpis" aria-label="Resumen del día">
	<div class="card kpi">
		<span class="kpi-label">Matriculados</span>
		<strong>{diario?.resumen.total ?? '—'}</strong>
	</div>
	<div class="card kpi">
		<span class="kpi-label">Presentes</span>
		<strong class="verde">{diario?.resumen.presentes ?? '—'}</strong>
	</div>
	<div class="card kpi">
		<span class="kpi-label">Tardanzas</span>
		<strong class="ambar">{diario?.resumen.tarde ?? '—'}</strong>
	</div>
	<div class="card kpi">
		<span class="kpi-label">Ausentes</span>
		<strong class="rojo">{diario?.resumen.ausentes ?? '—'}</strong>
	</div>
	<div class="card kpi">
		<span class="kpi-label">Alertas del día</span>
		<strong>{diario?.resumen.alertas ?? '—'}</strong>
	</div>
</section>

<section class="grid-dos">
	<div class="card panel">
		<div class="panel-header">
			<div>
				<h2>Composición del día</h2>
				<p class="muted small">{diario?.fecha ?? fecha}</p>
			</div>
			<span class="badge">R01</span>
		</div>
		{#if cargandoDiario}
			<p class="muted" style="padding: 24px">Cargando reporte diario…</p>
		{:else if diario && diario.resumen.total === 0}
			<div class="vacio"><p class="muted">Sin estudiantes matriculados para este filtro.</p></div>
		{:else}
			<div class="grafico"><canvas bind:this={canvasDiario}></canvas></div>
		{/if}
	</div>

	<div class="card panel">
		<div class="panel-header">
			<div>
				<h2>Asistencia mensual</h2>
				<p class="muted small">{mensual?.periodo ?? periodo}</p>
			</div>
			<span class="badge">R02</span>
		</div>
		{#if cargandoMensual}
			<p class="muted" style="padding: 24px">Cargando reporte mensual…</p>
		{:else if mensual && mensual.estudiantes.length === 0}
			<div class="vacio"><p class="muted">Sin asistencias registradas en este periodo.</p></div>
		{:else}
			<div class="grafico"><canvas bind:this={canvasMensual}></canvas></div>
		{/if}
	</div>
</section>

{#if mensual}
	<section class="kpis" aria-label="Resumen mensual">
		<div class="card kpi">
			<span class="kpi-label">Días registrados</span>
			<strong>{mensual.resumen.diasRegistrados}</strong>
		</div>
		<div class="card kpi">
			<span class="kpi-label">Asistencia promedio</span>
			<strong class="verde">{mensual.resumen.porcentajeAsistencia.toFixed(2)}%</strong>
		</div>
		<div class="card kpi">
			<span class="kpi-label">Tardanzas</span>
			<strong class="ambar">{mensual.resumen.diasTarde}</strong>
		</div>
		<div class="card kpi">
			<span class="kpi-label">Ausencias</span>
			<strong class="rojo">{mensual.resumen.diasAusente}</strong>
		</div>
		<div class="card kpi">
			<span class="kpi-label">Reincidencias</span>
			<strong class="rojo">{mensual.resumen.reincidencias}</strong>
		</div>
	</section>

	<section class="card" style="margin-top: 16px">
		<div class="panel-header" style="padding: 20px">
			<div>
				<h2>Detalle por estudiante</h2>
				<p class="muted small">Incluye inasistencias y reincidencias del mes.</p>
			</div>
		</div>
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Estudiante</th>
						<th>Sección</th>
						<th>Días</th>
						<th>Presente</th>
						<th>Tarde</th>
						<th>Ausente</th>
						<th>Justificado</th>
						<th>%</th>
						<th>Inasistencias</th>
						<th>Reincidencias</th>
					</tr>
				</thead>
				<tbody>
					{#each mensual.estudiantes as fila (fila.idEstudiante + '-' + fila.seccion)}
						<tr>
							<td>
								<strong>{fila.nombre}</strong>
								<div class="muted small">{fila.codigoMatricula}</div>
							</td>
							<td>{fila.seccion}</td>
							<td>{fila.diasRegistrados}</td>
							<td>{fila.diasPresente}</td>
							<td>{fila.diasTarde}</td>
							<td>{fila.diasAusente}</td>
							<td>{fila.diasJustificado}</td>
							<td>
								<span
									class="badge"
									class:badge-warn={fila.porcentajeAsistencia < 90 && fila.porcentajeAsistencia >= 75}
									class:badge-danger={fila.porcentajeAsistencia < 75}
								>
									{fila.porcentajeAsistencia.toFixed(2)}%
								</span>
							</td>
							<td>{fila.inasistencias}</td>
							<td>{fila.reincidencias}</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	</section>
{/if}

<style>
	.filtros {
		display: flex;
		gap: 14px;
		align-items: flex-end;
		flex-wrap: wrap;
		padding: 16px;
	}

	.filtros .field {
		min-width: 180px;
	}

	.kpis {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
		gap: 12px;
		margin-top: 16px;
	}

	.kpi {
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	.kpi-label {
		font-size: 12px;
		font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.04em;
		color: var(--kc-muted);
	}

	.kpi strong {
		font-size: 28px;
		line-height: 1.1;
	}

	.verde {
		color: var(--kc-green-dark);
	}

	.ambar {
		color: var(--kc-warn);
	}

	.rojo {
		color: var(--kc-danger);
	}

	.grid-dos {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
		gap: 16px;
		margin-top: 16px;
	}

	.panel {
		padding: 18px;
	}

	.panel-header {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 12px;
		margin-bottom: 16px;
	}

	.panel-header h2 {
		font-size: 17px;
	}

	.grafico {
		position: relative;
		height: 300px;
	}

	.vacio {
		padding: 40px 20px;
		text-align: center;
	}
</style>
