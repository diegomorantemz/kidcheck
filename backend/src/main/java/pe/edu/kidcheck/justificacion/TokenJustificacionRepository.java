package pe.edu.kidcheck.justificacion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TokenJustificacionRepository extends JpaRepository<TokenJustificacion, Long> {

    Optional<TokenJustificacion> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenJustificacion t where t.tokenHash = :tokenHash")
    Optional<TokenJustificacion> findByTokenHashForUpdate(String tokenHash);

    Optional<TokenJustificacion> findFirstByAlertaIdAndUsadoEnIsNullOrderByExpiraEnDesc(Long idAlerta);
}
