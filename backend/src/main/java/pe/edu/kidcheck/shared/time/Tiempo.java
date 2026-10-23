package pe.edu.kidcheck.shared.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Toda la base de datos guarda instantes en UTC. La fecha y hora "de negocio"
 * (a qué día pertenece un ingreso, si ya pasó la hora límite) se derivan
 * siempre con la zona del colegio, nunca con la zona del servidor.
 */
public final class Tiempo {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private Tiempo() {
    }

    public static LocalDateTime ahoraUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static LocalDateTime aUtc(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    public static Instant aInstante(LocalDateTime utc) {
        return utc.toInstant(ZoneOffset.UTC);
    }

    public static LocalDate fechaLocal(LocalDateTime utc) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(LIMA).toLocalDate();
    }

    public static LocalTime horaLocal(LocalDateTime utc) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(LIMA).toLocalTime();
    }

    /** Inicio del día local expresado en UTC, para filtrar columnas DATETIME. */
    public static LocalDateTime inicioDiaUtc(LocalDate fecha) {
        return fecha.atStartOfDay(LIMA).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Fin exclusivo del día local expresado en UTC. */
    public static LocalDateTime finDiaUtc(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay(LIMA).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static String hhmm(LocalTime hora) {
        return hora == null ? null : hora.format(HH_MM);
    }
}
