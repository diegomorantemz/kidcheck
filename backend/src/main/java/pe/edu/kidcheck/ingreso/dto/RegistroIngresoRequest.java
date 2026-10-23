package pe.edu.kidcheck.ingreso.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RegistroIngresoRequest(
        @NotNull(message = "es obligatorio")
        Long idEstudiante,

        String deviceUuid,

        @NotNull(message = "es obligatorio")
        Instant fechaHoraUtc,

        @Pattern(regexp = "qr|manual", message = "debe ser qr o manual")
        String canal,

        @Size(max = 255, message = "debe tener como máximo 255 caracteres")
        String observacion
) {
}
