package pe.edu.kidcheck.ingreso;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    Optional<Ingreso> findByIdempotencyKey(String idempotencyKey);

    Optional<Ingreso> findFirstByEstudianteIdAndFechaLocalOrderByHoraLocalAsc(Long idEstudiante, LocalDate fecha);

    /**
     * Lectura de bloqueo (current read) para que dos escaneos simultáneos del
     * mismo alumno vean el ingreso recién confirmado por la otra transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from Ingreso i
             where i.estudiante.id = :idEstudiante
               and i.fechaLocal = :fecha
             order by i.horaLocal
            """)
    List<Ingreso> buscarDelDiaForUpdate(@Param("idEstudiante") Long idEstudiante,
                                        @Param("fecha") LocalDate fecha);

    List<Ingreso> findByFechaLocalOrderByHoraLocalAsc(LocalDate fecha);

    long countByFechaLocal(LocalDate fecha);
}
