package pe.edu.kidcheck.sync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;

public record SyncIngresosRequest(
        @NotBlank(message = "es obligatorio")
        String deviceUuid,

        @NotEmpty(message = "debe incluir al menos un ingreso")
        List<@Valid Item> ingresos
) {
    public record Item(
            @NotBlank(message = "es obligatorio")
            String clientId,

            @NotNull(message = "es obligatorio")
            Long idEstudiante,

            @NotNull(message = "es obligatorio")
            Instant fechaHoraUtc,

            @Pattern(regexp = "qr|manual", message = "debe ser qr o manual")
            String canal
    ) {
    }
}
