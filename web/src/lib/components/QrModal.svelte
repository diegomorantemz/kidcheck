<script lang="ts">
	import type { CredencialEmitida } from '$lib/types';

	let { credencial, onclose }: { credencial: CredencialEmitida; onclose: () => void } = $props();

	let copiado = $state(false);
	const src = $derived(`data:image/png;base64,${credencial.qrPngBase64}`);

	async function copiar() {
		try {
			await navigator.clipboard.writeText(credencial.token);
			copiado = true;
			setTimeout(() => (copiado = false), 1600);
		} catch {
			copiado = false;
		}
	}
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div class="fondo" role="presentation" onclick={onclose} onkeydown={(e) => e.key === 'Escape' && onclose()}>
	<div
		class="caja card"
		role="dialog"
		aria-modal="true"
		aria-label="Credencial QR del estudiante"
		tabindex="-1"
		onclick={(e) => e.stopPropagation()}
		onkeydown={(e) => e.stopPropagation()}
	>
		<header>
			<h2>Credencial QR</h2>
			<p class="muted small">
				Imprime o descarga esta credencial. Por seguridad, el código no se vuelve a mostrar.
			</p>
		</header>

		<img class="qr" {src} alt="Código QR de la credencial" width="260" height="260" />

		<div class="token">
			<span class="label">Código de respaldo</span>
			<code>{credencial.token}</code>
		</div>

		<div class="acciones">
			<button class="btn btn-ghost" type="button" onclick={copiar}>
				{copiado ? 'Copiado' : 'Copiar código'}
			</button>
			<a class="btn btn-primary" href={src} download={`credencial-${credencial.idEstudiante}.png`}>
				Descargar PNG
			</a>
			<button class="btn btn-ghost" type="button" onclick={onclose}>Cerrar</button>
		</div>
	</div>
</div>

<style>
	.fondo {
		position: fixed;
		inset: 0;
		z-index: 50;
		background: rgba(18, 40, 28, 0.5);
		backdrop-filter: blur(3px);
		display: grid;
		place-items: center;
		padding: 20px;
	}

	.caja {
		width: min(420px, 100%);
		padding: 24px;
		display: flex;
		flex-direction: column;
		gap: 16px;
		text-align: center;
		background: #fff;
	}

	.caja header p {
		margin: 5px 0 0;
	}

	.qr {
		width: min(260px, 70vw);
		height: auto;
		margin: 0 auto;
		border: 1px solid var(--kc-border);
		border-radius: 12px;
		padding: 8px;
		background: #fff;
	}

	.token {
		display: flex;
		flex-direction: column;
		gap: 4px;
		align-items: center;
	}

	.token code {
		background: var(--kc-green-light);
		color: var(--kc-green-dark);
		padding: 6px 10px;
		border-radius: 8px;
		font-size: 14px;
		letter-spacing: 0.04em;
		word-break: break-all;
	}

	.acciones {
		display: flex;
		gap: 8px;
		justify-content: center;
		flex-wrap: wrap;
	}
</style>
