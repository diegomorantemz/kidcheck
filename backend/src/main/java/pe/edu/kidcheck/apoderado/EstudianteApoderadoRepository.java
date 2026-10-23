package pe.edu.kidcheck.apoderado;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
