package pe.edu.kidcheck.configuracion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {

    Optional<Configuracion> findByColegioIdAndClave(Long idColegio, String clave);

    List<Configuracion> findByColegioIdOrderByClave(Long idColegio);
}
