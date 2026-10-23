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
import pe.edu.kidcheck.asistencia.AsistenciaDiaRepository;
import pe.edu.kidcheck.colegio.Colegio;
import pe.edu.kidcheck.colegio.ColegioRepository;
import pe.edu.kidcheck.configuracion.ConfiguracionRepository;
import pe.edu.kidcheck.estudiante.Estudiante;
import pe.edu.kidcheck.estudiante.EstudianteRepository;
import pe.edu.kidcheck.notificacion.NotificacionService;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.LocalDate;

/**
 * RN-03: los viernes por la tarde se detectan alumnos con tres o más
 * ausencias sin justificación aprobada en los últimos 30 días y se genera
 * una alerta de reincidencia para psicología. La derivación final siempre
 * es manual.
 */
@Component
@RequiredArgsConstructor
public class ReincidenciaJob {

    private static final Logger log = LoggerFactory.getLogger(ReincidenciaJob.class);

    private final ColegioRepository colegios;
    private final EstudianteRepository estudiantes;
    private final AsistenciaDiaRepository asistencias;
    private final AlertaRepository alertas;
    private final ConfiguracionRepository configuraciones;
    private final NotificacionService notificaciones;

    @Value("${kidcheck.jobs.habilitado:true}")
    private boolean habilitado;

    @Scheduled(cron = "${kidcheck.jobs.reincidencia-cron:0 0 16 * * FRI}", zone = "America/Lima")
    @Transactional
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        LocalDate hoy = LocalDate.now(Tiempo.LIMA);
        LocalDate desde = hoy.minusDays(30);
        int total = 0;

        for (Colegio colegio : colegios.findAll()) {
            long umbral = umbral(colegio.getId());
            for (Object[] fila : asistencias.reincidentes(colegio.getId(), desde, hoy, umbral)) {
                Long idEstudiante = ((Number) fila[0]).longValue();
                int cantidad = ((Number) fila[1]).intValue();
                if (alertas.existsByEstudianteIdAndTipoAndFechaGeneracionAfter(
                        idEstudiante, Alerta.REINCIDENCIA, Tiempo.inicioDiaUtc(hoy.minusDays(30)))) {
                    continue;
                }
                Estudiante estudiante = estudiantes.findById(idEstudiante).orElse(null);
                if (estudiante == null) {
                    continue;
                }

                Alerta alerta = new Alerta();
                alerta.setEstudiante(estudiante);
                alerta.setTipo(Alerta.REINCIDENCIA);
                alerta.setUmbral((short) cantidad);
                alerta.setEstado("generada");
                alerta.setFechaGeneracion(Tiempo.ahoraUtc());
                alertas.save(alerta);
                notificaciones.encolarReincidencia(alerta, cantidad);
                total++;
            }
        }

        if (total > 0) {
            log.info("RN-03: {} alertas de reincidencia generadas", total);
        }
    }

    private long umbral(Long colegioId) {
        return configuraciones.findByColegioIdAndClave(colegioId, "umbral_reincidencia")
                .map(c -> {
                    try {
                        return Long.parseLong(c.getValor());
                    } catch (NumberFormatException ex) {
                        return 3L;
                    }
                })
                .orElse(3L);
    }
}
