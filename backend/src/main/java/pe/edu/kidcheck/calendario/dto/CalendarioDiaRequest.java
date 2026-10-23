package pe.edu.kidcheck.calendario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalendarioDiaRequest(
        @NotNull(message = "es obligatorio")
        Boolean esLaborable,

        @Size(max = 100, message = "máximo 100 caracteres")
        String descripcion
) {
}
