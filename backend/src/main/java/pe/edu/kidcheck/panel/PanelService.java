package pe.edu.kidcheck.panel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.kidcheck.alerta.Alerta;
import pe.edu.kidcheck.alerta.AlertaRepository;
import pe.edu.kidcheck.apoderado.EstudianteApoderadoRepository;
import pe.edu.kidcheck.asistencia.AsistenciaDia;
import pe.edu.kidcheck.asistencia.AsistenciaDiaRepository;
import pe.edu.kidcheck.estudiante.Matricula;
import pe.edu.kidcheck.estudiante.MatriculaRepository;
import pe.edu.kidcheck.panel.dto.PanelDiaResponse;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PanelService {

    private final MatriculaRepository matriculas;
    private final AsistenciaDiaRepository asistencias;
    private final AlertaRepository alertas;
    private final EstudianteApoderadoRepository vinculos;
    private final CurrentUser actual;

    public PanelDiaResponse panel(LocalDate fecha, Long seccionId) {
        Long colegioId = actual.colegioId();
        List<Matricula> activas = matriculas.listarActivas(colegioId, seccionId);

        Map<Long, AsistenciaDia> asistenciaPorEstudiante = new HashMap<>();
        for (AsistenciaDia asistencia : asistencias.listarPorFecha(fecha)) {
            asistenciaPorEstudiante.put(asistencia.getEstudiante().getId(), asistencia);
        }

        Map<Long, Alerta> alertaPorAsistencia = new HashMap<>();
        for (Alerta alerta : alertas.listarInasistenciasDelDia(colegioId, fecha)) {
            if (alerta.getAsistencia() != null) {
                alertaPorAsistencia.put(alerta.getAsistencia().getId(), alerta);
            }
        }

        Map<Long, String> apoderados = new HashMap<>();
        for (Object[] fila : vinculos.principalesPorColegio(colegioId)) {
            apoderados.put(((Number) fila[0]).longValue(), (String) fila[1]);
        }

        int presentes = 0;
        int tarde = 0;
        int ausentes = 0;
        int justificados = 0;
        int pendientes = 0;
        int totalAlertas = 0;

        Map<Long, SeccionContador> porSeccion = new LinkedHashMap<>();
        Map<Long, PanelDiaResponse.Fila> filas = new LinkedHashMap<>();

        for (Matricula matricula : activas) {
            Long idEstudiante = matricula.getEstudiante().getId();
            if (filas.containsKey(idEstudiante)) {
                continue;
            }
            AsistenciaDia asistencia = asistenciaPorEstudiante.get(idEstudiante);
            Alerta alerta = asistencia == null ? null : alertaPorAsistencia.get(asistencia.getId());

            String estado = asistencia == null ? "PENDIENTE" : asistencia.getEstado().toUpperCase();
            String hora = asistencia != null && asistencia.getIngreso() != null
                    ? Tiempo.hhmm(asistencia.getIngreso().getHoraLocal())
                    : null;
            boolean esTarde = asistencia != null && AsistenciaDia.TARDE.equals(asistencia.getEstado());

            switch (estado) {
                case "PRESENTE" -> presentes++;
                case "TARDE" -> {
                    presentes++;
                    tarde++;
                }
                case "AUSENTE" -> ausentes++;
                case "JUSTIFICADO" -> justificados++;
                default -> pendientes++;
            }
            if (alerta != null) {
                totalAlertas++;
            }

            SeccionContador contador = porSeccion.computeIfAbsent(
                    matricula.getSeccion().getId(),
                    id -> new SeccionContador(id, matricula.getSeccion().nombre()));
            contador.total++;
            if ("PRESENTE".equals(estado) || "TARDE".equals(estado)) {
                contador.presentes++;
            }
            if ("TARDE".equals(estado)) {
                contador.tarde++;
            }
            if ("PENDIENTE".equals(estado)) {
                contador.pendientes++;
            }

            filas.put(idEstudiante, new PanelDiaResponse.Fila(
                    idEstudiante,
                    matricula.getEstudiante().nombreCompleto(),
                    matricula.getSeccion().nombre(),
                    estado,
                    hora,
                    esTarde,
                    apoderados.get(idEstudiante),
                    alerta == null ? null : new PanelDiaResponse.AlertaResumen(
                            alerta.getId(), alerta.getTipo(), alerta.getEstado())));
        }

        PanelDiaResponse.Resumen resumen = new PanelDiaResponse.Resumen(
                filas.size(), presentes, tarde, ausentes, justificados, pendientes, totalAlertas);

        List<PanelDiaResponse.SeccionResumen> secciones = porSeccion.values().stream()
                .map(c -> new PanelDiaResponse.SeccionResumen(
                        c.id, c.nombre, c.total, c.presentes, c.tarde, c.pendientes))
                .toList();

        return new PanelDiaResponse(fecha, Instant.now(), resumen, secciones,
                filas.values().stream().toList());
    }

    /** La sección existe aunque todavía no tenga alumnos? No hace falta: el panel solo muestra activas. */
    private static final class SeccionContador {
        private final Long id;
        private final String nombre;
        private int total;
        private int presentes;
        private int tarde;
        private int pendientes;

        private SeccionContador(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }
}
