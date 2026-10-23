package pe.edu.kidcheck.justificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JustificacionRepository extends JpaRepository<Justificacion, Long> {

    Optional<Justificacion> findByAlertaId(Long idAlerta);

    List<Justificacion> findByEstadoOrderByFechaEnvioDesc(String estado);

    List<Justificacion> findAllByOrderByFechaEnvioDesc();
}
