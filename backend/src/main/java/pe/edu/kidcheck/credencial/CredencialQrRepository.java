package pe.edu.kidcheck.credencial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CredencialQrRepository extends JpaRepository<CredencialQr, Long> {

    boolean existsByEstudianteIdAndEstado(Long idEstudiante, String estado);

    List<CredencialQr> findByEstudianteIdAndEstado(Long idEstudiante, String estado);

    Optional<CredencialQr> findFirstByEstudianteIdAndEstadoOrderByEmitidaEnDesc(Long idEstudiante, String estado);

    Optional<CredencialQr> findByIdAndEstudianteColegioId(Long id, Long colegioId);

    @Modifying
    @Query("""
            update CredencialQr c
               set c.estado = 'revocada', c.revocadaEn = :fecha
             where c.estudiante.id = :idEstudiante and c.estado = 'activa'
            """)
    int revocarActivas(@Param("idEstudiante") Long idEstudiante, @Param("fecha") LocalDateTime fecha);
}
