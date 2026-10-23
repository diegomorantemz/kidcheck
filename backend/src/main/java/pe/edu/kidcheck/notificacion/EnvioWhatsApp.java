package pe.edu.kidcheck.notificacion;

public interface EnvioWhatsApp {

    NotificacionService.ResultadoEnvio enviar(String destino, String cuerpo);
}
