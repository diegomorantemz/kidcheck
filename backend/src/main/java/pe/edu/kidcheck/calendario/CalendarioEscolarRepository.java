package pe.edu.kidcheck.calendario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarioEscolarRepository extends JpaRepository<CalendarioEscolar, Long> {

    Optional<CalendarioEscolar> findByColegioIdAndFecha(Long idColegio, LocalDate fecha);

    List<CalendarioEscolar> findByColegioIdAndFechaBetweenOrderByFecha(Long idColegio, LocalDate desde,
                                                                       LocalDate hasta);
}
