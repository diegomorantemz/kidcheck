package pe.edu.kidcheck.estudiante.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.kidcheck.apoderado.dto.VinculoRequest;

import java.time.LocalDate;
import java.util.List;

public record EstudianteRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 20, message = "máximo 20 caracteres")
        String codigoMatricula,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "máximo 100 caracteres")
        String nombres,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "máximo 100 caracteres")
        String apellidos,

        LocalDate fechaNacimiento,

        Long seccionId,

        @Pattern(regexp = "activo|inactivo|egresado",
                message = "debe ser activo, inactivo o egresado")
        String estado,

        @Valid
        List<VinculoRequest> apoderados
) {
}
