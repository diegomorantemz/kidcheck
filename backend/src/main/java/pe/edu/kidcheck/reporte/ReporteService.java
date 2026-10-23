package pe.edu.kidcheck.reporte;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.edu.kidcheck.alerta.AlertaRepository;
import pe.edu.kidcheck.asistencia.AsistenciaDia;
import pe.edu.kidcheck.asistencia.AsistenciaDiaRepository;
import pe.edu.kidcheck.auditoria.AuditoriaService;
import pe.edu.kidcheck.estudiante.Matricula;
import pe.edu.kidcheck.estudiante.MatriculaRepository;
import pe.edu.kidcheck.reporte.dto.CsvExport;
import pe.edu.kidcheck.reporte.dto.ReporteDiarioResponse;
import pe.edu.kidcheck.reporte.dto.ReporteMensualResponse;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final String SQL_MENSUAL = """
            select v.id_estudiante, e.codigo_matricula, e.nombres, e.apellidos,
                   v.id_seccion, s.grado, s.letra,
                   v.dias_registrados, v.dias_presente, v.dias_tarde,
                   v.dias_ausente, v.dias_justificado, v.porcentaje_asistencia
              from v_asistencia_mensual v
              join estudiante e on e.id_estudiante = v.id_estudiante
              join seccion s on s.id_seccion = v.id_seccion
             where e.id_colegio = ?
               and v.periodo = ?
            """;

    private final JdbcTemplate jdbc;
    private final MatriculaRepository matriculas;
    private final AsistenciaDiaRepository asistencias;
    private final AlertaRepository alertas;
    private final AuditoriaService auditoria;
    private final CurrentUser actual;

    public ReporteDiarioResponse diario(LocalDate fecha, Long seccionId) {
        Long colegioId = actual.colegioId();
        List<Matricula> activas = matriculas.listarActivas(colegioId, seccionId);

        Map<Long, AsistenciaDia> porEstudiante = new HashMap<>();
        for (AsistenciaDia asistencia : asistencias.listar(colegioId, fecha, seccionId, null)) {
            porEstudiante.put(asistencia.getEstudiante().getId(), asistencia);
        }

        Contador total = new Contador();
        Map<Long, SeccionContador> porSeccion = new LinkedHashMap<>();
        Map<Long, Boolean> vistos = new HashMap<>();

        for (Matricula matricula : activas) {
            Long idEstudiante = matricula.getEstudiante().getId();
            if (vistos.put(idEstudiante, Boolean.TRUE) != null) {
                continue;
            }
            String estado = estado(porEstudiante.get(idEstudiante));
            total.sumar(estado);

            SeccionContador contador = porSeccion.computeIfAbsent(
                    matricula.getSeccion().getId(),
                    id -> new SeccionContador(id, matricula.getSeccion().nombre()));
            contador.total.sumar(estado);
        }

        int totalAlertas = alertas.listar(colegioId, null,
                Tiempo.inicioDiaUtc(fecha), Tiempo.finDiaUtc(fecha)).size();

        ReporteDiarioResponse.Resumen resumen = new ReporteDiarioResponse.Resumen(
                total.total, total.presentes, total.tarde, total.ausentes,
                total.justificados, total.pendientes, totalAlertas, total.porcentaje());

        List<ReporteDiarioResponse.SeccionResumen> secciones = porSeccion.values().stream()
                .map(c -> new ReporteDiarioResponse.SeccionResumen(
                        c.id, c.nombre, c.total.total, c.total.presentes, c.total.tarde,
                        c.total.ausentes, c.total.justificados, c.total.pendientes,
                        c.total.porcentaje()))
                .toList();

        auditoria.registrar("reporte", null, "consultar_diario",
                Map.of("fecha", fecha.toString(),
                        "seccion", seccionId == null ? "todas" : seccionId.toString()));

        return new ReporteDiarioResponse(fecha, Instant.now(), resumen, secciones);
    }

    public ReporteMensualResponse mensual(String periodoTexto, Long seccionId) {
        YearMonth periodo = periodo(periodoTexto);
        List<FilaMensual> filas = consultarMes(periodo, seccionId);
        Map<Long, int[]> alertasPorEstudiante = alertasDelMes(periodo);

        ReporteMensualResponse respuesta = construir(periodo, filas, alertasPorEstudiante);
        auditoria.registrar("reporte", null, "consultar_mensual",
                Map.of("periodo", periodo.toString(),
                        "seccion", seccionId == null ? "todas" : seccionId.toString()));
        return respuesta;
    }

    public CsvExport csv(String periodoTexto, Long seccionId) {
        YearMonth periodo = periodo(periodoTexto);
        List<FilaMensual> filas = consultarMes(periodo, seccionId);
        Map<Long, int[]> alertasPorEstudiante = alertasDelMes(periodo);

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Codigo,Estudiante,Seccion,Dias registrados,Presente,Tarde,Ausente,")
                .append("Justificado,% asistencia,Inasistencias,Reincidencias\r\n");

        for (FilaMensual fila : filas) {
            int[] conteo = alertasPorEstudiante.getOrDefault(fila.idEstudiante(), new int[]{0, 0});
            csv.append(texto(fila.codigoMatricula())).append(',')
                    .append(texto(fila.nombreCompleto())).append(',')
                    .append(texto(fila.seccion())).append(',')
                    .append(fila.diasRegistrados()).append(',')
                    .append(fila.diasPresente()).append(',')
                    .append(fila.diasTarde()).append(',')
                    .append(fila.diasAusente()).append(',')
                    .append(fila.diasJustificado()).append(',')
                    .append(redondear(fila.porcentajeAsistencia())).append(',')
                    .append(conteo[0]).append(',')
                    .append(conteo[1]).append("\r\n");
        }

        auditoria.registrar("reporte", null, "exportar_csv",
                Map.of("periodo", periodo.toString(),
                        "seccion", seccionId == null ? "todas" : seccionId.toString(),
                        "filas", filas.size()));

        return new CsvExport("asistencia-" + periodo + ".csv",
                csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private ReporteMensualResponse construir(YearMonth periodo, List<FilaMensual> filas,
                                             Map<Long, int[]> alertasPorEstudiante) {
        int diasRegistrados = 0;
        int diasPresente = 0;
        int diasTarde = 0;
        int diasAusente = 0;
        int diasJustificado = 0;
        int inasistencias = 0;
        int reincidencias = 0;

        Map<Long, SeccionMensual> porSeccion = new LinkedHashMap<>();
        List<ReporteMensualResponse.Fila> detalle = new ArrayList<>();

        for (FilaMensual fila : filas) {
            int[] conteo = alertasPorEstudiante.getOrDefault(fila.idEstudiante(), new int[]{0, 0});
            diasRegistrados += fila.diasRegistrados();
            diasPresente += fila.diasPresente();
            diasTarde += fila.diasTarde();
            diasAusente += fila.diasAusente();
            diasJustificado += fila.diasJustificado();
            inasistencias += conteo[0];
            reincidencias += conteo[1];

            detalle.add(new ReporteMensualResponse.Fila(
                    fila.idEstudiante(), fila.codigoMatricula(), fila.nombreCompleto(),
                    fila.idSeccion(), fila.seccion(),
                    fila.diasRegistrados(), fila.diasPresente(), fila.diasTarde(),
                    fila.diasAusente(), fila.diasJustificado(),
                    redondear(fila.porcentajeAsistencia()), conteo[0], conteo[1]));

            SeccionMensual seccion = porSeccion.computeIfAbsent(fila.idSeccion(),
                    id -> new SeccionMensual(id, fila.seccion()));
            seccion.estudiantes++;
            seccion.diasRegistrados += fila.diasRegistrados();
            seccion.diasPresente += fila.diasPresente();
            seccion.diasAusente += fila.diasAusente();
        }

        double porcentaje = diasRegistrados == 0 ? 0
                : redondear(100.0 * diasPresente / diasRegistrados);

        ReporteMensualResponse.Resumen resumen = new ReporteMensualResponse.Resumen(
                filas.size(), diasRegistrados, diasPresente, diasTarde, diasAusente,
                diasJustificado, inasistencias, reincidencias, porcentaje);

        List<ReporteMensualResponse.SeccionResumen> secciones = porSeccion.values().stream()
                .map(s -> new ReporteMensualResponse.SeccionResumen(
                        s.id, s.nombre, s.estudiantes, s.diasRegistrados,
                        s.diasPresente, s.diasAusente,
                        s.diasRegistrados == 0 ? 0
                                : redondear(100.0 * s.diasPresente / s.diasRegistrados)))
                .toList();

        return new ReporteMensualResponse(
                periodo.toString(), Instant.now(), resumen, secciones, detalle);
    }

    private List<FilaMensual> consultarMes(YearMonth periodo, Long seccionId) {
        String sql = SQL_MENSUAL + (seccionId == null ? "" : " and v.id_seccion = ?")
                + " order by s.grado, s.letra, e.apellidos, e.nombres";

        Object[] argumentos = seccionId == null
                ? new Object[]{actual.colegioId(), periodo.toString()}
                : new Object[]{actual.colegioId(), periodo.toString(), seccionId};

        return jdbc.query(sql, (rs, fila) -> new FilaMensual(
                rs.getLong("id_estudiante"),
                rs.getString("codigo_matricula"),
                rs.getString("nombres"),
                rs.getString("apellidos"),
                rs.getLong("id_seccion"),
                rs.getString("grado"),
                rs.getString("letra"),
                rs.getInt("dias_registrados"),
                rs.getInt("dias_presente"),
                rs.getInt("dias_tarde"),
                rs.getInt("dias_ausente"),
                rs.getInt("dias_justificado"),
                rs.getDouble("porcentaje_asistencia")), argumentos);
    }

    private Map<Long, int[]> alertasDelMes(YearMonth periodo) {
        LocalDateTime desde = periodo.atDay(1).atStartOfDay(Tiempo.LIMA)
                .withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime hasta = periodo.plusMonths(1).atDay(1).atStartOfDay(Tiempo.LIMA)
                .withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();

        Map<Long, int[]> resultado = new HashMap<>();
        jdbc.query("""
                        select a.id_estudiante,
                               sum(a.tipo = 'inasistencia') as inasistencias,
                               sum(a.tipo = 'reincidencia') as reincidencias
                          from alerta a
                          join estudiante e on e.id_estudiante = a.id_estudiante
                         where e.id_colegio = ?
                           and a.fecha_generacion >= ?
                           and a.fecha_generacion < ?
                         group by a.id_estudiante
                        """,
                rs -> {
                    resultado.put(rs.getLong("id_estudiante"),
                            new int[]{rs.getInt("inasistencias"), rs.getInt("reincidencias")});
                },
                actual.colegioId(), desde, hasta);
        return resultado;
    }

    private static YearMonth periodo(String texto) {
        if (texto == null || texto.isBlank()) {
            return YearMonth.now(Tiempo.LIMA);
        }
        try {
            return YearMonth.parse(texto.trim());
        } catch (DateTimeParseException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PERIODO_INVALIDO",
                    "Periodo inválido", "Usa el formato AAAA-MM, por ejemplo 2026-10.");
        }
    }

    private static String estado(AsistenciaDia asistencia) {
        return asistencia == null ? "PENDIENTE" : asistencia.getEstado().toUpperCase();
    }

    private static String texto(String valor) {
        return "\"" + (valor == null ? "" : valor.replace("\"", "\"\"")) + "\"";
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static final class Contador {
        private int total;
        private int presentes;
        private int tarde;
        private int ausentes;
        private int justificados;
        private int pendientes;

        private void sumar(String estado) {
            total++;
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
        }

        private double porcentaje() {
            return total == 0 ? 0 : redondear(100.0 * presentes / total);
        }
    }

    private static final class SeccionContador {
        private final Long id;
        private final String nombre;
        private final Contador total = new Contador();

        private SeccionContador(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    private static final class SeccionMensual {
        private final Long id;
        private final String nombre;
        private int estudiantes;
        private int diasRegistrados;
        private int diasPresente;
        private int diasAusente;

        private SeccionMensual(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }
}
