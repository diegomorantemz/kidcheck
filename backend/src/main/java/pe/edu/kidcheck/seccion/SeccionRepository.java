package pe.edu.kidcheck.seccion;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    @EntityGraph(attributePaths = {"periodo"})
    List<Seccion> findByPeriodoEstadoOrderByGradoAscLetraAsc(String estadoPeriodo);
}
