package pe.edu.kidcheck.notificacion;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificacionWorker {

    private final NotificacionService notificaciones;

    @Scheduled(fixedDelayString = "${kidcheck.notificaciones.intervalo-ms:30000}",
            initialDelayString = "${kidcheck.notificaciones.intervalo-ms:30000}")
    public void ejecutar() {
        notificaciones.procesarPendientes();
    }
}
