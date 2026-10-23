package pe.edu.kidcheck.panel.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PanelDiaResponse(
        LocalDate fecha,
        Instant actualizadoEn,
        Resumen resumen,
        List<SeccionResumen> secciones,
        List<Fila> estudiantes
) {
    public record Resumen(
            int total,
            int presentes,
            int tarde,
            int ausentes,
            int justificados,
            int pendientes,
            int alertas
    ) {
    }

    public record SeccionResumen(
            Long id,
            String nombre,
            int total,
            int presentes,
            int tarde,
            int pendientes
    ) {
    }

    public record Fila(
            Long idEstudiante,
            String nombre,
            String seccion,
            String estado,
            String hora,
            boolean tarde,
            String apoderado,
            AlertaResumen alerta
    ) {
    }

    public record AlertaResumen(Long id, String tipo, String estado) {
    }
}
