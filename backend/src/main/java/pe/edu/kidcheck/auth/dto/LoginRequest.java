package pe.edu.kidcheck.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 50, message = "máximo 50 caracteres")
        String usuario,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "máximo 100 caracteres")
        String password,

        @Size(max = 36, message = "máximo 36 caracteres")
        String deviceUuid
) {
}
