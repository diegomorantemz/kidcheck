package pe.edu.kidcheck.calendario.dto;

import java.time.LocalDate;

public record CalendarioDiaDto(
        LocalDate fecha,
        boolean esLaborable,
        String descripcion
) {
}
