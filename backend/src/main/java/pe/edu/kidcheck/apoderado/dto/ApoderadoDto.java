package pe.edu.kidcheck.apoderado.dto;

import pe.edu.kidcheck.apoderado.Apoderado;

public record ApoderadoDto(
        Long id,
        String nombresApellidos,
        String telefonoWhatsapp,
        String email,
        String estado
) {
    public static ApoderadoDto de(Apoderado apoderado) {
        return new ApoderadoDto(
                apoderado.getId(),
                apoderado.getNombresApellidos(),
                apoderado.getTelefonoWhatsapp(),
                apoderado.getEmail(),
                apoderado.getEstado());
    }
}
