package pe.edu.kidcheck.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("""
            select a from Auditoria a
              join a.usuario u
            where u.colegio.id = :colegioId
              and (:entidad is null or a.entidad = :entidad)
              and (:desde is null or a.creadoEn >= :desde)
              and (:hasta is null or a.creadoEn < :hasta)
            order by a.creadoEn desc, a.id desc
            """)
    Page<Auditoria> buscar(@Param("colegioId") Long colegioId,
                           @Param("entidad") String entidad,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta,
                           Pageable pageable);
}
