package pe.edu.kidcheck.alerta;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.alerta.dto.AlertaDto;
import pe.edu.kidcheck.alerta.dto.DerivarRequest;
import pe.edu.kidcheck.asistencia.AsistenciaDia;
import pe.edu.kidcheck.auditoria.AuditoriaService;
import pe.edu.kidcheck.justificacion.Justificacion;
import pe.edu.kidcheck.justificacion.JustificacionRepository;
import pe.edu.kidcheck.notificacion.NotificacionService;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertas;
    private final JustificacionRepository justificaciones;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final CurrentUser actual;

    public List<AlertaDto> listar(String estado, LocalDate fecha) {
        Long colegioId = actual.colegioId();
        LocalDateTime desde = fecha == null ? null : Tiempo.inicioDiaUtc(fecha);
        LocalDateTime hasta = fecha == null ? null : Tiempo.finDiaUtc(fecha);

        return alertas.listar(colegioId, estado, desde, hasta).stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public AlertaDto atender(Long id) {
        Alerta alerta = buscar(id);
        alerta.setEstado("atendida");
        alerta.setUsuarioAtencionId(actual.usuarioId());
        alerta.setFechaAtencion(Tiempo.ahoraUtc());
        auditoria.registrar("alerta", alerta.getId(), "atender",
                Map.of("idEstudiante", alerta.getEstudiante().getId(),
                        "estado", "atendida"));
        return aDto(alerta);
    }

    @Transactional
    public AlertaDto derivar(Long id, DerivarRequest request) {
        Alerta alerta = buscar(id);
        alerta.setEstado("atendida");
        alerta.setUsuarioAtencionId(actual.usuarioId());
        alerta.setFechaAtencion(Tiempo.ahoraUtc());

        String comentario = request == null || request.comentario() == null
                || request.comentario().isBlank() ? "Sin comentario" : request.comentario().trim();
        notificaciones.encolarDerivacion(alerta, comentario);
        auditoria.registrar("alerta", alerta.getId(), "derivar",
                Map.of("idEstudiante", alerta.getEstudiante().getId(),
                        "estado", "atendida"));
        return aDto(alerta);
    }

    private Alerta buscar(Long id) {
        return alertas.findByIdAndEstudianteColegioId(id, actual.colegioId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ALERTA_NO_ENCONTRADA",
                        "Alerta no encontrada",
                        "No existe la alerta con id " + id + " en este colegio."));
    }

    private AlertaDto aDto(Alerta alerta) {
        AsistenciaDia asistencia = alerta.getAsistencia();
        var justificacion = asistencia == null ? java.util.Optional.<Justificacion>empty()
                : justificaciones.findByAlertaId(alerta.getId());

        AlertaDto.JustificacionResumen resumen = justificacion
                .map(j -> new AlertaDto.JustificacionResumen(
                        j.getId(), j.getMotivo(), j.getDetalle(), j.getEstado(),
                        j.getFechaEnvio() == null ? null : Tiempo.aInstante(j.getFechaEnvio())))
                .orElse(null);

        return new AlertaDto(
                alerta.getId(),
                alerta.getTipo(),
                alerta.getEstado(),
                alerta.getUmbral(),
                Tiempo.aInstante(alerta.getFechaGeneracion()),
                alerta.getEstudiante().getId(),
                alerta.getEstudiante().nombreCompleto(),
                asistencia == null ? null : asistencia.getSeccion().nombre(),
                asistencia == null ? null : asistencia.getFecha().toString(),
                asistencia == null || asistencia.getIngreso() == null ? null
                        : Tiempo.hhmm(asistencia.getIngreso().getHoraLocal()),
                resumen);
    }
}
