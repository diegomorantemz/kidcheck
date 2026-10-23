package pe.edu.kidcheck.reporte;

/**
 * Filas ya calculadas de un mes. Se usan tanto para el JSON como para el CSV
 * y así ambos formatos muestran exactamente los mismos números.
 */
record FilaMensual(
        Long idEstudiante,
        String codigoMatricula,
        String nombres,
        String apellidos,
        Long idSeccion,
        String grado,
        String letra,
        int diasRegistrados,
        int diasPresente,
        int diasTarde,
        int diasAusente,
        int diasJustificado,
        double porcentajeAsistencia
) {
    String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    String seccion() {
        return grado + "°" + letra;
    }
}
