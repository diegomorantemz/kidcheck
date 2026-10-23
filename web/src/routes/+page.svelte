<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';

	onMount(() => {
		goto('/estudiantes', { replaceState: true });
	});
</script>

<p class="muted" style="padding: 48px; text-align: center">Cargando KidCheck…</p>
