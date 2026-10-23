package pe.edu.kidcheck.asistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaDiaRepository extends JpaRepository<AsistenciaDia, Long> {

    Optional<AsistenciaDia> findByEstudianteIdAndFecha(Long idEstudiante, LocalDate fecha);

    boolean existsByEstudianteIdAndFecha(Long idEstudiante, LocalDate fecha);

    @Query("""
            select a from AsistenciaDia a
              left join fetch a.ingreso
            where a.fecha = :fecha
            """)
    List<AsistenciaDia> listarPorFecha(@Param("fecha") LocalDate fecha);

    @Query("""
            select a from AsistenciaDia a
              join fetch a.estudiante e
              join fetch a.seccion s
              left join fetch a.ingreso i
            where e.colegio.id = :colegioId
              and a.fecha = :fecha
              and (:seccionId is null or s.id = :seccionId)
              and (:estado is null or a.estado = :estado)
            order by s.grado, s.letra, e.apellidos, e.nombres
            """)
    List<AsistenciaDia> listar(@Param("colegioId") Long colegioId,
                               @Param("fecha") LocalDate fecha,
                               @Param("seccionId") Long seccionId,
                               @Param("estado") String estado);

    long countByFechaAndEstado(LocalDate fecha, String estado);

    /**
     * Alumnos con ausencias sin justificación aprobada dentro de la ventana.
     * Devuelve pares [idEstudiante, cantidad] ordenados de mayor a menor.
     */
    @Query("""
            select a.estudiante.id, count(a)
              from AsistenciaDia a
             where a.estudiante.colegio.id = :colegioId
               and a.fecha between :desde and :hasta
               and a.estado = 'ausente'
               and not exists (
                    select j from Justificacion j
                     where j.alerta.asistencia = a and j.estado = 'aprobada')
             group by a.estudiante.id
            having count(a) >= :umbral
             order by count(a) desc
            """)
    List<Object[]> reincidentes(@Param("colegioId") Long colegioId,
                                @Param("desde") LocalDate desde,
                                @Param("hasta") LocalDate hasta,
                                @Param("umbral") long umbral);
}
