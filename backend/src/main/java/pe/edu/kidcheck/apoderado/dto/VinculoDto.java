package pe.edu.kidcheck.apoderado.dto;

import pe.edu.kidcheck.apoderado.EstudianteApoderado;

public record VinculoDto(
        Long idApoderado,
        String nombresApellidos,
        String telefonoWhatsapp,
        String email,
        String parentesco,
        boolean esPrincipal,
        boolean autorizadoRecoger
) {
    public static VinculoDto de(EstudianteApoderado vinculo) {
        var apoderado = vinculo.getApoderado();
        return new VinculoDto(
                apoderado.getId(),
                apoderado.getNombresApellidos(),
                apoderado.getTelefonoWhatsapp(),
                apoderado.getEmail(),
                vinculo.getParentesco(),
                vinculo.principal(),
                vinculo.autorizadoParaRecoger());
    }
}
