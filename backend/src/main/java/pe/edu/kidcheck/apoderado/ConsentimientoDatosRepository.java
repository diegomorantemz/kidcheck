package pe.edu.kidcheck.apoderado;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentimientoDatosRepository extends JpaRepository<ConsentimientoDatos, Long> {

    boolean existsByEstudianteIdAndApoderadoIdAndFinalidadAndEstado(
            Long idEstudiante, Long idApoderado, String finalidad, String estado);

    List<ConsentimientoDatos> findByEstudianteIdAndFinalidad(Long idEstudiante, String finalidad);
}
