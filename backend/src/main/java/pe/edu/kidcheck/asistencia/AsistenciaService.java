package pe.edu.kidcheck.asistencia;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.alerta.Alerta;
import pe.edu.kidcheck.alerta.AlertaRepository;
import pe.edu.kidcheck.apoderado.EstudianteApoderado;
import pe.edu.kidcheck.apoderado.EstudianteApoderadoRepository;
import pe.edu.kidcheck.asistencia.dto.AsistenciaDto;
import pe.edu.kidcheck.asistencia.dto.JustificarRequest;
import pe.edu.kidcheck.auditoria.AuditoriaService;
import pe.edu.kidcheck.justificacion.Justificacion;
import pe.edu.kidcheck.justificacion.JustificacionRepository;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaDiaRepository asistencias;
    private final AlertaRepository alertas;
    private final JustificacionRepository justificaciones;
    private final EstudianteApoderadoRepository vinculos;
    private final AuditoriaService auditoria;
    private final CurrentUser actual;

    public List<AsistenciaDto> listar(LocalDate fecha, Long seccionId, String estado) {
        Long colegioId = actual.colegioId();
        List<AsistenciaDia> lista = asistencias.listar(colegioId, fecha, seccionId, estado);

        Map<Long, Alerta> alertaPorAsistencia = new HashMap<>();
        for (Alerta alerta : alertas.listarInasistenciasDelDia(colegioId, fecha)) {
            if (alerta.getAsistencia() != null) {
                alertaPorAsistencia.put(alerta.getAsistencia().getId(), alerta);
            }
        }

        return lista.stream().map(asistencia -> {
            Alerta alerta = alertaPorAsistencia.get(asistencia.getId());
            String justificacion = alerta == null ? null
                    : justificaciones.findByAlertaId(alerta.getId())
                    .map(Justificacion::getEstado).orElse(null);
            return new AsistenciaDto(
                    asistencia.getId(),
                    asistencia.getEstudiante().getId(),
                    asistencia.getEstudiante().nombreCompleto(),
                    asistencia.getSeccion().nombre(),
                    asistencia.getEstado().toUpperCase(),
                    asistencia.getIngreso() == null ? null
                            : Tiempo.hhmm(asistencia.getIngreso().getHoraLocal()),
                    asistencia.getMinutosTarde(),
                    asistencia.getOrigen(),
                    alerta == null ? null : alerta.getId(),
                    justificacion);
        }).toList();
    }

    @Transactional
    public AsistenciaDto justificar(Long idAsistencia, JustificarRequest request) {
        AsistenciaDia asistencia = asistencias.findById(idAsistencia)
                .filter(a -> a.getEstudiante().getColegio().getId().equals(actual.colegioId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ASISTENCIA_NO_ENCONTRADA",
                        "Asistencia no encontrada",
                        "No existe la asistencia con id " + idAsistencia + " en este colegio."));

        LocalDateTime ahora = Tiempo.ahoraUtc();
        asistencia.setEstado(AsistenciaDia.JUSTIFICADO);
        asistencias.save(asistencia);

        Optional<Alerta> alerta = alertas.listarInasistenciasDelDia(
                        actual.colegioId(), asistencia.getFecha()).stream()
                .filter(a -> a.getAsistencia() != null && a.getAsistencia().getId().equals(asistencia.getId()))
                .findFirst();

        alerta.ifPresent(a -> {
            a.setEstado("justificada");
            a.setUsuarioAtencionId(actual.usuarioId());
            a.setFechaAtencion(ahora);

            EstudianteApoderado principal = vinculos.principal(asistencia.getEstudiante().getId())
                    .orElse(null);
            if (principal != null) {
                Justificacion justificacion = justificaciones.findByAlertaId(a.getId())
                        .orElseGet(Justificacion::new);
                justificacion.setAlerta(a);
                justificacion.setApoderado(principal.getApoderado());
                justificacion.setMotivo(request.motivo());
                justificacion.setDetalle(request.detalle());
                justificacion.setEstado(Justificacion.APROBADA);
                justificacion.setUsuarioRevisionId(actual.usuarioId());
                justificacion.setFechaEnvio(justificacion.getFechaEnvio() == null
                        ? ahora : justificacion.getFechaEnvio());
                justificacion.setFechaRevision(ahora);
                justificaciones.save(justificacion);
            }
        });

        auditoria.registrar("asistencia", asistencia.getId(), "justificar_manual",
                Map.of("idEstudiante", asistencia.getEstudiante().getId(),
                        "motivo", request.motivo(),
                        "estado", "justificado"));

        return new AsistenciaDto(asistencia.getId(), asistencia.getEstudiante().getId(),
                asistencia.getEstudiante().nombreCompleto(), asistencia.getSeccion().nombre(),
                asistencia.getEstado().toUpperCase(),
                asistencia.getIngreso() == null ? null
                        : Tiempo.hhmm(asistencia.getIngreso().getHoraLocal()),
                asistencia.getMinutosTarde(), asistencia.getOrigen(),
                alerta.map(Alerta::getId).orElse(null), Justificacion.APROBADA);
    }
}
