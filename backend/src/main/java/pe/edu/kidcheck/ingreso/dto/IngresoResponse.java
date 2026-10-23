package pe.edu.kidcheck.ingreso.dto;

public record IngresoResponse(
        Long idIngreso,
        String estadoAsistencia,
        String horaLocal,
        String mensaje,
        boolean notificacionEncolada
) {
}
