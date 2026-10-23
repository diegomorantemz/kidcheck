<script lang="ts">
	import { onMount } from 'svelte';
	import { api, mensajeDeError } from '$lib/api';
	import type { CalendarioDia, Configuracion, Seccion } from '$lib/types';

	let configuraciones = $state<Configuracion[]>([]);
	let secciones = $state<Seccion[]>([]);
	let calendario = $state<CalendarioDia[]>([]);
	let cargando = $state(true);
	let error = $state('');
	let ok = $state('');
	let guardando = $state('');

	let nuevoDia = $state(fechaLocalLima());
	let nuevoLaborable = $state(true);
	let nuevaDescripcion = $state('');

	function fechaLocalLima() {
		return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(new Date());
	}

	function anioActual() {
		return Number(fechaLocalLima().slice(0, 4));
	}

	onMount(cargar);

	async function cargar() {
		cargando = true;
		error = '';
		try {
			const [configuracion, listaSecciones, dias] = await Promise.all([
				api<Configuracion[]>('/configuracion'),
				api<Seccion[]>('/secciones'),
				api<CalendarioDia[]>(`/calendario?anio=${anioActual()}`)
			]);
			configuraciones = configuracion;
			secciones = listaSecciones.map((seccion) => ({
				...seccion,
				horaLimiteIngreso: seccion.horaLimiteIngreso.slice(0, 5)
			}));
			calendario = dias;
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			cargando = false;
		}
	}

	function valor(clave: string) {
		return configuraciones.find((item) => item.clave === clave)?.valor ?? '';
	}

	function fijarValor(clave: string, valorNuevo: string) {
		const existente = configuraciones.find((item) => item.clave === clave);
		if (existente) {
			existente.valor = valorNuevo;
		} else {
			configuraciones = [...configuraciones, { clave, valor: valorNuevo }];
		}
	}

	async function guardarConfiguracion(clave: string) {
		guardando = clave;
		error = '';
		ok = '';
		try {
			const actualizado = await api<Configuracion>(`/configuracion/${clave}`, {
				method: 'PUT',
				body: { valor: valor(clave) }
			});
			fijarValor(actualizado.clave, actualizado.valor);
			ok = `Configuración "${clave}" guardada.`;
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			guardando = '';
		}
	}

	async function guardarSeccion(seccion: Seccion) {
		guardando = `seccion-${seccion.id}`;
		error = '';
		ok = '';
		try {
			const actualizada = await api<Seccion>(`/secciones/${seccion.id}`, {
				method: 'PUT',
				body: {
					horaLimiteIngreso: `${seccion.horaLimiteIngreso}:00`,
					toleranciaMinutos: Number(seccion.toleranciaMinutos)
				}
			});
			secciones = secciones.map((item) =>
				item.id === actualizada.id
					? { ...actualizada, horaLimiteIngreso: actualizada.horaLimiteIngreso.slice(0, 5) }
					: item
			);
			ok = `Horario de ${actualizada.nombre} actualizado.`;
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			guardando = '';
		}
	}

	async function guardarDia(evento: SubmitEvent) {
		evento.preventDefault();
		guardando = 'calendario';
		error = '';
		ok = '';
		try {
			const dia = await api<CalendarioDia>(`/calendario/${nuevoDia}`, {
				method: 'PUT',
				body: {
					esLaborable: nuevoLaborable,
					descripcion: nuevaDescripcion.trim() || null
				}
			});
			const otros = calendario.filter((item) => item.fecha !== dia.fecha);
			calendario = [...otros, dia].sort((a, b) => a.fecha.localeCompare(b.fecha));
			nuevaDescripcion = '';
			ok = `${dia.fecha} marcado como ${dia.esLaborable ? 'laborable' : 'no laborable'}.`;
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			guardando = '';
		}
	}
</script>

<svelte:head>
	<title>Configuración · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Configuración</h1>
		<p>Horarios, tolerancia, calendario escolar y canal de notificación.</p>
	</div>
	<a class="btn btn-ghost" href="/reportes">Ver reportes</a>
</header>

{#if error}
	<div class="alerta" role="alert" style="margin-bottom: 16px">{error}</div>
{/if}
{#if ok}
	<div class="alerta ok" role="status" style="margin-bottom: 16px">{ok}</div>
{/if}

{#if cargando}
	<p class="muted" style="padding: 28px">Cargando configuración…</p>
{:else}
	<section class="grid-dos">
		<form
			class="card panel"
			onsubmit={(evento) => {
				evento.preventDefault();
				guardarConfiguracion('umbral_reincidencia');
			}}
		>
			<h2>Reglas de alerta</h2>
			<p class="muted small">
				El umbral define cuántas ausencias sin justificar en 30 días generan una reincidencia.
			</p>
			<div class="field">
				<label class="label" for="umbral">Umbral de reincidencia</label>
				<input
					class="input"
					id="umbral"
					type="number"
					min="1"
					max="10"
					value={valor('umbral_reincidencia')}
					oninput={(evento) => fijarValor('umbral_reincidencia', evento.currentTarget.value)}
				/>
			</div>
			<button class="btn btn-primary" type="submit" disabled={guardando === 'umbral_reincidencia'}>
				{guardando === 'umbral_reincidencia' ? 'Guardando…' : 'Guardar umbral'}
			</button>
		</form>

		<form
			class="card panel"
			onsubmit={(evento) => {
				evento.preventDefault();
				guardarConfiguracion('canal_principal');
			}}
		>
			<h2>Canal de notificación</h2>
			<p class="muted small">
				WhatsApp es el canal principal; el correo se usa como respaldo si el apoderado no tiene número.
			</p>
			<div class="field">
				<label class="label" for="canal">Canal principal</label>
				<select
					class="select"
					id="canal"
					value={valor('canal_principal')}
					onchange={(evento) => fijarValor('canal_principal', evento.currentTarget.value)}
				>
					<option value="whatsapp">WhatsApp</option>
					<option value="email">Correo electrónico</option>
				</select>
			</div>
			<div class="field">
				<span class="label">Zona horaria</span>
				<span class="badge">{valor('zona_horaria') || 'America/Lima'}</span>
			</div>
			<button class="btn btn-primary" type="submit" disabled={guardando === 'canal_principal'}>
				{guardando === 'canal_principal' ? 'Guardando…' : 'Guardar canal'}
			</button>
		</form>
	</section>

	<section class="card" style="margin-top: 16px">
		<div class="panel-header">
			<div>
				<h2>Secciones y horarios</h2>
				<p class="muted small">Hora límite de ingreso y tolerancia para la alerta de inasistencia.</p>
			</div>
		</div>
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>Sección</th>
						<th>Hora límite</th>
						<th>Tolerancia (min)</th>
						<th style="text-align: right">Acciones</th>
					</tr>
				</thead>
				<tbody>
					{#each secciones as seccion (seccion.id)}
						<tr>
							<td>
								<strong>{seccion.nombre}</strong>
								<div class="muted small">Periodo {seccion.periodo}</div>
							</td>
							<td>
								<input
									class="input"
									type="time"
									aria-label={`Hora límite de ${seccion.nombre}`}
									bind:value={seccion.horaLimiteIngreso}
								/>
							</td>
							<td>
								<input
									class="input"
									type="number"
									min="0"
									max="120"
									aria-label={`Tolerancia de ${seccion.nombre}`}
									bind:value={seccion.toleranciaMinutos}
								/>
							</td>
							<td>
								<div class="acciones">
									<button
										class="btn btn-ghost btn-sm"
										type="button"
										disabled={guardando === `seccion-${seccion.id}`}
										onclick={() => guardarSeccion(seccion)}
									>
										{guardando === `seccion-${seccion.id}` ? 'Guardando…' : 'Guardar'}
									</button>
								</div>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	</section>

	<section class="grid-dos" style="margin-top: 16px">
		<form class="card panel" onsubmit={guardarDia}>
			<h2>Calendario escolar</h2>
			<p class="muted small">
				Los días no laborables no generan alertas de inasistencia.
			</p>
			<div class="field">
				<label class="label" for="dia">Fecha</label>
				<input class="input" id="dia" type="date" bind:value={nuevoDia} />
			</div>
			<label class="check">
				<input type="checkbox" bind:checked={nuevoLaborable} />
				Es día laborable
			</label>
			<div class="field">
				<label class="label" for="descripcion">Descripción</label>
				<input
					class="input"
					id="descripcion"
					maxlength="100"
					placeholder="Feriado, suspensión, clase…"
					bind:value={nuevaDescripcion}
				/>
			</div>
			<button class="btn btn-primary" type="submit" disabled={guardando === 'calendario'}>
				{guardando === 'calendario' ? 'Guardando…' : 'Guardar día'}
			</button>
		</form>

		<div class="card panel">
			<h2>Días configurados</h2>
			<p class="muted small">Año {anioActual()}.</p>
			{#if calendario.length === 0}
				<div class="vacio"><p class="muted">Aún no hay días configurados.</p></div>
			{:else}
				<div class="lista-dias">
					{#each calendario as dia (dia.fecha)}
						<div class="dia">
							<div>
								<strong>{dia.fecha}</strong>
								<div class="muted small">{dia.descripcion || 'Sin descripción'}</div>
							</div>
							<span class="badge" class:badge-danger={!dia.esLaborable}>
								{dia.esLaborable ? 'Laborable' : 'No laborable'}
							</span>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	</section>
{/if}

<style>
	.grid-dos {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
		gap: 16px;
	}

	.panel {
		padding: 20px;
		display: flex;
		flex-direction: column;
		gap: 14px;
	}

	.panel h2 {
		font-size: 17px;
	}

	.panel .btn {
		align-self: flex-start;
	}

	.panel-header {
		padding: 20px 20px 0;
	}

	.lista-dias {
		display: flex;
		flex-direction: column;
		gap: 8px;
		max-height: 320px;
		overflow-y: auto;
	}

	.dia {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		padding: 10px 12px;
		border: 1px solid var(--kc-border);
		border-radius: 10px;
	}

	.vacio {
		padding: 24px;
		text-align: center;
	}
</style>
