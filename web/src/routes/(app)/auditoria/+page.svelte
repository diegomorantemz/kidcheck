<script lang="ts">
	import { onMount } from 'svelte';
	import { api, mensajeDeError } from '$lib/api';
	import type { Auditoria, PageResponse } from '$lib/types';

	const ENTIDADES = [
		'estudiante',
		'credencial',
		'asistencia',
		'alerta',
		'justificacion',
		'configuracion',
		'calendario',
		'seccion',
		'reporte'
	];

	let entidad = $state('');
	let desde = $state('');
	let hasta = $state('');
	let pagina = $state(0);
	let tamano = 50;
	let datos = $state<PageResponse<Auditoria> | null>(null);
	let cargando = $state(true);
	let error = $state('');

	onMount(cargar);

	async function cargar() {
		cargando = true;
		error = '';
		try {
			const parametros = new URLSearchParams({ pagina: String(pagina), tamano: String(tamano) });
			if (entidad) parametros.set('entidad', entidad);
			if (desde) parametros.set('desde', desde);
			if (hasta) parametros.set('hasta', hasta);
			datos = await api<PageResponse<Auditoria>>(`/auditoria?${parametros}`);
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	}

	function buscar(evento: SubmitEvent) {
		evento.preventDefault();
		pagina = 0;
		cargar();
	}

	function cambiarPagina(nueva: number) {
		if (!datos || nueva < 0 || nueva >= datos.totalPaginas) return;
		pagina = nueva;
		cargar();
	}

	function fechaHora(valor: string) {
		return new Date(valor).toLocaleString('es-PE', {
			dateStyle: 'short',
			timeStyle: 'medium',
			timeZone: 'America/Lima'
		});
	}

	function claseAccion(accion: string) {
		return accion === 'crear' || accion === 'emitir' || accion === 'aprobar'
			? 'badge-ok'
			: accion === 'revocar' || accion === 'rechazar'
				? 'badge-danger'
				: 'badge-info';
	}

	function datosLegibles(datos: Record<string, unknown> | null | undefined) {
		if (!datos || Object.keys(datos).length === 0) return '—';
		return Object.entries(datos)
			.map(([clave, valor]) => `${clave}: ${valor}`)
			.join(' · ');
	}
</script>

<svelte:head>
	<title>Auditoría · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Auditoría</h1>
		<p>Quién creó, modificó, aprobó o exportó información del colegio.</p>
	</div>
	<span class="badge">RNF-03</span>
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}

<form class="card filtros" onsubmit={buscar}>
	<div class="field">
		<label class="label" for="entidad">Entidad</label>
		<select class="select" id="entidad" bind:value={entidad}>
			<option value="">Todas</option>
			{#each ENTIDADES as valor (valor)}
				<option value={valor}>{valor}</option>
			{/each}
		</select>
	</div>
	<div class="field">
		<label class="label" for="desde">Desde</label>
		<input class="input" id="desde" type="date" bind:value={desde} />
	</div>
	<div class="field">
		<label class="label" for="hasta">Hasta</label>
		<input class="input" id="hasta" type="date" bind:value={hasta} />
	</div>
	<button class="btn btn-ghost" type="submit">Filtrar</button>
</form>

<section class="card" style="margin-top: 16px">
	{#if cargando}
		<p class="muted" style="padding: 28px">Cargando registros de auditoría…</p>
	{:else if !datos || datos.contenido.length === 0}
		<div class="vacio">
			<h2>Sin registros</h2>
			<p class="muted">No hay acciones auditadas para los filtros seleccionados.</p>
		</div>
	{:else}
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Fecha (Lima)</th>
						<th>Usuario</th>
						<th>Entidad</th>
						<th>Acción</th>
						<th>Detalle</th>
						<th>IP</th>
					</tr>
				</thead>
				<tbody>
					{#each datos.contenido as registro (registro.id)}
						<tr>
							<td class="small">{fechaHora(registro.fecha)}</td>
							<td>
								<strong>{registro.usuario ?? 'Sistema'}</strong>
								<div class="muted small">#{registro.idUsuario ?? '—'}</div>
							</td>
							<td>
								{registro.entidad}
								<div class="muted small">#{registro.idEntidad ?? '—'}</div>
							</td>
							<td><span class="badge {claseAccion(registro.accion)}">{registro.accion}</span></td>
							<td class="small">{datosLegibles(registro.datos)}</td>
							<td class="muted small">{registro.ip ?? '—'}</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>

		<div class="paginacion">
			<span class="muted small">
				Página {datos.pagina + 1} de {Math.max(datos.totalPaginas, 1)} · {datos.total} registros
			</span>
			<div class="acciones">
				<button
					class="btn btn-ghost btn-sm"
					type="button"
					disabled={datos.pagina === 0}
					onclick={() => cambiarPagina(pagina - 1)}
				>
					Anterior
				</button>
				<button
					class="btn btn-ghost btn-sm"
					type="button"
					disabled={datos.pagina + 1 >= datos.totalPaginas}
					onclick={() => cambiarPagina(pagina + 1)}
				>
					Siguiente
				</button>
			</div>
		</div>
	{/if}
</section>

<style>
	.filtros {
		display: flex;
		gap: 14px;
		align-items: flex-end;
		flex-wrap: wrap;
		padding: 16px;
	}

	.filtros .field {
		min-width: 170px;
	}

	.paginacion {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16px;
		padding: 14px 16px;
		border-top: 1px solid var(--kc-border);
		flex-wrap: wrap;
	}

	.vacio {
		padding: 40px 20px;
		text-align: center;
	}
</style>
