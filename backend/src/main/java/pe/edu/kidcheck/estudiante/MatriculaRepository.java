package pe.edu.kidcheck.estudiante;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    Optional<Matricula> findFirstByEstudianteIdAndEstadoOrderByFechaMatriculaDesc(Long idEstudiante, String estado);

    List<Matricula> findByEstudianteIdAndEstado(Long idEstudiante, String estado);
}
