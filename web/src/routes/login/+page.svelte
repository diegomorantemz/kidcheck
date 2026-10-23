<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api, mensajeDeError } from '$lib/api';

	let usuario = $state('admin1');
	let password = $state('KidCheck2026!');
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
				<label class="label" for="password">Contraseña</label>
				<input
					class="input"
					id="password"
					name="password"
					type="password"
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
