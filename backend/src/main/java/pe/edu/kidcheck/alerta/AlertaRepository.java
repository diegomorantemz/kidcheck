package pe.edu.kidcheck.alerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    Optional<Alerta> findByIdAndEstudianteColegioId(Long id, Long colegioId);

    boolean existsByAsistenciaIdAndTipo(Long idAsistencia, String tipo);

    boolean existsByEstudianteIdAndTipoAndFechaGeneracionAfter(Long idEstudiante, String tipo,
                                                               LocalDateTime desde);

    @Query("""
            select a from Alerta a
              join fetch a.estudiante e
              join fetch e.colegio c
              left join fetch a.asistencia s
              left join fetch s.seccion
            where c.id = :colegioId
              and (:estado is null or a.estado = :estado)
              and (:desde is null or a.fechaGeneracion >= :desde)
              and (:hasta is null or a.fechaGeneracion < :hasta)
            order by a.fechaGeneracion desc
            """)
    List<Alerta> listar(@Param("colegioId") Long colegioId,
                        @Param("estado") String estado,
                        @Param("desde") LocalDateTime desde,
                        @Param("hasta") LocalDateTime hasta);

    @Query("""
            select a from Alerta a
              join fetch a.estudiante e
              left join fetch a.asistencia s
            where e.colegio.id = :colegioId
              and s.fecha = :fecha
              and a.tipo = 'inasistencia'
            """)
    List<Alerta> listarInasistenciasDelDia(@Param("colegioId") Long colegioId,
                                           @Param("fecha") LocalDate fecha);

    long countByEstudianteColegioIdAndEstadoIn(Long colegioId, List<String> estados);
}
