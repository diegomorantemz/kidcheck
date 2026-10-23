<script lang="ts">
	import { onMount } from 'svelte';
	import { api, mensajeDeError } from '$lib/api';
	import type { PanelDia, Seccion } from '$lib/types';

	let fecha = $state(fechaLocalLima());
	let seccionId = $state<number | ''>('');
	let panel = $state<PanelDia | null>(null);
	let secciones = $state<Seccion[]>([]);
	let cargando = $state(true);
	let error = $state('');
	let ultimaActualizacion = $state('');

	function fechaLocalLima() {
		return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(new Date());
	}

	onMount(() => {
		void iniciar();
		const temporizador = setInterval(() => {
			if (!document.hidden) void cargar(true);
		}, 5000);
		const alVolver = () => {
			if (!document.hidden) void cargar(true);
		};
		document.addEventListener('visibilitychange', alVolver);

		return () => {
			clearInterval(temporizador);
			document.removeEventListener('visibilitychange', alVolver);
		};
	});

	async function iniciar() {
		try {
			secciones = await api<Seccion[]>('/secciones');
		} catch {
			// El panel sigue funcionando aunque falle el filtro de secciones.
		}
		await cargar();
	}

	async function cargar(silencioso = false) {
		if (!silencioso) cargando = true;
		error = '';
		try {
			const parametros = new URLSearchParams({ fecha });
			if (seccionId !== '') parametros.set('seccionId', String(seccionId));
			panel = await api<PanelDia>(`/panel/dia?${parametros}`);
			ultimaActualizacion = new Date(panel.actualizadoEn).toLocaleTimeString('es-PE');
		} catch (excepcion) {
			if (!silencioso) error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	}

	function hora(valor?: string | null) {
		return valor ? valor.slice(0, 5) : '—';
	}
</script>

<svelte:head>
	<title>Panel del día · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Panel del día</h1>
		<p>Ingresos, tardanzas y ausencias en tiempo real.</p>
	</div>
	<div class="live">
		<span class="dot"></span>
		{#if ultimaActualizacion}
			Actualizado {ultimaActualizacion} · cada 5 s
		{:else}
			Conectando…
		{/if}
	</div>
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}

<form class="card filtros" onsubmit={(e) => { e.preventDefault(); cargar(); }}>
	<div class="field">
		<label class="label" for="fecha">Fecha</label>
		<input class="input" id="fecha" type="date" bind:value={fecha} onchange={() => cargar()} />
	</div>
	<div class="field">
		<label class="label" for="seccion">Sección</label>
		<select class="select" id="seccion" bind:value={seccionId} onchange={() => cargar()}>
			<option value="">Todas</option>
			{#each secciones as seccion (seccion.id)}
				<option value={seccion.id}>{seccion.nombre}</option>
			{/each}
		</select>
	</div>
	<button class="btn btn-ghost" type="submit">Actualizar</button>
</form>

{#if cargando && !panel}
	<p class="muted" style="padding: 40px">Cargando el día…</p>
{:else if panel}
	<section class="tarjetas">
		<article class="stat">
			<span class="stat-label">Total</span>
			<strong>{panel.resumen.total}</strong>
			<small>alumnos con matrícula activa</small>
		</article>
		<article class="stat verde">
			<span class="stat-label">Presentes</span>
			<strong>{panel.resumen.presentes}</strong>
			<small>{panel.resumen.tarde} con tardanza</small>
		</article>
		<article class="stat ambar">
			<span class="stat-label">Pendientes</span>
			<strong>{panel.resumen.pendientes}</strong>
			<small>sin registro todavía</small>
		</article>
		<article class="stat rojo">
			<span class="stat-label">Ausentes</span>
			<strong>{panel.resumen.ausentes}</strong>
			<small>{panel.resumen.justificados} justificados</small>
		</article>
		<article class="stat azul">
			<span class="stat-label">Alertas</span>
			<strong>{panel.resumen.alertas}</strong>
			<small><a href="/alertas">abrir bandeja</a></small>
		</article>
	</section>

	{#if panel.secciones.length > 0}
		<section class="secciones">
			{#each panel.secciones as seccion (seccion.id)}
				<article class="seccion-mini">
					<strong>{seccion.nombre}</strong>
					<span class="muted small">{seccion.presentes}/{seccion.total} presentes</span>
					<div class="barra">
						<div
							style="width: {seccion.total ? (seccion.presentes / seccion.total) * 100 : 0}%"
						></div>
					</div>
					<span class="muted small">{seccion.pendientes} pendientes</span>
				</article>
			{/each}
		</section>
	{/if}

	<section class="card" style="margin-top: 18px">
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Estudiante</th>
						<th>Sección</th>
						<th>Estado</th>
						<th>Hora</th>
						<th>Apoderado</th>
						<th>Alerta</th>
					</tr>
				</thead>
				<tbody>
					{#each panel.estudiantes as estudiante (estudiante.idEstudiante)}
						<tr>
							<td><strong>{estudiante.nombre}</strong></td>
							<td>{estudiante.seccion}</td>
							<td>
								<span class="badge" class:badge-ok={estudiante.estado === 'PRESENTE'} class:badge-warn={estudiante.estado === 'TARDE' || estudiante.estado === 'PENDIENTE'} class:badge-danger={estudiante.estado === 'AUSENTE'} class:badge-info={estudiante.estado === 'JUSTIFICADO'}>
									{estudiante.estado}
								</span>
								{#if estudiante.tarde && estudiante.hora}
									<div class="muted small">tardanza</div>
								{/if}
							</td>
							<td>{hora(estudiante.hora)}</td>
							<td class="muted">{estudiante.apoderado ?? '—'}</td>
							<td>
								{#if estudiante.alerta}
									<a class="badge badge-warn" href="/alertas?id={estudiante.alerta.id}">
										{alertaTexto(estudiante.alerta.tipo)}
									</a>
								{:else}
									<span class="muted small">—</span>
								{/if}
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
		{#if panel.estudiantes.length === 0}
			<div class="vacio">
				<h2>Sin alumnos</h2>
				<p class="muted">No hay matrículas activas para esta fecha y sección.</p>
			</div>
		{/if}
	</section>
{/if}

{#snippet alertaTexto(tipo: string)}
	{tipo === 'inasistencia' ? 'Inasistencia' : tipo === 'reincidencia' ? 'Reincidencia' : 'Tardanza'}
{/snippet}

<style>
	.live {
		display: inline-flex;
		align-items: center;
		gap: 8px;
		font-size: 13px;
		color: var(--kc-muted);
		background: #fff;
		border: 1px solid var(--kc-border);
		border-radius: 999px;
		padding: 7px 12px;
	}

	.dot {
		width: 8px;
		height: 8px;
		border-radius: 50%;
		background: var(--kc-green);
		box-shadow: 0 0 0 0 rgba(25, 135, 84, 0.45);
		animation: pulso 1.8s infinite;
	}

	@keyframes pulso {
		70% {
			box-shadow: 0 0 0 7px rgba(25, 135, 84, 0);
		}
		100% {
			box-shadow: 0 0 0 0 rgba(25, 135, 84, 0);
		}
	}

	.filtros {
		display: grid;
		grid-template-columns: minmax(160px, 220px) minmax(160px, 220px) auto;
		gap: 12px;
		align-items: end;
		padding: 14px;
		margin-bottom: 18px;
	}

	.tarjetas {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
		gap: 12px;
	}

	.stat {
		background: #fff;
		border: 1px solid var(--kc-border);
		border-radius: var(--kc-radius);
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 2px;
		border-top: 3px solid #c9d6ce;
	}

	.stat.verde {
		border-top-color: var(--kc-green);
	}
	.stat.ambar {
		border-top-color: #d9a400;
	}
	.stat.rojo {
		border-top-color: var(--kc-danger);
	}
	.stat.azul {
		border-top-color: #2f6fed;
	}

	.stat-label {
		font-size: 12px;
		text-transform: uppercase;
		letter-spacing: 0.05em;
		color: var(--kc-muted);
		font-weight: 700;
	}

	.stat strong {
		font-size: 30px;
		line-height: 1.1;
	}

	.stat small {
		color: var(--kc-muted);
		font-size: 12px;
	}

	.secciones {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
		gap: 12px;
		margin-top: 14px;
	}

	.seccion-mini {
		background: #fff;
		border: 1px solid var(--kc-border);
		border-radius: 13px;
		padding: 12px 14px;
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	.barra {
		height: 6px;
		background: #edf2ee;
		border-radius: 999px;
		overflow: hidden;
		margin: 3px 0;
	}

	.barra div {
		height: 100%;
		background: var(--kc-green);
		border-radius: 999px;
		transition: width 0.4s;
	}

	.vacio {
		padding: 36px 22px;
		text-align: center;
	}

	.vacio h2 {
		font-size: 18px;
		margin-bottom: 6px;
	}

	.vacio p {
		margin: 0;
	}

	@media (max-width: 640px) {
		.filtros {
			grid-template-columns: 1fr;
		}
	}
</style>
