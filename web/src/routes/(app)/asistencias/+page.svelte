<script lang="ts">
	import { onMount } from 'svelte';
	import { api, mensajeDeError } from '$lib/api';
	import type { Asistencia, Seccion } from '$lib/types';

	let fecha = $state(fechaLocalLima());
	let seccionId = $state<number | ''>('');
	let estado = $state('');
	let asistencias = $state<Asistencia[]>([]);
	let secciones = $state<Seccion[]>([]);
	let cargando = $state(true);
	let error = $state('');
	let ok = $state('');

	let justificando = $state<Asistencia | null>(null);
	let motivo = $state('enfermedad');
	let detalle = $state('');
	let guardando = $state(false);

	function fechaLocalLima() {
		return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(new Date());
	}

	onMount(async () => {
		try {
			secciones = await api<Seccion[]>('/secciones');
		} catch {
			// filtro opcional
		}
		await cargar();
	});

	async function cargar() {
		cargando = true;
		error = '';
		try {
			const parametros = new URLSearchParams({ fecha });
			if (seccionId !== '') parametros.set('seccionId', String(seccionId));
			if (estado) parametros.set('estado', estado);
			asistencias = await api<Asistencia[]>(`/asistencias?${parametros}`);
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	}

	function abrirJustificar(asistencia: Asistencia) {
		justificando = asistencia;
		motivo = 'enfermedad';
		detalle = '';
		error = '';
	}

	async function enviarJustificacion(evento: SubmitEvent) {
		evento.preventDefault();
		if (!justificando) return;
		guardando = true;
		error = '';
		ok = '';
		try {
			await api(`/asistencias/${justificando.idAsistencia}/justificar`, {
				method: 'POST',
				body: { motivo, detalle: detalle.trim() || null }
			});
			ok = `Asistencia de ${justificando.nombre} justificada.`;
			justificando = null;
			await cargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			guardando = false;
		}
	}

	function claseEstado(valor: string) {
		return valor === 'PRESENTE'
			? 'badge-ok'
			: valor === 'TARDE'
				? 'badge-warn'
				: valor === 'AUSENTE'
					? 'badge-danger'
					: 'badge-info';
	}
</script>

<svelte:head>
	<title>Asistencias · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Asistencias</h1>
		<p>Listado por día con justificación manual del colegio.</p>
	</div>
	<a class="btn btn-ghost" href="/dashboard">Ver panel del día</a>
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}
{#if ok}
	<div class="alerta ok" role="status" style="margin-bottom: 16px">{ok}</div>
{/if}

<form class="card filtros" onsubmit={(e) => { e.preventDefault(); cargar(); }}>
	<div class="field">
		<label class="label" for="fecha">Fecha</label>
		<input class="input" id="fecha" type="date" bind:value={fecha} />
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
	<div class="field">
		<label class="label" for="estado">Estado</label>
		<select class="select" id="estado" bind:value={estado}>
			<option value="">Todos</option>
			<option value="presente">Presente</option>
			<option value="tarde">Tarde</option>
			<option value="ausente">Ausente</option>
			<option value="justificado">Justificado</option>
		</select>
	</div>
	<button class="btn btn-ghost" type="submit">Buscar</button>
</form>

{#if justificando}
	<form class="card formulario" onsubmit={enviarJustificacion}>
		<div>
			<h2>Justificar ausencia</h2>
			<p class="muted small">
				{justificando.nombre} · {justificando.seccion} · {fecha}
			</p>
		</div>
		<div class="grid">
			<div class="field">
				<label class="label" for="motivo">Motivo</label>
				<select class="select" id="motivo" bind:value={motivo}>
					<option value="enfermedad">Enfermedad</option>
					<option value="cita_medica">Cita médica</option>
					<option value="viaje">Viaje</option>
					<option value="emergencia_familiar">Emergencia familiar</option>
					<option value="otro">Otro</option>
				</select>
			</div>
			<div class="field">
				<label class="label" for="detalle">Detalle</label>
				<input class="input" id="detalle" bind:value={detalle} maxlength="500" />
			</div>
		</div>
		<div class="acciones">
			<button class="btn btn-ghost" type="button" onclick={() => (justificando = null)}>
				Cancelar
			</button>
			<button class="btn btn-primary" type="submit" disabled={guardando}>
				{guardando ? 'Guardando…' : 'Aprobar justificación'}
			</button>
		</div>
	</form>
{/if}

<section class="card" style="margin-top: 16px">
	{#if cargando}
		<p class="muted" style="padding: 28px">Cargando asistencias…</p>
	{:else if asistencias.length === 0}
		<div class="vacio">
			<h2>Sin registros</h2>
			<p class="muted">No hay asistencias para los filtros seleccionados.</p>
		</div>
	{:else}
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Estudiante</th>
						<th>Sección</th>
						<th>Estado</th>
						<th>Hora</th>
						<th>Origen</th>
						<th>Justificación</th>
						<th style="text-align: right">Acciones</th>
					</tr>
				</thead>
				<tbody>
					{#each asistencias as asistencia (asistencia.idAsistencia)}
						<tr>
							<td><strong>{asistencia.nombre}</strong></td>
							<td>{asistencia.seccion}</td>
							<td>
								<span class="badge {claseEstado(asistencia.estado)}">{asistencia.estado}</span>
								{#if asistencia.minutosTarde > 0}
									<div class="muted small">{asistencia.minutosTarde} min tarde</div>
								{/if}
							</td>
							<td>{asistencia.hora?.slice(0, 5) ?? '—'}</td>
							<td class="muted small">{asistencia.origen}</td>
							<td class="muted small">{asistencia.justificacionEstado ?? '—'}</td>
							<td>
								<div class="acciones">
									{#if asistencia.estado !== 'JUSTIFICADO'}
										<button
											class="btn btn-ghost btn-sm"
											type="button"
											onclick={() => abrirJustificar(asistencia)}
										>
											Justificar
										</button>
									{:else}
										<span class="muted small">—</span>
									{/if}
								</div>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</section>

<style>
	.filtros {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)) auto;
		gap: 12px;
		align-items: end;
		padding: 14px;
	}

	.formulario {
		padding: 20px;
		margin-bottom: 16px;
		display: flex;
		flex-direction: column;
		gap: 14px;
		border-left: 4px solid var(--kc-green);
	}

	.grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
		gap: 14px;
	}

	.vacio {
		padding: 40px 22px;
		text-align: center;
	}

	.vacio h2 {
		font-size: 18px;
		margin-bottom: 6px;
	}

	.vacio p {
		margin: 0;
	}
</style>
