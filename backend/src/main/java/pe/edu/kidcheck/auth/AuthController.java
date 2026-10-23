package pe.edu.kidcheck.auth;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.auth.dto.LoginRequest;
import pe.edu.kidcheck.auth.dto.LoginResponse;
import pe.edu.kidcheck.auth.dto.RefreshRequest;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // JWT stateless: el cliente descarta sus tokens. Se deja el endpoint
        // para que la SPA tenga un punto único de cierre de sesión.
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("usuario", authentication.getName());
        body.put("autoridades", authentication.getAuthorities().stream()
                .map(Object::toString)
                .toList());

        if (authentication.getDetails() instanceof Claims claims) {
            body.put("id", claims.get(JwtTokenProvider.CLAIM_UID));
            body.put("nombre", claims.get(JwtTokenProvider.CLAIM_NOMBRE));
            body.put("rol", claims.get(JwtTokenProvider.CLAIM_ROL));
            body.put("colegio", claims.get(JwtTokenProvider.CLAIM_COLEGIO));
        }
        return body;
    }
}
