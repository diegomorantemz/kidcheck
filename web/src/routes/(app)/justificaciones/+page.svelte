<script lang="ts">
	import { onMount } from 'svelte';
	import { api, mensajeDeError } from '$lib/api';
	import type { Justificacion } from '$lib/types';

	let estado = $state('pendiente');
	let justificaciones = $state<Justificacion[]>([]);
	let cargando = $state(true);
	let error = $state('');
	let ok = $state('');
	let procesando = $state<number | null>(null);

	onMount(cargar);

	async function cargar() {
		cargando = true;
		error = '';
		try {
			const parametros = new URLSearchParams();
			if (estado) parametros.set('estado', estado);
			justificaciones = await api<Justificacion[]>(`/justificaciones?${parametros}`);
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	}

	async function revisar(justificacion: Justificacion, aprobar: boolean) {
		const accion = aprobar ? 'aprobar' : 'rechazar';
		if (!confirm(`¿${aprobar ? 'Aprobar' : 'Rechazar'} la justificación de ${justificacion.estudiante}?`)) {
			return;
		}
		procesando = justificacion.id;
		error = '';
		ok = '';
		try {
			await api(`/justificaciones/${justificacion.id}/${accion}`, { method: 'POST' });
			ok = `Justificación de ${justificacion.estudiante} ${aprobar ? 'aprobada' : 'rechazada'}.`;
			await cargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			procesando = null;
		}
	}

	function fecha(valor: string) {
		return new Date(valor).toLocaleString('es-PE', {
			dateStyle: 'short',
			timeStyle: 'short',
			timeZone: 'America/Lima'
		});
	}
</script>

<svelte:head>
	<title>Justificaciones · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Justificaciones</h1>
		<p>Revisión de ausencias enviadas por los apoderados.</p>
	</div>
	<a class="btn btn-ghost" href="/alertas">Volver a alertas</a>
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}
{#if ok}
	<div class="alerta ok" role="status" style="margin-bottom: 16px">{ok}</div>
{/if}

<form class="card filtros" onsubmit={(e) => { e.preventDefault(); cargar(); }}>
	<div class="field">
		<label class="label" for="estado">Estado</label>
		<select class="select" id="estado" bind:value={estado}>
			<option value="">Todas</option>
			<option value="pendiente">Pendientes</option>
			<option value="aprobada">Aprobadas</option>
			<option value="rechazada">Rechazadas</option>
		</select>
	</div>
	<button class="btn btn-ghost" type="submit">Filtrar</button>
</form>

<section class="card" style="margin-top: 16px">
	{#if cargando}
		<p class="muted" style="padding: 28px">Cargando justificaciones…</p>
	{:else if justificaciones.length === 0}
		<div class="vacio">
			<h2>Sin justificaciones</h2>
			<p class="muted">No hay envíos para el estado seleccionado.</p>
		</div>
	{:else}
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Estudiante</th>
						<th>Fecha de ausencia</th>
						<th>Motivo</th>
						<th>Apoderado</th>
						<th>Enviada</th>
						<th>Estado</th>
						<th style="text-align: right">Acciones</th>
					</tr>
				</thead>
				<tbody>
					{#each justificaciones as justificacion (justificacion.id)}
						<tr>
							<td>
								<strong>{justificacion.estudiante}</strong>
								<div class="muted small">{justificacion.seccion ?? '—'}</div>
							</td>
							<td>{justificacion.fecha ?? '—'}</td>
							<td>
								<div>{justificacion.motivo}</div>
								{#if justificacion.detalle}
									<div class="muted small">{justificacion.detalle}</div>
								{/if}
							</td>
							<td class="muted small">{justificacion.apoderado ?? '—'}</td>
							<td class="muted small">{fecha(justificacion.fechaEnvio)}</td>
							<td>
								<span class="badge" class:badge-warn={justificacion.estado === 'pendiente'} class:badge-ok={justificacion.estado === 'aprobada'} class:badge-danger={justificacion.estado === 'rechazada'}>
									{justificacion.estado}
								</span>
							</td>
							<td>
								<div class="acciones">
									{#if justificacion.estado === 'pendiente'}
										<button
											class="btn btn-ghost btn-sm"
											type="button"
											disabled={procesando === justificacion.id}
											onclick={() => revisar(justificacion, false)}
										>
											Rechazar
										</button>
										<button
											class="btn btn-primary btn-sm"
											type="button"
											disabled={procesando === justificacion.id}
											onclick={() => revisar(justificacion, true)}
										>
											Aprobar
										</button>
									{:else}
										<span class="muted small">revisada</span>
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
		grid-template-columns: minmax(180px, 260px) auto;
		gap: 12px;
		align-items: end;
		padding: 14px;
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
