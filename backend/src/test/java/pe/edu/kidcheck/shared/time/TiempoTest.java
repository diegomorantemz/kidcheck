package pe.edu.kidcheck.shared.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TiempoTest {

    @Test
    void convierteUtcALaFechaYHoraDeLima() {
        LocalDateTime utc = LocalDateTime.of(2026, 10, 23, 12, 41, 5);

        assertThat(Tiempo.fechaLocal(utc)).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(Tiempo.horaLocal(utc)).isEqualTo(LocalTime.of(7, 41, 5));
    }

    @Test
    void cruzaLaMedianocheDeLimaCorrectamente() {
        LocalDateTime utc = LocalDateTime.of(2026, 10, 23, 4, 30);

        assertThat(Tiempo.fechaLocal(utc)).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(Tiempo.horaLocal(utc)).isEqualTo(LocalTime.of(23, 30));
    }

    @Test
    void calculaLosLimitesDelDiaLocalEnUtc() {
        LocalDate fecha = LocalDate.of(2026, 10, 23);

        assertThat(Tiempo.inicioDiaUtc(fecha)).isEqualTo(LocalDateTime.of(2026, 10, 23, 5, 0));
        assertThat(Tiempo.finDiaUtc(fecha)).isEqualTo(LocalDateTime.of(2026, 10, 24, 5, 0));
    }

    @Test
    void formateaHoraParaLaApi() {
        assertThat(Tiempo.hhmm(LocalTime.of(7, 45))).isEqualTo("07:45");
        assertThat(Tiempo.hhmm(null)).isNull();
    }
}
