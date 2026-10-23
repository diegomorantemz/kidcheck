package pe.edu.kidcheck.apoderado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VinculoRequest(
        @NotNull(message = "es obligatorio")
        Long idApoderado,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "padre|madre|apoderado|otro",
                message = "debe ser padre, madre, apoderado u otro")
        String parentesco,

        boolean esPrincipal,

        boolean autorizadoRecoger
) {
}
