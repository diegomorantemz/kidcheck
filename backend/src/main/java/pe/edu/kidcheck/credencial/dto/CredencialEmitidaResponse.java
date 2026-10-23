package pe.edu.kidcheck.credencial.dto;

public record CredencialEmitidaResponse(
        Long idCredencial,
        Long idEstudiante,
        String token,
        String qrPngBase64
) {
}
