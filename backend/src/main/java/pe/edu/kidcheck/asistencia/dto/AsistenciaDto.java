package pe.edu.kidcheck.asistencia.dto;

public record AsistenciaDto(
        Long idAsistencia,
        Long idEstudiante,
        String nombre,
        String seccion,
        String estado,
        String hora,
        int minutosTarde,
        String origen,
        Long alertaId,
        String justificacionEstado
) {
}
