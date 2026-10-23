package pe.edu.kidcheck.apoderado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApoderadoRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 150, message = "máximo 150 caracteres")
        String nombresApellidos,

        @Size(max = 20, message = "máximo 20 caracteres")
        String telefonoWhatsapp,

        @Email(message = "debe ser un correo válido")
        @Size(max = 150, message = "máximo 150 caracteres")
        String email
) {
}
