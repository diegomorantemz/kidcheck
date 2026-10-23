package pe.edu.kidcheck.notificacion;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Envío real vía Twilio (WhatsApp Sandbox en la demo). Se usa la API REST
 * directamente para no agregar el SDK completo al proyecto.
 *
 * El número de destino debe haber enviado antes "join &lt;código&gt;" al
 * número del sandbox. En producción se migra a Content API con plantillas
 * aprobadas.
 */
@Component
@ConditionalOnProperty(name = "kidcheck.notificaciones.twilio.habilitado", havingValue = "true")
public class TwilioWhatsApp implements EnvioWhatsApp {

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String from;

    public TwilioWhatsApp(
            @Value("${kidcheck.notificaciones.twilio.account-sid}") String accountSid,
            @Value("${kidcheck.notificaciones.twilio.auth-token}") String authToken,
            @Value("${kidcheck.notificaciones.twilio.from}") String from) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.from = from;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public NotificacionService.ResultadoEnvio enviar(String destino, String cuerpo) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("From", from);
        formulario.add("To", aWhatsApp(destino));
        formulario.add("Body", cuerpo);

        JsonNode respuesta = restClient.post()
                .uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
                .headers(headers -> headers.setBasicAuth(accountSid, authToken))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(JsonNode.class);

        String sid = respuesta == null ? null : respuesta.path("sid").asText(null);
        return new NotificacionService.ResultadoEnvio("twilio", sid);
    }

    private static String aWhatsApp(String destino) {
        return destino.startsWith("whatsapp:") ? destino : "whatsapp:" + destino;
    }
}
