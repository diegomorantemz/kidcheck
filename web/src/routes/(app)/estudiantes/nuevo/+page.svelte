<script lang="ts">
	import EstudianteForm from '$lib/components/EstudianteForm.svelte';
</script>

<svelte:head>
	<title>Nuevo estudiante · KidCheck</title>
</svelte:head>

<header class="page-header">
	<div>
		<h1>Nuevo estudiante</h1>
		<p>Registra los datos, la sección y los apoderados del alumno.</p>
	</div>
	<a class="btn btn-ghost" href="/estudiantes">Volver</a>
</header>

<EstudianteForm />
