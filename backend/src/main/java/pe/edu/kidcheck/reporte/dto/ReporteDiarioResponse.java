package pe.edu.kidcheck.reporte.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ReporteDiarioResponse(
        LocalDate fecha,
        Instant generadoEn,
        Resumen resumen,
        List<SeccionResumen> secciones
) {
    public record Resumen(
            int total,
            int presentes,
            int tarde,
            int ausentes,
            int justificados,
            int pendientes,
            int alertas,
            double porcentajeAsistencia
    ) {
    }

    public record SeccionResumen(
            Long id,
            String nombre,
            int total,
            int presentes,
            int tarde,
            int ausentes,
            int justificados,
            int pendientes,
            double porcentajeAsistencia
    ) {
    }
}
