<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api, mensajeDeError } from '$lib/api';

	let usuario = $state('admin1');
	let password = $state('KidCheck2026!');
	let mostrarPassword = $state(false);
	let error = $state('');
	let enviando = $state(false);

	onMount(async () => {
		try {
			await api('/auth/me');
			await goto('/dashboard', { replaceState: true });
		} catch {
			// Sin sesión: se queda en el formulario.
		}
	});

	async function entrar(evento: SubmitEvent) {
		evento.preventDefault();
		error = '';
		enviando = true;

		try {
			await api('/auth/login', { method: 'POST', body: { usuario, password } });
			await goto('/dashboard');
		} catch (excepcion) {
			error = mensajeDeError(excepcion);
		} finally {
			enviando = false;
		}
	}
</script>

<svelte:head>
	<title>Ingresar · KidCheck</title>
</svelte:head>

<main class="login">
	<section class="marca">
		<div class="logo">KC</div>
		<h1>KidCheck</h1>
		<p>Control de ingreso escolar con credencial QR, alertas de inasistencia y avisos a las familias.</p>

		<ul>
			<li>Credencial QR sin datos biométricos</li>
			<li>Portería que funciona sin internet</li>
			<li>Alertas automáticas por WhatsApp</li>
		</ul>

		<p class="demo">Proyecto académico con datos ficticios</p>
	</section>

	<section class="panel">
		<form class="card formulario" onsubmit={entrar}>
			<header>
				<h2>Ingresa al panel</h2>
				<p class="muted small">Usa tu cuenta del colegio.</p>
			</header>

			{#if error}
				<div class="alerta" role="alert">{error}</div>
			{/if}

			<div class="field">
				<label class="label" for="usuario">Usuario</label>
				<input
					class="input"
					id="usuario"
					name="usuario"
					autocomplete="username"
					bind:value={usuario}
					required
				/>
			</div>

			<div class="field">
				<div class="label-fila">
					<label class="label" for="password">Contraseña</label>
					<button
						type="button"
						class="btn-ojo"
						aria-label={mostrarPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
						aria-pressed={mostrarPassword}
						onclick={() => (mostrarPassword = !mostrarPassword)}
					>
						{#if mostrarPassword}
							<svg viewBox="0 0 24 24" aria-hidden="true">
								<path
									d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"
								/>
							</svg>
						{:else}
							<svg viewBox="0 0 24 24" aria-hidden="true">
								<path
									d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"
								/>
							</svg>
						{/if}
					</button>
				</div>
				<input
					class="input"
					id="password"
					name="password"
					type={mostrarPassword ? 'text' : 'password'}
					autocomplete="current-password"
					bind:value={password}
					required
				/>
			</div>

			<button class="btn btn-primary" type="submit" disabled={enviando}>
				{enviando ? 'Ingresando…' : 'Ingresar'}
			</button>

			<p class="muted small demo-credenciales">
				Demo: <code>admin1</code> · <code>KidCheck2026!</code>
			</p>
		</form>
	</section>
</main>

<style>
	.login {
		min-height: 100vh;
		display: grid;
		grid-template-columns: 1.05fr 1fr;
	}

	.marca {
		background:
			radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.16), transparent 45%),
			linear-gradient(150deg, #1c9b63, #126b40 70%);
		color: #fff;
		padding: 56px clamp(28px, 5vw, 72px);
		display: flex;
		flex-direction: column;
		justify-content: center;
		gap: 18px;
	}

	.logo {
		width: 54px;
		height: 54px;
		border-radius: 16px;
		background: rgba(255, 255, 255, 0.16);
		border: 1px solid rgba(255, 255, 255, 0.35);
		display: grid;
		place-items: center;
		font-weight: 800;
		letter-spacing: 0.06em;
	}

	.marca h1 {
		font-size: clamp(32px, 4vw, 46px);
	}

	.marca p {
		max-width: 44ch;
		margin: 0;
		color: rgba(255, 255, 255, 0.88);
		font-size: 16px;
	}

	.marca ul {
		margin: 8px 0 0;
		padding-left: 20px;
		display: grid;
		gap: 8px;
		color: rgba(255, 255, 255, 0.92);
	}

	.marca .demo {
		margin-top: 26px;
		font-size: 13px;
		text-transform: uppercase;
		letter-spacing: 0.08em;
		color: rgba(255, 255, 255, 0.65);
	}

	.panel {
		display: grid;
		place-items: center;
		padding: 28px;
	}

	.formulario {
		width: min(420px, 100%);
		padding: 30px;
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	.formulario header p {
		margin: 4px 0 0;
	}

	.label-fila {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
	}

	.btn-ojo {
		display: grid;
		place-items: center;
		width: 32px;
		height: 32px;
		padding: 0;
		border: 0;
		border-radius: 8px;
		background: transparent;
		color: var(--kc-green-dark);
		cursor: pointer;
	}

	.btn-ojo:hover {
		background: var(--kc-green-light);
	}

	.btn-ojo svg {
		width: 20px;
		height: 20px;
		fill: currentColor;
	}

	.demo-credenciales {
		margin: 0;
		text-align: center;
	}

	code {
		background: var(--kc-green-light);
		padding: 1px 6px;
		border-radius: 5px;
	}

	@media (max-width: 860px) {
		.login {
			grid-template-columns: 1fr;
		}

		.marca {
			display: none;
		}
	}
</style>
