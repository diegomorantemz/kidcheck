<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api, mensajeDeError } from '$lib/api';
	import type { Apoderado, Estudiante, PageResponse, Seccion } from '$lib/types';

	let { id = null }: { id?: number | null } = $props();

	interface VinculoForm {
		idApoderado: number | '';
		parentesco: string;
		esPrincipal: boolean;
		autorizadoRecoger: boolean;
	}

	let cargando = $state(true);
	let guardando = $state(false);
	let error = $state('');
	let ok = $state('');

	let codigoMatricula = $state('');
	let nombres = $state('');
	let apellidos = $state('');
	let fechaNacimiento = $state('');
	let seccionId = $state<number | ''>('');
	let estado = $state('activo');
	let generarQr = $state(true);

	let secciones = $state<Seccion[]>([]);
	let apoderados = $state<Apoderado[]>([]);
	let vinculos = $state<VinculoForm[]>([]);

	onMount(async () => {
		try {
			const [listaSecciones, listaApoderados] = await Promise.all([
				api<Seccion[]>('/secciones'),
				api<PageResponse<Apoderado>>('/apoderados?size=100')
			]);
			secciones = listaSecciones;
			apoderados = listaApoderados.contenido;

			if (id) {
				const estudiante = await api<Estudiante>(`/estudiantes/${id}`);
				codigoMatricula = estudiante.codigoMatricula;
				nombres = estudiante.nombres;
				apellidos = estudiante.apellidos;
				fechaNacimiento = estudiante.fechaNacimiento ?? '';
				seccionId = estudiante.seccionId ?? '';
				estado = estudiante.estado;
				vinculos = estudiante.apoderados.map((v) => ({
					idApoderado: v.idApoderado,
					parentesco: v.parentesco,
					esPrincipal: v.esPrincipal,
					autorizadoRecoger: v.autorizadoRecoger
				}));
			} else if (secciones.length > 0) {
				seccionId = secciones[0].id;
			}
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	});

	function agregarVinculo() {
		const usados = new Set(vinculos.map((v) => v.idApoderado));
		const libre = apoderados.find((a) => !usados.has(a.id));
		vinculos.push({
			idApoderado: libre?.id ?? '',
			parentesco: 'madre',
			esPrincipal: vinculos.length === 0,
			autorizadoRecoger: true
		});
	}

	function quitarVinculo(indice: number) {
		vinculos.splice(indice, 1);
	}

	async function guardar(evento: SubmitEvent) {
		evento.preventDefault();
		error = '';
		ok = '';

		const usados = new Set<number>();
		for (const vinculo of vinculos) {
			if (vinculo.idApoderado === '') {
				error = 'Selecciona un apoderado en cada vínculo.';
				return;
			}
			if (usados.has(Number(vinculo.idApoderado))) {
				error = 'No puedes vincular al mismo apoderado dos veces.';
				return;
			}
			usados.add(Number(vinculo.idApoderado));
		}

		const cuerpo = {
			codigoMatricula,
			nombres,
			apellidos,
			fechaNacimiento: fechaNacimiento || null,
			seccionId: seccionId === '' ? null : Number(seccionId),
			estado,
			apoderados: vinculos.map((vinculo) => ({
				idApoderado: Number(vinculo.idApoderado),
				parentesco: vinculo.parentesco,
				esPrincipal: vinculo.esPrincipal,
				autorizadoRecoger: vinculo.autorizadoRecoger
			}))
		};

		guardando = true;
		try {
			if (id) {
				await api(`/estudiantes/${id}`, { method: 'PUT', body: cuerpo });
				ok = 'Cambios guardados.';
			} else {
				const creado = await api<Estudiante>('/estudiantes', { method: 'POST', body: cuerpo });
				await goto(generarQr ? `/estudiantes?qr=${creado.id}` : '/estudiantes');
			}
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			guardando = false;
		}
	}
</script>

{#if cargando}
	<p class="muted">Cargando formulario…</p>
{:else}
	<form class="formulario" onsubmit={guardar}>
		<section class="card bloque">
			<h2>Datos del estudiante</h2>

			<div class="grid">
				<div class="field">
					<label class="label" for="codigo">Código de matrícula</label>
					<input class="input" id="codigo" bind:value={codigoMatricula} maxlength="20" required />
				</div>

				<div class="field">
					<label class="label" for="seccion">Sección</label>
					<select class="select" id="seccion" bind:value={seccionId} required>
						<option value="">Selecciona…</option>
						{#each secciones as seccion (seccion.id)}
							<option value={seccion.id}>{seccion.nombre} · {seccion.periodo}</option>
						{/each}
					</select>
				</div>

				<div class="field">
					<label class="label" for="nombres">Nombres</label>
					<input class="input" id="nombres" bind:value={nombres} maxlength="100" required />
				</div>

				<div class="field">
					<label class="label" for="apellidos">Apellidos</label>
					<input class="input" id="apellidos" bind:value={apellidos} maxlength="100" required />
				</div>

				<div class="field">
					<label class="label" for="nacimiento">Fecha de nacimiento</label>
					<input class="input" id="nacimiento" type="date" bind:value={fechaNacimiento} />
				</div>

				{#if id}
					<div class="field">
						<label class="label" for="estado">Estado</label>
						<select class="select" id="estado" bind:value={estado}>
							<option value="activo">Activo</option>
							<option value="inactivo">Inactivo</option>
							<option value="egresado">Egresado</option>
						</select>
					</div>
				{/if}
			</div>
		</section>

		<section class="card bloque">
			<div class="titulo-fila">
				<div>
					<h2>Apoderados</h2>
					<p class="muted small">Personas autorizadas a recoger al estudiante y recibir avisos.</p>
				</div>
				<button class="btn btn-ghost btn-sm" type="button" onclick={agregarVinculo}>
					+ Agregar vínculo
				</button>
			</div>

			{#if apoderados.length === 0}
				<p class="alerta">
					No hay apoderados registrados. Crea uno primero en la sección <a href="/apoderados">Apoderados</a>.
				</p>
			{:else if vinculos.length === 0}
				<p class="muted small">Sin apoderados vinculados.</p>
			{:else}
				<div class="vinculos">
					{#each vinculos as vinculo, indice (indice)}
						<div class="vinculo">
							<select class="select" bind:value={vinculo.idApoderado} aria-label="Apoderado">
								<option value="">Selecciona…</option>
								{#each apoderados as apoderado (apoderado.id)}
									<option value={apoderado.id}>{apoderado.nombresApellidos}</option>
								{/each}
							</select>

							<select class="select" bind:value={vinculo.parentesco} aria-label="Parentesco">
								<option value="padre">Padre</option>
								<option value="madre">Madre</option>
								<option value="apoderado">Apoderado</option>
								<option value="otro">Otro</option>
							</select>

							<label class="check">
								<input type="checkbox" bind:checked={vinculo.esPrincipal} />
								Principal
							</label>

							<label class="check">
								<input type="checkbox" bind:checked={vinculo.autorizadoRecoger} />
								Autorizado a recoger
							</label>

							<button
								class="btn btn-danger btn-sm"
								type="button"
								onclick={() => quitarVinculo(indice)}
							>
								Quitar
							</button>
						</div>
					{/each}
				</div>
			{/if}
		</section>

		{#if error}
			<div class="alerta" role="alert">{error}</div>
		{/if}
		{#if ok}
			<div class="alerta ok" role="status">{ok}</div>
		{/if}

		{#if !id}
			<label class="check">
				<input type="checkbox" bind:checked={generarQr} />
				Generar la credencial QR al guardar
			</label>
		{/if}

		<div class="pie">
			<a class="btn btn-ghost" href="/estudiantes">Cancelar</a>
			<button class="btn btn-primary" type="submit" disabled={guardando}>
				{guardando ? 'Guardando…' : id ? 'Guardar cambios' : 'Registrar estudiante'}
			</button>
		</div>
	</form>
{/if}

<style>
	.formulario {
		display: flex;
		flex-direction: column;
		gap: 18px;
		max-width: 960px;
	}

	.bloque {
		padding: 22px;
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	.grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
		gap: 14px;
	}

	.titulo-fila {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 12px;
		flex-wrap: wrap;
	}

	.titulo-fila p {
		margin: 4px 0 0;
	}

	.vinculos {
		display: flex;
		flex-direction: column;
		gap: 10px;
	}

	.vinculo {
		display: grid;
		grid-template-columns: 1.5fr 1fr auto auto auto;
		gap: 10px;
		align-items: center;
		padding: 10px;
		border: 1px solid var(--kc-border);
		border-radius: 12px;
		background: #fafcfa;
	}

	.pie {
		display: flex;
		justify-content: flex-end;
		gap: 10px;
	}

	@media (max-width: 760px) {
		.vinculo {
			grid-template-columns: 1fr 1fr;
		}
	}
</style>
