package pe.edu.kidcheck.reporte.dto;

import java.time.Instant;
import java.util.List;

public record ReporteMensualResponse(
        String periodo,
        Instant generadoEn,
        Resumen resumen,
        List<SeccionResumen> secciones,
        List<Fila> estudiantes
) {
    public record Resumen(
            int estudiantes,
            int diasRegistrados,
            int diasPresente,
            int diasTarde,
            int diasAusente,
            int diasJustificado,
            int inasistencias,
            int reincidencias,
            double porcentajeAsistencia
    ) {
    }

    public record SeccionResumen(
            Long id,
            String nombre,
            int estudiantes,
            int diasRegistrados,
            int diasPresente,
            int diasAusente,
            double porcentajeAsistencia
    ) {
    }

    public record Fila(
            Long idEstudiante,
            String codigoMatricula,
            String nombre,
            Long idSeccion,
            String seccion,
            int diasRegistrados,
            int diasPresente,
            int diasTarde,
            int diasAusente,
            int diasJustificado,
            double porcentajeAsistencia,
            int inasistencias,
            int reincidencias
    ) {
    }
}
