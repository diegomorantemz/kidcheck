<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { api } from '$lib/api';
	import type { Me } from '$lib/types';

	let { children } = $props();

	let me = $state<Me | null>(null);
	let cargando = $state(true);

	const ROLES: Record<string, string> = {
		ADMINISTRADOR: 'Administrador',
		AUXILIAR: 'Auxiliar',
		PSICOLOGO: 'Psicólogo',
		VIGILANTE: 'Vigilante'
	};

	onMount(async () => {
		try {
			me = await api<Me>('/auth/me');
		} catch {
			await goto('/login', { replaceState: true });
		} finally {
			cargando = false;
		}
	});

	async function salir() {
		try {
			await api('/auth/logout', { method: 'POST' });
		} finally {
			await goto('/login');
		}
	}

	function activo(ruta: string) {
		return page.url.pathname.startsWith(ruta);
	}
</script>

{#if cargando}
	<div class="cargando">Cargando panel…</div>
{:else if me}
	<div class="shell">
		<aside class="lateral">
			<a class="marca" href="/estudiantes">
				<span class="logo">KC</span>
				<span>
					<strong>KidCheck</strong>
					<small>Panel del colegio</small>
				</span>
			</a>

			<nav>
				<a class:activo={activo('/estudiantes')} href="/estudiantes">Estudiantes</a>
				<a class:activo={activo('/apoderados')} href="/apoderados">Apoderados</a>
			</nav>

			<div class="usuario">
				<div class="avatar">{me.nombre.charAt(0)}</div>
				<div class="datos">
					<strong>{me.nombre}</strong>
					<span class="muted small">{ROLES[me.rol] ?? me.rol}</span>
				</div>
				<button class="btn btn-ghost btn-sm" onclick={salir}>Salir</button>
			</div>
		</aside>

		<main class="contenido">
			{@render children()}
		</main>
	</div>
{/if}

<style>
	.cargando {
		min-height: 100vh;
		display: grid;
		place-items: center;
		color: var(--kc-muted);
	}

	.shell {
		min-height: 100vh;
		display: grid;
		grid-template-columns: 256px 1fr;
	}

	.lateral {
		background: #fff;
		border-right: 1px solid var(--kc-border);
		padding: 22px 18px;
		display: flex;
		flex-direction: column;
		gap: 26px;
		position: sticky;
		top: 0;
		height: 100vh;
	}

	.marca {
		display: flex;
		align-items: center;
		gap: 11px;
		text-decoration: none;
		color: var(--kc-text);
	}

	.logo {
		width: 42px;
		height: 42px;
		border-radius: 13px;
		background: var(--kc-green);
		color: #fff;
		display: grid;
		place-items: center;
		font-weight: 800;
		font-size: 15px;
	}

	.marca strong {
		display: block;
		font-size: 17px;
		letter-spacing: -0.01em;
	}

	.marca small {
		color: var(--kc-muted);
		font-size: 12px;
	}

	nav {
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	nav a {
		padding: 10px 13px;
		border-radius: 10px;
		text-decoration: none;
		color: var(--kc-text);
		font-weight: 600;
		font-size: 14px;
	}

	nav a:hover {
		background: var(--kc-green-light);
	}

	nav a.activo {
		background: var(--kc-green-light);
		color: var(--kc-green-dark);
	}

	.usuario {
		margin-top: auto;
		border-top: 1px solid var(--kc-border);
		padding-top: 16px;
		display: flex;
		align-items: center;
		gap: 10px;
		flex-wrap: wrap;
	}

	.avatar {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		background: var(--kc-green-light);
		color: var(--kc-green-dark);
		display: grid;
		place-items: center;
		font-weight: 700;
	}

	.datos {
		display: flex;
		flex-direction: column;
		line-height: 1.25;
		flex: 1;
		min-width: 0;
	}

	.datos strong {
		font-size: 13px;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.contenido {
		padding: clamp(20px, 3vw, 40px);
		max-width: 1240px;
		width: 100%;
	}

	@media (max-width: 860px) {
		.shell {
			grid-template-columns: 1fr;
		}

		.lateral {
			position: static;
			height: auto;
			flex-direction: row;
			align-items: center;
			flex-wrap: wrap;
			gap: 14px;
			border-right: none;
			border-bottom: 1px solid var(--kc-border);
			padding: 14px 18px;
		}

		nav {
			flex-direction: row;
		}

		.usuario {
			margin-top: 0;
			margin-left: auto;
			border-top: none;
			padding-top: 0;
		}

		.datos {
			display: none;
		}
	}
</style>
