package pe.edu.kidcheck.estudiante.dto;

import pe.edu.kidcheck.apoderado.dto.VinculoDto;

import java.time.LocalDate;
import java.util.List;

public record EstudianteDto(
        Long id,
        String codigoMatricula,
        String nombres,
        String apellidos,
        String nombreCompleto,
        LocalDate fechaNacimiento,
        String estado,
        Long seccionId,
        String seccion,
        Long idCredencial,
        boolean credencialActiva,
        List<VinculoDto> apoderados
) {
}
