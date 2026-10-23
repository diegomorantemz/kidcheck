package pe.edu.kidcheck.alerta.dto;

import jakarta.validation.constraints.Size;

public record DerivarRequest(
        @Size(max = 255, message = "máximo 255 caracteres")
        String comentario
) {
}
