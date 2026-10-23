package pe.edu.kidcheck.apoderado;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstudianteApoderadoRepository extends JpaRepository<EstudianteApoderado, EstudianteApoderadoId> {

    @Query("""
            select ea from EstudianteApoderado ea
              join fetch ea.apoderado
            where ea.estudiante.id = :idEstudiante
            order by ea.esPrincipal desc, ea.apoderado.nombresApellidos
            """)
    List<EstudianteApoderado> listarPorEstudiante(@Param("idEstudiante") Long idEstudiante);

    @Modifying
    @Query("delete from EstudianteApoderado ea where ea.estudiante.id = :idEstudiante")
    void borrarPorEstudiante(@Param("idEstudiante") Long idEstudiante);

    /**
     * Apoderados principales de todos los alumnos de un colegio, para el panel.
     * Devuelve pares [idEstudiante, nombresApellidos].
     */
    @Query("""
            select ea.estudiante.id, ea.apoderado.nombresApellidos
              from EstudianteApoderado ea
             where ea.estudiante.colegio.id = :colegioId
               and ea.esPrincipal = 1
            """)
    List<Object[]> principalesPorColegio(@Param("colegioId") Long colegioId);

    @Query("""
            select ea from EstudianteApoderado ea
              join fetch ea.apoderado
             where ea.estudiante.id = :idEstudiante
               and ea.esPrincipal = 1
            """)
    Optional<EstudianteApoderado> principal(@Param("idEstudiante") Long idEstudiante);
}
