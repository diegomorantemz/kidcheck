package pe.edu.kidcheck.sync.dto;

import java.util.List;

public record RosterResponse(
        String version,
        ColegioInfo colegio,
        List<EstudianteRoster> estudiantes
) {
    public record ColegioInfo(Long id, String zonaHoraria) {
    }

    public record EstudianteRoster(
            Long idEstudiante,
            String codigoMatricula,
            String nombres,
            String apellidos,
            String tokenHash,
            String seccion,
            String horaLimite,
            String fotoUrl
    ) {
    }
}
