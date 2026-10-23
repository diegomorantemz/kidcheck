package pe.edu.kidcheck.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.kidcheck.usuario.Usuario;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    public static final String CLAIM_TIPO = "typ";
    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";
    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_COLEGIO = "colegio";
    public static final String CLAIM_NOMBRE = "nombre";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokenProvider(
            @Value("${kidcheck.jwt.secret}") String secret,
            @Value("${kidcheck.jwt.access-minutes:15}") long accessMinutes,
            @Value("${kidcheck.jwt.refresh-hours:8}") long refreshHours) {

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("kidcheck.jwt.secret debe tener al menos 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = Duration.ofMinutes(accessMinutes);
        this.refreshTtl = Duration.ofHours(refreshHours);
    }

    public String generateAccessToken(Usuario usuario) {
        return build(usuario, TIPO_ACCESS, accessTtl);
    }

    public String generateRefreshToken(Usuario usuario) {
        return build(usuario, TIPO_REFRESH, refreshTtl);
    }

    private String build(Usuario usuario, String tipo, Duration ttl) {
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_UID, usuario.getId());
        claims.put(CLAIM_ROL, usuario.getRol().getNombre().toUpperCase());
        claims.put(CLAIM_COLEGIO, usuario.getColegio().getId());
        claims.put(CLAIM_NOMBRE, usuario.getNombreCompleto());
        claims.put(CLAIM_TIPO, tipo);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(usuario.getUsuarioLogin())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isAccess(Claims claims) {
        return TIPO_ACCESS.equals(claims.get(CLAIM_TIPO, String.class));
    }

    public boolean isRefresh(Claims claims) {
        return TIPO_REFRESH.equals(claims.get(CLAIM_TIPO, String.class));
    }

    public long accessSeconds() {
        return accessTtl.toSeconds();
    }
}
