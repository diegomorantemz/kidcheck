<script lang="ts">
	import { page } from '$app/state';
	import EstudianteForm from '$lib/components/EstudianteForm.svelte';

	const id = $derived(Number(page.params.id));
</script>

<svelte:head>
	<title>Editar estudiante · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Editar estudiante</h1>
		<p>Actualiza datos, sección o apoderados.</p>
	</div>
	<a class="btn btn-ghost" href="/estudiantes">Volver</a>
</header>

{#if Number.isFinite(id) && id > 0}
	<EstudianteForm {id} />
{:else}
	<div class="alerta">Estudiante inválido.</div>
{/if}
