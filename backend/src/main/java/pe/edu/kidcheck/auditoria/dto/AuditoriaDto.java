package pe.edu.kidcheck.auditoria.dto;

import java.time.Instant;
import java.util.Map;

public record AuditoriaDto(
        Long id,
        Instant fecha,
        Long idUsuario,
        String usuario,
        String entidad,
        Long idEntidad,
        String accion,
        Map<String, Object> datos,
        String ip
) {
}
