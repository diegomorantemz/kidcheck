package pe.edu.kidcheck.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.auth.dto.ColegioInfo;
import pe.edu.kidcheck.auth.dto.LoginRequest;
import pe.edu.kidcheck.auth.dto.LoginResponse;
import pe.edu.kidcheck.auth.dto.UsuarioInfo;
import pe.edu.kidcheck.dispositivo.Dispositivo;
import pe.edu.kidcheck.dispositivo.DispositivoRepository;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.usuario.Usuario;
import pe.edu.kidcheck.usuario.UsuarioRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INTENTOS = 5;

    private final UsuarioRepository usuarios;
    private final DispositivoRepository dispositivos;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByUsuarioLogin(request.usuario())
                .orElseThrow(() -> credencialesInvalidas());

        if (usuario.isBloqueado()) {
            throw new ApiException(HttpStatus.LOCKED, "CUENTA_BLOQUEADA", "Cuenta bloqueada",
                    "Demasiados intentos fallidos. Contacta al administrador del colegio.");
        }
        if (!usuario.isActivo()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CUENTA_INACTIVA", "Cuenta inactiva",
                    "La cuenta no está habilitada para iniciar sesión.");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getContrasenaHash())) {
            registrarIntentoFallido(usuario);
            throw credencialesInvalidas();
        }

        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(ahoraUtc());
        usuarios.save(usuario);

        registrarDispositivo(request.deviceUuid(), usuario);

        return toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw refreshInvalido();
        }

        if (!tokenProvider.isRefresh(claims)) {
            throw refreshInvalido();
        }

        Long usuarioId = ((Number) claims.get(JwtTokenProvider.CLAIM_UID)).longValue();
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(AuthService::refreshInvalido);

        if (!usuario.isActivo()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CUENTA_INACTIVA", "Cuenta inactiva",
                    "La cuenta no está habilitada para iniciar sesión.");
        }

        return toResponse(usuario);
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS) {
            usuario.setEstado("bloqueado");
        }
        usuarios.save(usuario);
    }

    private void registrarDispositivo(String deviceUuid, Usuario usuario) {
        if (deviceUuid == null || deviceUuid.isBlank()) {
            return;
        }
        String uuid = deviceUuid.trim().toLowerCase(Locale.ROOT);

        Dispositivo dispositivo = dispositivos.findByDeviceUuid(uuid)
                .orElseGet(() -> {
                    Dispositivo nuevo = new Dispositivo();
                    nuevo.setDeviceUuid(uuid);
                    nuevo.setColegio(usuario.getColegio());
                    nuevo.setTipo("tablet");
                    nuevo.setUbicacion("Portería");
                    nuevo.setEstado("activo");
                    return nuevo;
                });

        dispositivo.setUltimaSincronizacion(ahoraUtc());
        dispositivos.save(dispositivo);
    }

    private LoginResponse toResponse(Usuario usuario) {
        return new LoginResponse(
                tokenProvider.generateAccessToken(usuario),
                tokenProvider.generateRefreshToken(usuario),
                tokenProvider.accessSeconds(),
                new UsuarioInfo(usuario.getId(), usuario.getNombreCompleto(),
                        usuario.getRol().getNombre().toUpperCase()),
                new ColegioInfo(usuario.getColegio().getId(), usuario.getColegio().getNombre()));
    }

    private static ApiException credencialesInvalidas() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Credenciales inválidas",
                "Usuario o contraseña incorrectos.");
    }

    private static ApiException refreshInvalido() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALIDO", "Sesión expirada",
                "El token de renovación no es válido o expiró. Vuelve a iniciar sesión.");
    }

    private static LocalDateTime ahoraUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
