package pe.edu.kidcheck.notificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    boolean existsByIngresoIdAndPlantilla(Long idIngreso, String plantilla);

    boolean existsByAlertaIdAndPlantilla(Long idAlerta, String plantilla);

    long countByEstadoEntrega(String estadoEntrega);

    /**
     * Toma un lote de la bandeja de salida bloqueando las filas.
     * SKIP LOCKED permite varios workers sin enviar dos veces el mismo mensaje.
     */
    @Query(value = """
            select * from notificacion
             where estado_entrega = 'pendiente'
               and (proximo_intento is null or proximo_intento <= :ahora)
             order by id_notificacion
             limit :limite
             for update skip locked
            """, nativeQuery = true)
    List<Notificacion> tomarPendientes(@Param("ahora") LocalDateTime ahora,
                                       @Param("limite") int limite);

    List<Notificacion> findByEstudianteIdOrderByCreadoEnDesc(Long idEstudiante);
}
