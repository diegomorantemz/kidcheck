package pe.edu.kidcheck.justificacion.dto;

import java.time.Instant;
import java.time.LocalDate;

public record JustificacionDto(
        Long id,
        Long idAlerta,
        Long idEstudiante,
        String estudiante,
        String seccion,
        LocalDate fecha,
        String motivo,
        String detalle,
        String estado,
        String apoderado,
        Instant fechaEnvio,
        Instant fechaRevision
) {
}
