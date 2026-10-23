package pe.edu.kidcheck.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Modo demo: no llama a Twilio, pero deja la notificación como enviada para
 * poder mostrar el flujo completo sin credenciales ni teléfonos reales.
 */
@Component
@ConditionalOnProperty(name = "kidcheck.notificaciones.twilio.habilitado",
        havingValue = "false", matchIfMissing = true)
public class WhatsAppSimulado implements EnvioWhatsApp {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSimulado.class);

    @Override
    public NotificacionService.ResultadoEnvio enviar(String destino, String cuerpo) {
        log.info("WhatsApp simulado: mensaje encolado (sin datos personales en el log)");
        return new NotificacionService.ResultadoEnvio("simulado", "sim-" + UUID.randomUUID());
    }
}
