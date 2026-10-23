package pe.edu.kidcheck.justificacion.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record JustificacionPublicaResponse(
        String estudiante,
        String seccion,
        LocalDate fecha,
        String horaLimite,
        Instant expiraEn,
        List<String> motivos,
        boolean yaEnviada
) {
}
