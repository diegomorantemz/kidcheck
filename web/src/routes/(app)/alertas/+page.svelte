<script lang="ts">
	import { onMount } from 'svelte';
	import { api, mensajeDeError } from '$lib/api';
	import type { Alerta } from '$lib/types';

	let estado = $state('');
	let fecha = $state('');
	let alertas = $state<Alerta[]>([]);
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
			if (fecha) parametros.set('fecha', fecha);
			alertas = await api<Alerta[]>(`/alertas?${parametros}`);
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	}

	async function atender(alerta: Alerta) {
		if (!confirm(`¿Marcar como atendida la alerta de ${alerta.estudiante}?`)) return;
		procesando = alerta.idAlerta;
		error = '';
		ok = '';
		try {
			await api(`/alertas/${alerta.idAlerta}/atender`, { method: 'POST' });
			ok = `Alerta de ${alerta.estudiante} atendida.`;
			await cargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			procesando = null;
		}
	}

	async function derivar(alerta: Alerta) {
		const comentario = prompt(`Derivar a psicología el caso de ${alerta.estudiante}. Comentario:`);
		if (comentario === null) return;
		procesando = alerta.idAlerta;
		error = '';
		ok = '';
		try {
			await api(`/alertas/${alerta.idAlerta}/derivar`, {
				method: 'POST',
				body: { comentario: comentario.trim() || null }
			});
			ok = `Caso de ${alerta.estudiante} derivado a psicología.`;
			await cargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			procesando = null;
		}
	}

	function claseEstado(valor: string) {
		return valor === 'generada'
			? 'badge-danger'
			: valor === 'notificada'
				? 'badge-warn'
				: valor === 'justificada'
					? 'badge-info'
					: 'badge-ok';
	}

	function fechaHora(valor: string) {
		return new Date(valor).toLocaleString('es-PE', {
			dateStyle: 'short',
			timeStyle: 'short',
			timeZone: 'America/Lima'
		});
	}
</script>

<svelte:head>
	<title>Alertas · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Bandeja de alertas</h1>
		<p>Inasistencias y reincidencias para atender o derivar.</p>
	</div>
	<a class="btn btn-ghost" href="/justificaciones">Ver justificaciones</a>
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}
{#if ok}
	<div class="alerta ok" role="status" style="margin-bottom: 16px">{ok}</div>
{/if}

<form class="card filtros" onsubmit={(e) => { e.preventDefault(); cargar(); }}>
	<div class="field">
		<label class="label" for="fecha">Fecha de generación</label>
		<input class="input" id="fecha" type="date" bind:value={fecha} />
	</div>
	<div class="field">
		<label class="label" for="estado">Estado</label>
		<select class="select" id="estado" bind:value={estado}>
			<option value="">Todos</option>
			<option value="generada">Generada</option>
			<option value="notificada">Notificada</option>
			<option value="justificada">Justificada</option>
			<option value="atendida">Atendida</option>
			<option value="cerrada">Cerrada</option>
		</select>
	</div>
	<button class="btn btn-ghost" type="submit">Filtrar</button>
</form>

<section class="card" style="margin-top: 16px">
	{#if cargando}
		<p class="muted" style="padding: 28px">Cargando alertas…</p>
	{:else if alertas.length === 0}
		<div class="vacio">
			<h2>Sin alertas</h2>
			<p class="muted">No hay casos para los filtros seleccionados.</p>
		</div>
	{:else}
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Caso</th>
						<th>Estudiante</th>
						<th>Tipo</th>
						<th>Generada</th>
						<th>Justificación</th>
						<th>Estado</th>
						<th style="text-align: right">Acciones</th>
					</tr>
				</thead>
				<tbody>
					{#each alertas as alerta (alerta.idAlerta)}
						<tr>
							<td class="muted small">#{alerta.idAlerta}</td>
							<td>
								<strong>{alerta.estudiante}</strong>
								<div class="muted small">
									{alerta.seccion ?? '—'}
									{#if alerta.fechaAsistencia}· {alerta.fechaAsistencia}{/if}
								</div>
							</td>
							<td>
								<span class="badge" class:badge-danger={alerta.tipo === 'inasistencia'} class:badge-warn={alerta.tipo === 'reincidencia'}>
									{alerta.tipo}
								</span>
								{#if alerta.umbral}
									<div class="muted small">{alerta.umbral} ausencias</div>
								{/if}
							</td>
							<td class="muted small">{fechaHora(alerta.fechaGeneracion)}</td>
							<td>
								{#if alerta.justificacion}
									<div class="small">{alerta.justificacion.motivo}</div>
									<div class="muted small">{alerta.justificacion.estado}</div>
								{:else}
									<span class="muted small">—</span>
								{/if}
							</td>
							<td>
								<span class="badge {claseEstado(alerta.estado)}">{alerta.estado}</span>
							</td>
							<td>
								<div class="acciones">
									{#if alerta.estado !== 'atendida' && alerta.estado !== 'cerrada'}
										<button
											class="btn btn-ghost btn-sm"
											type="button"
											disabled={procesando === alerta.idAlerta}
											onclick={() => atender(alerta)}
										>
											Atender
										</button>
										<button
											class="btn btn-primary btn-sm"
											type="button"
											disabled={procesando === alerta.idAlerta}
											onclick={() => derivar(alerta)}
										>
											Derivar
										</button>
									{:else}
										<span class="muted small">cerrado</span>
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
		grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)) auto;
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
