package pe.edu.kidcheck.estudiante;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    boolean existsByColegioIdAndCodigoMatricula(Long colegioId, String codigoMatricula);

    boolean existsByColegioIdAndCodigoMatriculaAndIdNot(Long colegioId, String codigoMatricula, Long id);

    Optional<Estudiante> findByIdAndColegioId(Long id, Long colegioId);

    @Query("""
            select distinct e from Estudiante e
              left join Matricula m on m.estudiante = e and m.estado = 'activa'
            where e.colegio.id = :colegioId
              and (:q is null
                   or lower(e.nombres) like :q
                   or lower(e.apellidos) like :q
                   or lower(e.codigoMatricula) like :q)
              and (:seccionId is null or m.seccion.id = :seccionId)
              and (:estado is null or e.estado = :estado)
            """)
    Page<Estudiante> buscar(@Param("colegioId") Long colegioId,
                            @Param("q") String q,
                            @Param("seccionId") Long seccionId,
                            @Param("estado") String estado,
                            Pageable pageable);
}
