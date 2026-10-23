package pe.edu.kidcheck.estudiante;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    Optional<Matricula> findFirstByEstudianteIdAndEstadoOrderByFechaMatriculaDesc(Long idEstudiante, String estado);

    List<Matricula> findByEstudianteIdAndEstado(Long idEstudiante, String estado);

    @Query("""
            select m from Matricula m
              join fetch m.estudiante e
              join fetch m.seccion s
            where m.estado = 'activa'
              and e.estado = 'activo'
              and e.colegio.id = :colegioId
              and (:seccionId is null or s.id = :seccionId)
            order by s.grado, s.letra, e.apellidos, e.nombres
            """)
    List<Matricula> listarActivas(@Param("colegioId") Long colegioId,
                                  @Param("seccionId") Long seccionId);

    @Query("""
            select m from Matricula m
              join fetch m.estudiante e
              join fetch m.seccion s
              join fetch e.colegio
            where m.estado = 'activa'
              and e.estado = 'activo'
            order by e.colegio.id, s.grado, s.letra, e.apellidos, e.nombres
            """)
    List<Matricula> listarTodasActivas();
}
