package pe.edu.kidcheck.shared.security;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pe.edu.kidcheck.shared.error.ApiException;

/**
 * Datos del usuario autenticado tomados del JWT. No consulta la base de datos:
 * el token de acceso vive 15 minutos, así que un cambio de rol tarda como
 * máximo ese tiempo en reflejarse.
 */
@Component
public class CurrentUser {

    public Long usuarioId() {
        return numero("uid");
    }

    public Long colegioId() {
        return numero("colegio");
    }

    public String rol() {
        return texto("rol");
    }

    public String nombre() {
        return texto("nombre");
    }

    private Long numero(String clave) {
        Object valor = claims().get(clave);
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        throw noAutenticado();
    }

    private String texto(String clave) {
        return claims().get(clave, String.class);
    }

    private Claims claims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Claims claims) {
            return claims;
        }
        throw noAutenticado();
    }

    private static ApiException noAutenticado() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "No autenticado",
                "Inicia sesión para continuar.");
    }
}
