package pe.edu.kidcheck.notificacion;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.alerta.Alerta;
import pe.edu.kidcheck.apoderado.Apoderado;
import pe.edu.kidcheck.apoderado.ConsentimientoDatosRepository;
import pe.edu.kidcheck.apoderado.EstudianteApoderado;
import pe.edu.kidcheck.apoderado.EstudianteApoderadoRepository;
import pe.edu.kidcheck.estudiante.Estudiante;
import pe.edu.kidcheck.ingreso.Ingreso;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bandeja de salida transaccional (outbox). Las notificaciones se crean en la
 * misma transacción que el ingreso o la alerta; un worker las envía después.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificaciones;
    private final EstudianteApoderadoRepository vinculos;
    private final ConsentimientoDatosRepository consentimientos;
    private final EnvioWhatsApp envioWhatsApp;

    @Value("${kidcheck.web.url:http://localhost:5173}")
    private String webUrl;

    @Value("${kidcheck.notificaciones.max-intentos:3}")
    private int maxIntentos;

    @Transactional
    public boolean encolarIngreso(Ingreso ingreso) {
        if (notificaciones.existsByIngresoIdAndPlantilla(ingreso.getId(), "ingreso_confirmado")) {
            return false;
        }
        Estudiante estudiante = ingreso.getEstudiante();
        Apoderado apoderado = apoderadoPrincipal(estudiante.getId());
        Destino destino = destino(estudiante, apoderado);
        if (destino == null) {
            return false;
        }

        String cuerpo = "KidCheck · " + estudiante.getColegio().getNombre() + ": "
                + estudiante.nombreCompleto() + " registró su ingreso a las "
                + Tiempo.hhmm(ingreso.getHoraLocal())
                + ". — No responder a este mensaje.";

        crear(estudiante, apoderado, ingreso, null, destino, "ingreso_confirmado", cuerpo);
        return true;
    }

    @Transactional
    public boolean encolarInasistencia(Alerta alerta, String tokenPlano) {
        if (notificaciones.existsByAlertaIdAndPlantilla(alerta.getId(), "alerta_inasistencia")) {
            return false;
        }
        Estudiante estudiante = alerta.getEstudiante();
        Apoderado apoderado = apoderadoPrincipal(estudiante.getId());
        Destino destino = destino(estudiante, apoderado);
        if (destino == null) {
            return false;
        }

        String hora = alerta.getAsistencia() != null && alerta.getAsistencia().getSeccion() != null
                ? Tiempo.hhmm(alerta.getAsistencia().getSeccion().getHoraLimiteIngreso())
                : "la hora límite";
        String enlace = webUrl + "/j/" + tokenPlano;
        String cuerpo = "KidCheck · " + estudiante.getColegio().getNombre()
                + ": no registramos el ingreso de " + estudiante.nombreCompleto()
                + " hoy (límite " + hora + "). Si es una ausencia justificada, regístrala aquí: "
                + enlace + " (vence en 72 h).";

        crear(estudiante, apoderado, null, alerta, destino, "alerta_inasistencia", cuerpo);
        return true;
    }

    @Transactional
    public boolean encolarReincidencia(Alerta alerta, int cantidad) {
        if (notificaciones.existsByAlertaIdAndPlantilla(alerta.getId(), "reincidencia_psicologia")) {
            return false;
        }
        Estudiante estudiante = alerta.getEstudiante();
        String cuerpo = "KidCheck: " + estudiante.nombreCompleto() + " acumula " + cantidad
                + " inasistencias sin justificar en 30 días. Caso #" + alerta.getId()
                + ". Revisar en el panel.";
        crear(estudiante, null, null, alerta,
                new Destino("app", "psicologia"), "reincidencia_psicologia", cuerpo);
        return true;
    }

    @Transactional
    public boolean encolarDerivacion(Alerta alerta, String comentario) {
        if (notificaciones.existsByAlertaIdAndPlantilla(alerta.getId(), "derivacion_psicologia")) {
            return false;
        }
        Estudiante estudiante = alerta.getEstudiante();
        String cuerpo = "KidCheck: caso #" + alerta.getId() + " de "
                + estudiante.nombreCompleto() + " derivado a psicología. " + comentario;
        crear(estudiante, null, null, alerta,
                new Destino("app", "psicologia"), "derivacion_psicologia", cuerpo);
        return true;
    }

    private void crear(Estudiante estudiante, Apoderado apoderado, Ingreso ingreso, Alerta alerta,
                       Destino destino, String plantilla, String cuerpo) {
        Notificacion notificacion = new Notificacion();
        notificacion.setEstudiante(estudiante);
        notificacion.setApoderado(apoderado);
        notificacion.setIngreso(ingreso);
        notificacion.setAlerta(alerta);
        notificacion.setCanal(destino.canal());
        notificacion.setDestino(destino.valor());
        notificacion.setCuerpo(cuerpo);
        notificacion.setPlantilla(plantilla);
        notificacion.setEstadoEntrega("pendiente");
        notificacion.setIntentos(0);
        notificaciones.save(notificacion);
    }

    /**
     * Solo se notifica a un apoderado con consentimiento vigente para
     * notificaciones. WhatsApp es el canal principal; si no hay teléfono se
     * usa el correo.
     */
    private Destino destino(Estudiante estudiante, Apoderado apoderado) {
        if (apoderado == null) {
            return null;
        }
        boolean autorizado = consentimientos.existsByEstudianteIdAndApoderadoIdAndFinalidadAndEstado(
                estudiante.getId(), apoderado.getId(), "notificaciones", "autorizado");
        if (!autorizado) {
            return null;
        }
        if (apoderado.getTelefonoWhatsapp() != null && !apoderado.getTelefonoWhatsapp().isBlank()) {
            return new Destino("whatsapp", apoderado.getTelefonoWhatsapp());
        }
        if (apoderado.getEmail() != null && !apoderado.getEmail().isBlank()) {
            return new Destino("email", apoderado.getEmail());
        }
        return null;
    }

    private Apoderado apoderadoPrincipal(Long idEstudiante) {
        EstudianteApoderado principal = vinculos.principal(idEstudiante)
                .orElseGet(() -> vinculos.listarPorEstudiante(idEstudiante).stream().findFirst().orElse(null));
        return principal == null ? null : principal.getApoderado();
    }

    private record Destino(String canal, String valor) {
    }

    // ---- Worker ---------------------------------------------------------

    @Transactional
    public void procesarPendientes() {
        List<Notificacion> lote = notificaciones.tomarPendientes(Tiempo.ahoraUtc(), 20);
        if (lote.isEmpty()) {
            return;
        }
        for (Notificacion notificacion : lote) {
            enviar(notificacion);
        }
    }

    private void enviar(Notificacion notificacion) {
        LocalDateTime ahora = Tiempo.ahoraUtc();
        notificacion.setIntentos(notificacion.getIntentos() + 1);
        try {
            ResultadoEnvio resultado = despachar(notificacion, ahora);
            notificacion.setEstadoEntrega("enviado");
            notificacion.setEnviadoEn(ahora);
            notificacion.setProveedor(resultado.proveedor());
            notificacion.setIdExterno(resultado.idExterno());
            notificacion.setErrorCodigo(null);
            notificacion.setErrorMensaje(null);
            notificacion.setProximoIntento(null);

            if (notificacion.getAlerta() != null
                    && "generada".equals(notificacion.getAlerta().getEstado())) {
                notificacion.getAlerta().setEstado("notificada");
            }
            log.info("Notificación {} entregada por {}", notificacion.getId(), resultado.proveedor());
        } catch (Exception ex) {
            notificacion.setErrorCodigo("ENVIO_FALLIDO");
            notificacion.setErrorMensaje(recortar(ex.getMessage()));
            if (notificacion.getIntentos() >= maxIntentos) {
                notificacion.setEstadoEntrega("fallido");
                notificacion.setProximoIntento(null);
                log.warn("Notificación {} agotó los intentos", notificacion.getId());
            } else {
                notificacion.setEstadoEntrega("pendiente");
                int minutos = notificacion.getIntentos() == 1 ? 1 : 5;
                notificacion.setProximoIntento(ahora.plusMinutes(minutos));
                log.warn("Notificación {} falló; reintento en {} min", notificacion.getId(), minutos);
            }
        }
    }

    private ResultadoEnvio despachar(Notificacion notificacion, LocalDateTime ahora) {
        String cuerpo = notificacion.getCuerpo() == null || notificacion.getCuerpo().isBlank()
                ? cuerpoDeRespaldo(notificacion)
                : notificacion.getCuerpo();
        return switch (notificacion.getCanal()) {
            case "whatsapp" -> envioWhatsApp.enviar(notificacion.getDestino(), cuerpo);
            case "app" -> new ResultadoEnvio("app", "panel-" + notificacion.getId());
            default -> new ResultadoEnvio("simulado-" + notificacion.getCanal(),
                    "local-" + notificacion.getId());
        };
    }

    /**
     * Las notificaciones sembradas antes de existir la columna cuerpo no
     * tienen texto. Se genera un mensaje mínimo para no enviar un vacío.
     */
    private String cuerpoDeRespaldo(Notificacion notificacion) {
        String nombre = notificacion.getEstudiante().nombreCompleto();
        return switch (notificacion.getPlantilla()) {
            case "ingreso_confirmado" -> "KidCheck: " + nombre + " registró su ingreso.";
            case "alerta_inasistencia" -> "KidCheck: no registramos el ingreso de " + nombre
                    + " hoy. Contacta al colegio para justificar la ausencia.";
            case "reincidencia_psicologia" -> "KidCheck: revisar reincidencia de inasistencias de "
                    + nombre + " en el panel.";
            default -> "KidCheck: notificación sobre " + nombre + ".";
        };
    }

    private static String recortar(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        return mensaje.length() > 255 ? mensaje.substring(0, 255) : mensaje;
    }

    public record ResultadoEnvio(String proveedor, String idExterno) {
    }
}
