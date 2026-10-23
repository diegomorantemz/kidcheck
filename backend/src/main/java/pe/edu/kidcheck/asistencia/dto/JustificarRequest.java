package pe.edu.kidcheck.asistencia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JustificarRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "enfermedad|cita_medica|viaje|emergencia_familiar|otro",
                message = "motivo no válido")
        String motivo,

        @Size(max = 500, message = "máximo 500 caracteres")
        String detalle
) {
}
