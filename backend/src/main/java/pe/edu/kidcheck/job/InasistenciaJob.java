package pe.edu.kidcheck.job;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.alerta.Alerta;
import pe.edu.kidcheck.alerta.AlertaRepository;
import pe.edu.kidcheck.asistencia.AsistenciaDia;
import pe.edu.kidcheck.asistencia.AsistenciaDiaRepository;
import pe.edu.kidcheck.calendario.CalendarioEscolar;
import pe.edu.kidcheck.calendario.CalendarioEscolarRepository;
import pe.edu.kidcheck.estudiante.Matricula;
import pe.edu.kidcheck.estudiante.MatriculaRepository;
import pe.edu.kidcheck.justificacion.JustificacionService;
import pe.edu.kidcheck.notificacion.NotificacionService;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RN-01: pasada la hora límite más la tolerancia, todo alumno con matrícula
 * activa y sin asistencia del día queda como ausente. Crear la alerta, el
 * token de justificación y la notificación es idempotente: el UNIQUE de
 * asistencia_dia y la comprobación previa evitan duplicados.
 */
@Component
@RequiredArgsConstructor
public class InasistenciaJob {

    private static final Logger log = LoggerFactory.getLogger(InasistenciaJob.class);

    private final MatriculaRepository matriculas;
    private final AsistenciaDiaRepository asistencias;
    private final AlertaRepository alertas;
    private final CalendarioEscolarRepository calendario;
    private final JustificacionService justificaciones;
    private final NotificacionService notificaciones;

    @Value("${kidcheck.jobs.habilitado:true}")
    private boolean habilitado;

    @Scheduled(cron = "${kidcheck.jobs.inasistencia-cron:0 */5 * * * *}", zone = "America/Lima")
    @Transactional
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        LocalDate hoy = LocalDate.now(Tiempo.LIMA);
        LocalDateTime ahoraLocal = LocalDateTime.now(Tiempo.LIMA);
        Map<Long, Boolean> laborables = new HashMap<>();

        int creadas = 0;
        List<Matricula> activas = matriculas.listarTodasActivas();
        for (Matricula matricula : activas) {
            Long colegioId = matricula.getEstudiante().getColegio().getId();
            boolean laborable = laborables.computeIfAbsent(colegioId,
                    id -> calendario.findByColegioIdAndFecha(id, hoy)
                            .map(CalendarioEscolar::laborable)
                            .orElse(true));
            if (!laborable) {
                continue;
            }

            LocalDateTime limite = hoy.atTime(matricula.getSeccion().getHoraLimiteIngreso())
                    .plusMinutes(matricula.getSeccion().getToleranciaMinutos());
            if (!ahoraLocal.isAfter(limite)) {
                continue;
            }

            Long idEstudiante = matricula.getEstudiante().getId();
            if (asistencias.existsByEstudianteIdAndFecha(idEstudiante, hoy)) {
                continue;
            }

            AsistenciaDia asistencia = new AsistenciaDia();
            asistencia.setEstudiante(matricula.getEstudiante());
            asistencia.setSeccion(matricula.getSeccion());
            asistencia.setFecha(hoy);
            asistencia.setEstado(AsistenciaDia.AUSENTE);
            asistencia.setMinutosTarde((short) 0);
            asistencia.setOrigen("job");
            asistencias.save(asistencia);

            Alerta alerta = new Alerta();
            alerta.setAsistencia(asistencia);
            alerta.setEstudiante(matricula.getEstudiante());
            alerta.setTipo(Alerta.INASISTENCIA);
            alerta.setEstado("generada");
            alerta.setFechaGeneracion(Tiempo.ahoraUtc());
            alertas.save(alerta);

            String token = justificaciones.crearToken(alerta);
            notificaciones.encolarInasistencia(alerta, token);
            creadas++;
        }

        if (creadas > 0) {
            log.info("RN-01: {} inasistencias registradas para {}", creadas, hoy);
        }
    }
}
