<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/state';
	import { api, mensajeDeError } from '$lib/api';
	import type { JustificacionPublica } from '$lib/types';

	let datos = $state<JustificacionPublica | null>(null);
	let cargando = $state(true);
	let error = $state('');
	let enviando = $state(false);
	let enviada = $state('');
	let motivo = $state('enfermedad');
	let detalle = $state('');

	const NOMBRES: Record<string, string> = {
		enfermedad: 'Enfermedad',
		cita_medica: 'Cita médica',
		viaje: 'Viaje familiar',
		emergencia_familiar: 'Emergencia familiar',
		otro: 'Otro motivo'
	};

	onMount(async () => {
		try {
			datos = await api<JustificacionPublica>(
				`/justificaciones/mia?token=${encodeURIComponent(page.params.token ?? '')}`
			);
			motivo = datos.motivos[0] ?? 'enfermedad';
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	});

	async function enviar(evento: SubmitEvent) {
		evento.preventDefault();
		enviando = true;
		error = '';
		try {
			const respuesta = await api<{ mensaje: string }>('/justificaciones', {
				method: 'POST',
				body: {
					token: page.params.token,
					motivo,
					detalle: detalle.trim() || null
				}
			});
			enviada = respuesta.mensaje;
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			enviando = false;
		}
	}

	function fecha(valor?: string | null) {
		if (!valor) return '—';
		return new Date(valor + 'T12:00:00').toLocaleDateString('es-PE', {
			dateStyle: 'long'
		});
	}
</script>

<svelte:head>
	<title>Justificar inasistencia · KidCheck</title>
</svelte:head>

<main class="publico">
	<div class="tarjeta card">
		<div class="marca">
			<span class="logo">KC</span>
			<div>
				<strong>KidCheck</strong>
				<small>Justificación de inasistencia</small>
			</div>
		</div>

		{#if cargando}
			<p class="muted">Cargando el enlace…</p>
		{:else if enviada}
			<div class="resultado ok">
				<h1>Justificación enviada</h1>
				<p>{enviada}</p>
				<p class="muted small">Puedes cerrar esta página.</p>
			</div>
		{:else if error && !datos}
			<div class="resultado malo">
				<h1>Enlace no disponible</h1>
				<p>{error}</p>
				<p class="muted small">Solicita un enlace nuevo al colegio.</p>
			</div>
		{:else if datos}
			<div class="encabezado">
				<h1>Justificar ausencia</h1>
				<p class="muted">
					{datos.estudiante} · {datos.seccion ?? 'Sin sección'} · {fecha(datos.fecha)}
				</p>
				{#if datos.horaLimite}
					<p class="muted small">Hora límite de ingreso: {datos.horaLimite}</p>
				{/if}
			</div>

			{#if datos.yaEnviada}
				<div class="alerta ok">Ya registramos una justificación para esta ausencia.</div>
			{:else}
				{#if error}
					<div class="alerta" role="alert">{error}</div>
				{/if}
				<form onsubmit={enviar} class="formulario">
					<div class="field">
						<label class="label" for="motivo">Motivo</label>
						<select class="select" id="motivo" bind:value={motivo}>
							{#each datos.motivos as opcion (opcion)}
								<option value={opcion}>{NOMBRES[opcion] ?? opcion}</option>
							{/each}
						</select>
					</div>
					<div class="field">
						<label class="label" for="detalle">Detalle (opcional)</label>
						<textarea
							class="input"
							id="detalle"
							rows="4"
							maxlength="500"
							bind:value={detalle}
							placeholder="Cuéntanos brevemente qué ocurrió…"
						></textarea>
					</div>
					<button class="btn btn-primary" type="submit" disabled={enviando}>
						{enviando ? 'Enviando…' : 'Enviar justificación'}
					</button>
				</form>
			{/if}
		{/if}
	</div>
</main>

<style>
	.publico {
		min-height: 100vh;
		display: grid;
		place-items: center;
		padding: 22px;
		background:
			radial-gradient(circle at 15% 10%, rgba(25, 135, 84, 0.16), transparent 42%),
			var(--kc-bg);
	}

	.tarjeta {
		width: min(560px, 100%);
		padding: clamp(22px, 5vw, 38px);
		display: flex;
		flex-direction: column;
		gap: 22px;
	}

	.marca {
		display: flex;
		align-items: center;
		gap: 12px;
	}

	.logo {
		width: 46px;
		height: 46px;
		border-radius: 14px;
		background: var(--kc-green);
		color: #fff;
		display: grid;
		place-items: center;
		font-weight: 800;
	}

	.marca strong {
		display: block;
		font-size: 18px;
	}

	.marca small {
		color: var(--kc-muted);
		font-size: 12px;
	}

	.encabezado h1,
	.resultado h1 {
		font-size: 24px;
		margin-bottom: 6px;
	}

	.encabezado p,
	.resultado p {
		margin: 3px 0;
	}

	.formulario {
		display: flex;
		flex-direction: column;
		gap: 14px;
	}

	textarea.input {
		resize: vertical;
		min-height: 96px;
	}

	.resultado {
		padding: 18px;
		border-radius: 14px;
	}

	.resultado.ok {
		background: var(--kc-green-light);
	}

	.resultado.malo {
		background: var(--kc-danger-light);
	}
</style>
