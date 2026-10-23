package pe.edu.kidcheck.estudiante;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Bloquea la fila del estudiante mientras se registra el ingreso. Así dos
     * escaneos simultáneos del mismo carnet no pueden crear dos asistencias.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estudiante e where e.id = :id and e.colegio.id = :colegioId")
    Optional<Estudiante> findByIdAndColegioIdForUpdate(@Param("id") Long id,
                                                       @Param("colegioId") Long colegioId);

    /**
     * Roster offline: una fila por matrícula activa con su credencial activa.
     * Devuelve id, código, nombres, apellidos, tokenHash, grado, letra y hora límite.
     */
    @Query("""
            select e.id, e.codigoMatricula, e.nombres, e.apellidos,
                   c.tokenHash, s.grado, s.letra, s.horaLimiteIngreso
              from Matricula m
              join m.estudiante e
              join m.seccion s
              join CredencialQr c on c.estudiante = e and c.estado = 'activa'
             where m.estado = 'activa'
               and e.estado = 'activo'
               and e.colegio.id = :colegioId
             order by s.grado, s.letra, e.apellidos, e.nombres
            """)
    List<Object[]> roster(@Param("colegioId") Long colegioId);
}
