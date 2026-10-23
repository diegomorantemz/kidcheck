package pe.edu.kidcheck.alerta.dto;

import java.time.Instant;

public record AlertaDto(
        Long idAlerta,
        String tipo,
        String estado,
        Short umbral,
        Instant fechaGeneracion,
        Long idEstudiante,
        String estudiante,
        String seccion,
        String fechaAsistencia,
        String hora,
        JustificacionResumen justificacion
) {
    public record JustificacionResumen(
            Long id,
            String motivo,
            String detalle,
            String estado,
            Instant fechaEnvio
    ) {
    }
}
