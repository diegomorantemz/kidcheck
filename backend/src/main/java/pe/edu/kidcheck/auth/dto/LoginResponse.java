package pe.edu.kidcheck.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UsuarioInfo usuario,
        ColegioInfo colegio
) {
}
