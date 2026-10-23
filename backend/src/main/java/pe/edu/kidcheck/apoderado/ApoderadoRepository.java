package pe.edu.kidcheck.apoderado;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    Optional<Apoderado> findByIdAndColegioId(Long id, Long colegioId);

    @Query("""
            select a from Apoderado a
            where a.colegio.id = :colegioId
              and (:q is null
                   or lower(a.nombresApellidos) like :q
                   or a.telefonoWhatsapp like :q)
            """)
    Page<Apoderado> buscar(@Param("colegioId") Long colegioId,
                           @Param("q") String q,
                           Pageable pageable);
}
