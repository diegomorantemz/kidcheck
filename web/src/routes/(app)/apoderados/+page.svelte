<script lang="ts">
	import { onMount } from 'svelte';
	import { api, mensajeDeError } from '$lib/api';
	import type { Apoderado, PageResponse } from '$lib/types';

	let q = $state('');
	let apoderados = $state<Apoderado[]>([]);
	let cargando = $state(true);
	let guardando = $state(false);
	let error = $state('');
	let ok = $state('');

	let mostrarFormulario = $state(false);
	let editandoId = $state<number | null>(null);
	let nombresApellidos = $state('');
	let telefonoWhatsapp = $state('');
	let email = $state('');

	onMount(cargar);

	async function cargar() {
		cargando = true;
		error = '';
		try {
			const parametros = new URLSearchParams({ size: '50' });
			if (q.trim()) parametros.set('q', q.trim());
			const datos = await api<PageResponse<Apoderado>>(`/apoderados?${parametros}`);
			apoderados = datos.contenido;
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	}

	function nuevo() {
		editandoId = null;
		nombresApellidos = '';
		telefonoWhatsapp = '';
		email = '';
		mostrarFormulario = true;
		ok = '';
		error = '';
	}

	function editar(apoderado: Apoderado) {
		editandoId = apoderado.id;
		nombresApellidos = apoderado.nombresApellidos;
		telefonoWhatsapp = apoderado.telefonoWhatsapp ?? '';
		email = apoderado.email ?? '';
		mostrarFormulario = true;
		ok = '';
		error = '';
	}

	async function guardar(evento: SubmitEvent) {
		evento.preventDefault();
		error = '';
		ok = '';
		guardando = true;

		const cuerpo = {
			nombresApellidos,
			telefonoWhatsapp: telefonoWhatsapp.trim() || null,
			email: email.trim() || null
		};

		try {
			if (editandoId) {
				await api(`/apoderados/${editandoId}`, { method: 'PUT', body: cuerpo });
				ok = 'Apoderado actualizado.';
			} else {
				await api('/apoderados', { method: 'POST', body: cuerpo });
				ok = 'Apoderado registrado.';
			}
			mostrarFormulario = false;
			await cargar();
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			guardando = false;
		}
	}
</script>

<svelte:head>
	<title>Apoderados · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Apoderados</h1>
		<p>Contactos de las familias para avisos por WhatsApp y recojo autorizado.</p>
	</div>
	<button class="btn btn-primary" type="button" onclick={nuevo}>+ Nuevo apoderado</button>
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}
{#if ok}
	<div class="alerta ok" role="status" style="margin-bottom: 16px">{ok}</div>
{/if}

{#if mostrarFormulario}
	<form class="card formulario" onsubmit={guardar}>
		<div>
			<h2>{editandoId ? 'Editar apoderado' : 'Nuevo apoderado'}</h2>
			<p class="muted small">El teléfono debe incluir el código de país (ej. +51911111111).</p>
		</div>

		<div class="grid">
			<div class="field">
				<label class="label" for="nombre">Nombres y apellidos</label>
				<input class="input" id="nombre" bind:value={nombresApellidos} maxlength="150" required />
			</div>
			<div class="field">
				<label class="label" for="telefono">Teléfono WhatsApp</label>
				<input class="input" id="telefono" bind:value={telefonoWhatsapp} maxlength="20" />
			</div>
			<div class="field">
				<label class="label" for="email">Correo</label>
				<input class="input" id="email" type="email" bind:value={email} maxlength="150" />
			</div>
		</div>

		<div class="acciones">
			<button class="btn btn-ghost" type="button" onclick={() => (mostrarFormulario = false)}>
				Cancelar
			</button>
			<button class="btn btn-primary" type="submit" disabled={guardando}>
				{guardando ? 'Guardando…' : editandoId ? 'Guardar cambios' : 'Registrar'}
			</button>
		</div>
	</form>
{/if}

<form class="card filtros" onsubmit={(e) => { e.preventDefault(); cargar(); }}>
	<input
		class="input"
		type="search"
		placeholder="Buscar por nombre o teléfono…"
		aria-label="Buscar apoderado"
		bind:value={q}
	/>
	<button class="btn btn-ghost" type="submit">Buscar</button>
</form>

<section class="card" style="margin-top: 16px">
	{#if cargando}
		<p class="muted" style="padding: 28px">Cargando apoderados…</p>
	{:else if apoderados.length === 0}
		<div class="vacio">
			<h2>Sin apoderados</h2>
			<p class="muted">Registra al primer contacto familiar.</p>
			<button class="btn btn-primary" type="button" onclick={nuevo}>Registrar apoderado</button>
		</div>
	{:else}
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Apoderado</th>
						<th>WhatsApp</th>
						<th>Correo</th>
						<th>Estado</th>
						<th style="text-align: right">Acciones</th>
					</tr>
				</thead>
				<tbody>
					{#each apoderados as apoderado (apoderado.id)}
						<tr>
							<td><strong>{apoderado.nombresApellidos}</strong></td>
							<td>{apoderado.telefonoWhatsapp ?? '—'}</td>
							<td class="muted">{apoderado.email ?? '—'}</td>
							<td>
								<span class="badge" class:off={apoderado.estado !== 'activo'}>
									{apoderado.estado}
								</span>
							</td>
							<td>
								<div class="acciones">
									<button
										class="btn btn-ghost btn-sm"
										type="button"
										onclick={() => editar(apoderado)}
									>
										Editar
									</button>
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
	.formulario {
		padding: 20px;
		margin-bottom: 16px;
		display: flex;
		flex-direction: column;
		gap: 14px;
	}

	.grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
		gap: 14px;
	}

	.filtros {
		display: grid;
		grid-template-columns: 1fr auto;
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
</style>
