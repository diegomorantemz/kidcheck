package pe.edu.kidcheck.credencial;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.credencial.dto.CredencialEmitidaResponse;
import pe.edu.kidcheck.estudiante.Estudiante;
import pe.edu.kidcheck.estudiante.EstudianteRepository;
import pe.edu.kidcheck.shared.crypto.Hashes;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CredencialService {

    private static final int LADO_QR = 512;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EstudianteRepository estudiantes;
    private final CredencialQrRepository credenciales;
    private final QrService qrService;
    private final CurrentUser actual;

    /**
     * Emite una credencial nueva y revoca la anterior. El token en claro
     * se devuelve una sola vez; en la base solo queda su hash SHA-256.
     */
    @Transactional
    public CredencialEmitidaResponse emitir(Long idEstudiante) {
        Estudiante estudiante = estudiantes
                .findByIdAndColegioId(idEstudiante, actual.colegioId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO",
                        "Estudiante no encontrado",
                        "No existe un estudiante con id " + idEstudiante + " en este colegio."));

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        credenciales.revocarActivas(estudiante.getId(), ahora);

        String token = nuevoToken();

        CredencialQr credencial = new CredencialQr();
        credencial.setEstudiante(estudiante);
        credencial.setTokenHash(Hashes.sha256Hex(token));
        credencial.setEstado("activa");
        credencial.setEmitidaEn(ahora);
        credenciales.save(credencial);

        String pngBase64 = Base64.getEncoder().encodeToString(qrService.png(token, LADO_QR));

        return new CredencialEmitidaResponse(
                credencial.getId(),
                estudiante.getId(),
                token,
                pngBase64);
    }

    @Transactional
    public void revocar(Long idCredencial) {
        CredencialQr credencial = credenciales
                .findByIdAndEstudianteColegioId(idCredencial, actual.colegioId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CREDENCIAL_NO_ENCONTRADA",
                        "Credencial no encontrada",
                        "No existe una credencial con id " + idCredencial + " en este colegio."));

        if ("activa".equals(credencial.getEstado())) {
            credencial.setEstado("revocada");
            credencial.setRevocadaEn(LocalDateTime.now(ZoneOffset.UTC));
            credenciales.save(credencial);
        }
    }

    private static String nuevoToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
