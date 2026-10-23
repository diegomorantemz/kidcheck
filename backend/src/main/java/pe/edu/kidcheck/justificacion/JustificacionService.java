package pe.edu.kidcheck.justificacion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.alerta.Alerta;
import pe.edu.kidcheck.apoderado.Apoderado;
import pe.edu.kidcheck.apoderado.EstudianteApoderado;
import pe.edu.kidcheck.apoderado.EstudianteApoderadoRepository;
import pe.edu.kidcheck.asistencia.AsistenciaDia;
import pe.edu.kidcheck.asistencia.AsistenciaDiaRepository;
import pe.edu.kidcheck.auditoria.AuditoriaService;
import pe.edu.kidcheck.justificacion.dto.EnviarJustificacionRequest;
import pe.edu.kidcheck.justificacion.dto.JustificacionDto;
import pe.edu.kidcheck.justificacion.dto.JustificacionPublicaResponse;
import pe.edu.kidcheck.shared.crypto.Hashes;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JustificacionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> MOTIVOS = List.of(
            "enfermedad", "cita_medica", "viaje", "emergencia_familiar", "otro");

    private final JustificacionRepository justificaciones;
    private final TokenJustificacionRepository tokens;
    private final AsistenciaDiaRepository asistencias;
    private final EstudianteApoderadoRepository vinculos;
    private final AuditoriaService auditoria;
    private final CurrentUser actual;

    /** Crea un token de un solo uso, válido 72 h, y devuelve el valor en claro. */
    @Transactional
    public String crearToken(Alerta alerta) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String claro = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenJustificacion token = new TokenJustificacion();
        token.setAlerta(alerta);
        token.setTokenHash(Hashes.sha256Hex(claro));
        token.setExpiraEn(Tiempo.ahoraUtc().plusHours(72));
        tokens.save(token);
        return claro;
    }

    @Transactional(readOnly = true)
    public JustificacionPublicaResponse consultar(String tokenPlano) {
        TokenJustificacion token = tokenVigente(tokenPlano, false);
        Alerta alerta = token.getAlerta();
        AsistenciaDia asistencia = alerta.getAsistencia();
        boolean yaEnviada = justificaciones.findByAlertaId(alerta.getId()).isPresent();

        return new JustificacionPublicaResponse(
                alerta.getEstudiante().nombreCompleto(),
                asistencia == null ? null : asistencia.getSeccion().nombre(),
                asistencia == null ? null : asistencia.getFecha(),
                asistencia == null ? null : Tiempo.hhmm(asistencia.getSeccion().getHoraLimiteIngreso()),
                Tiempo.aInstante(token.getExpiraEn()),
                MOTIVOS,
                yaEnviada);
    }

    @Transactional
    public String enviar(EnviarJustificacionRequest request) {
        TokenJustificacion token = tokenVigente(request.token(), true);
        Alerta alerta = token.getAlerta();

        if (justificaciones.findByAlertaId(alerta.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "JUSTIFICACION_YA_ENVIADA",
                    "Justificación ya enviada",
                    "El apoderado ya registró una justificación para esta inasistencia.");
        }

        EstudianteApoderado principal = vinculos.principal(alerta.getEstudiante().getId())
                .orElseGet(() -> vinculos.listarPorEstudiante(alerta.getEstudiante().getId())
                        .stream().findFirst().orElse(null));
        if (principal == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SIN_APODERADO",
                    "Sin apoderado",
                    "El estudiante no tiene un apoderado registrado.");
        }

        LocalDateTime ahora = Tiempo.ahoraUtc();
        Justificacion justificacion = new Justificacion();
        justificacion.setAlerta(alerta);
        justificacion.setApoderado(principal.getApoderado());
        justificacion.setMotivo(request.motivo());
        justificacion.setDetalle(request.detalle());
        justificacion.setEstado(Justificacion.PENDIENTE);
        justificacion.setFechaEnvio(ahora);
        justificaciones.save(justificacion);

        token.setUsadoEn(ahora);
        tokens.save(token);

        return "Justificación enviada. El colegio la revisará.";
    }

    @Transactional(readOnly = true)
    public List<JustificacionDto> listar(String estado) {
        Long colegioId = actual.colegioId();
        List<Justificacion> lista = estado == null || estado.isBlank()
                ? justificaciones.findAllByOrderByFechaEnvioDesc()
                : justificaciones.findByEstadoOrderByFechaEnvioDesc(estado);

        return lista.stream()
                .filter(j -> j.getAlerta().getEstudiante().getColegio().getId().equals(colegioId))
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public JustificacionDto revisar(Long id, boolean aprobar) {
        Justificacion justificacion = justificaciones.findById(id)
                .filter(j -> j.getAlerta().getEstudiante().getColegio().getId().equals(actual.colegioId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JUSTIFICACION_NO_ENCONTRADA",
                        "Justificación no encontrada",
                        "No existe la justificación con id " + id + " en este colegio."));

        if (!Justificacion.PENDIENTE.equals(justificacion.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "JUSTIFICACION_YA_REVISADA",
                    "Justificación ya revisada",
                    "Esta justificación ya fue aprobada o rechazada.");
        }

        LocalDateTime ahora = Tiempo.ahoraUtc();
        justificacion.setEstado(aprobar ? Justificacion.APROBADA : Justificacion.RECHAZADA);
        justificacion.setUsuarioRevisionId(actual.usuarioId());
        justificacion.setFechaRevision(ahora);
        justificaciones.save(justificacion);

        if (aprobar) {
            Alerta alerta = justificacion.getAlerta();
            alerta.setEstado("justificada");
            alerta.setUsuarioAtencionId(actual.usuarioId());
            alerta.setFechaAtencion(ahora);
            if (alerta.getAsistencia() != null) {
                AsistenciaDia asistencia = alerta.getAsistencia();
                asistencia.setEstado(AsistenciaDia.JUSTIFICADO);
                asistencias.save(asistencia);
            }
        }

        auditoria.registrar("justificacion", justificacion.getId(),
                aprobar ? "aprobar" : "rechazar",
                Map.of("idAlerta", justificacion.getAlerta().getId(),
                        "motivo", justificacion.getMotivo(),
                        "estado", justificacion.getEstado()));

        return aDto(justificacion);
    }

    private TokenJustificacion tokenVigente(String tokenPlano, boolean bloquear) {
        if (tokenPlano == null || tokenPlano.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOKEN_REQUERIDO",
                    "Token requerido", "Falta el token de justificación.");
        }
        String hash = Hashes.sha256Hex(tokenPlano.trim());
        TokenJustificacion token = (bloquear
                ? tokens.findByTokenHashForUpdate(hash)
                : tokens.findByTokenHash(hash))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOKEN_INVALIDO",
                        "Enlace inválido", "El enlace de justificación no existe."));

        if (token.getUsadoEn() != null) {
            throw new ApiException(HttpStatus.GONE, "TOKEN_USADO",
                    "Enlace ya utilizado", "Este enlace de justificación ya fue usado.");
        }
        if (!token.getExpiraEn().isAfter(Tiempo.ahoraUtc())) {
            throw new ApiException(HttpStatus.GONE, "TOKEN_EXPIRADO",
                    "Enlace expirado", "El enlace de justificación venció. Contacta al colegio.");
        }
        return token;
    }

    private JustificacionDto aDto(Justificacion j) {
        Alerta alerta = j.getAlerta();
        AsistenciaDia asistencia = alerta.getAsistencia();
        Apoderado apoderado = j.getApoderado();
        return new JustificacionDto(
                j.getId(), alerta.getId(), alerta.getEstudiante().getId(),
                alerta.getEstudiante().nombreCompleto(),
                asistencia == null ? null : asistencia.getSeccion().nombre(),
                asistencia == null ? null : asistencia.getFecha(),
                j.getMotivo(), j.getDetalle(), j.getEstado(),
                apoderado == null ? null : apoderado.getNombresApellidos(),
                Tiempo.aInstante(j.getFechaEnvio()),
                j.getFechaRevision() == null ? null : Tiempo.aInstante(j.getFechaRevision()));
    }
}
