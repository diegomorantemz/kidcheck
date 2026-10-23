<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { api, mensajeDeError } from '$lib/api';
	import type { CredencialEmitida, Estudiante, PageResponse, Seccion } from '$lib/types';
	import QrModal from '$lib/components/QrModal.svelte';

	let q = $state('');
	let seccionId = $state<number | ''>('');
	let pagina = $state(0);

	let datos = $state<PageResponse<Estudiante> | null>(null);
	let secciones = $state<Seccion[]>([]);
	let cargando = $state(true);
	let error = $state('');
	let emitiendo = $state<number | null>(null);
	let qr = $state<CredencialEmitida | null>(null);

	onMount(async () => {
		try {
			secciones = await api<Seccion[]>('/secciones');
			await recargar();

			const pendiente = page.url.searchParams.get('qr');
			if (pendiente) {
				await emitir(Number(pendiente));
				const limpia = new URL(page.url);
				limpia.searchParams.delete('qr');
				await goto(limpia, { replaceState: true, noScroll: true, keepFocus: true });
			}
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	});

	async function recargar() {
		const parametros = new URLSearchParams({ page: String(pagina), size: '10' });
		if (q.trim()) parametros.set('q', q.trim());
		if (seccionId !== '') parametros.set('seccionId', String(seccionId));

		datos = await api<PageResponse<Estudiante>>(`/estudiantes?${parametros}`);
	}

	async function buscar(evento: SubmitEvent) {
		evento.preventDefault();
		pagina = 0;
		error = '';
		try {
			await recargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		}
	}

	async function irA(nueva: number) {
		pagina = nueva;
		error = '';
		try {
			await recargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		}
	}

	async function emitir(idEstudiante: number) {
		emitiendo = idEstudiante;
		error = '';
		try {
			qr = await api<CredencialEmitida>(`/estudiantes/${idEstudiante}/credencial`, { method: 'POST' });
			await recargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			emitiendo = null;
		}
	}

	async function revocar(estudiante: Estudiante) {
		if (!estudiante.idCredencial) return;
		if (!confirm(`¿Revocar la credencial de ${estudiante.nombreCompleto}?`)) return;

		error = '';
		try {
			await api(`/credenciales/${estudiante.idCredencial}`, { method: 'DELETE' });
			await recargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		}
	}
</script>

<svelte:head>
	<title>Estudiantes · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Estudiantes</h1>
		<p>Alta, matrícula y credencial QR del alumnado.</p>
	</div>
	<a class="btn btn-primary" href="/estudiantes/nuevo">+ Nuevo estudiante</a>
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}

<form class="card filtros" onsubmit={buscar}>
	<input
		class="input"
		type="search"
		placeholder="Buscar por nombre, apellido o código…"
		aria-label="Buscar estudiante"
		bind:value={q}
	/>
	<select class="select" bind:value={seccionId} aria-label="Filtrar por sección">
		<option value="">Todas las secciones</option>
		{#each secciones as seccion (seccion.id)}
			<option value={seccion.id}>{seccion.nombre}</option>
		{/each}
	</select>
	<button class="btn btn-ghost" type="submit">Buscar</button>
</form>

<section class="card" style="margin-top: 16px">
	{#if cargando}
		<p class="muted" style="padding: 28px">Cargando estudiantes…</p>
	{:else if !datos || datos.contenido.length === 0}
		<div class="vacio">
			<h2>Sin resultados</h2>
			<p class="muted">Ajusta la búsqueda o registra al primer estudiante.</p>
			<a class="btn btn-primary" href="/estudiantes/nuevo">Registrar estudiante</a>
		</div>
	{:else}
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Estudiante</th>
						<th>Sección</th>
						<th>Apoderado principal</th>
						<th>Credencial</th>
						<th style="text-align: right">Acciones</th>
					</tr>
				</thead>
				<tbody>
					{#each datos.contenido as estudiante (estudiante.id)}
						{@const apoderado = estudiante.apoderados.find((a) => a.esPrincipal) ??
							estudiante.apoderados[0]}
						<tr>
							<td>
								<strong>{estudiante.nombreCompleto}</strong>
								<div class="muted small">{estudiante.codigoMatricula}</div>
							</td>
							<td>{estudiante.seccion ?? '—'}</td>
							<td>
								{#if apoderado}
									<div>{apoderado.nombresApellidos}</div>
									<div class="muted small">
										{apoderado.telefonoWhatsapp ?? 'Sin teléfono'}
									</div>
								{:else}
									<span class="muted">—</span>
								{/if}
							</td>
							<td>
								<span class="badge" class:off={!estudiante.credencialActiva}>
									{estudiante.credencialActiva ? 'Activa' : 'Sin credencial'}
								</span>
							</td>
							<td>
								<div class="acciones">
									<a class="btn btn-ghost btn-sm" href={`/estudiantes/${estudiante.id}`}>Editar</a>

									{#if estudiante.credencialActiva}
										<button
											class="btn btn-ghost btn-sm"
											type="button"
											disabled={emitiendo === estudiante.id}
											onclick={() => emitir(estudiante.id)}
										>
											{emitiendo === estudiante.id ? 'Emitiendo…' : 'Reemitir QR'}
										</button>
										<button
											class="btn btn-danger btn-sm"
											type="button"
											onclick={() => revocar(estudiante)}
										>
											Revocar
										</button>
									{:else}
										<button
											class="btn btn-primary btn-sm"
											type="button"
											disabled={emitiendo === estudiante.id}
											onclick={() => emitir(estudiante.id)}
										>
											{emitiendo === estudiante.id ? 'Emitiendo…' : 'Generar QR'}
										</button>
									{/if}
								</div>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>

		<footer class="paginacion">
			<span class="muted small">
				{datos.total} estudiante{datos.total === 1 ? '' : 's'} · página {datos.pagina + 1} de
				{Math.max(datos.totalPaginas, 1)}
			</span>
			<div class="acciones">
				<button
					class="btn btn-ghost btn-sm"
					type="button"
					disabled={datos.pagina === 0}
					onclick={() => irA(datos!.pagina - 1)}
				>
					Anterior
				</button>
				<button
					class="btn btn-ghost btn-sm"
					type="button"
					disabled={datos.pagina + 1 >= datos.totalPaginas}
					onclick={() => irA(datos!.pagina + 1)}
				>
					Siguiente
				</button>
			</div>
		</footer>
	{/if}
</section>

{#if qr}
	<QrModal credencial={qr} onclose={() => (qr = null)} />
{/if}

<style>
	.filtros {
		display: grid;
		grid-template-columns: 1fr 220px auto;
		gap: 10px;
		padding: 14px;
	}

	.vacio {
		padding: 44px 22px;
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
	}

	.vacio h2 {
		font-size: 18px;
	}

	.vacio p {
		margin: 0;
	}

	.paginacion {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 12px;
		padding: 13px 16px;
		border-top: 1px solid var(--kc-border);
		flex-wrap: wrap;
	}

	@media (max-width: 760px) {
		.filtros {
			grid-template-columns: 1fr;
		}
	}
</style>
