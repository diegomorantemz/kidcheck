package pe.edu.kidcheck.justificacion;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.justificacion.dto.EnviarJustificacionRequest;
import pe.edu.kidcheck.justificacion.dto.JustificacionDto;
import pe.edu.kidcheck.justificacion.dto.JustificacionPublicaResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/justificaciones")
@RequiredArgsConstructor
public class JustificacionController {

    private final JustificacionService justificaciones;

    /** Formulario público abierto desde el enlace enviado por WhatsApp. */
    @GetMapping("/mia")
    public JustificacionPublicaResponse mia(@RequestParam String token) {
        return justificaciones.consultar(token);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> enviar(@Valid @RequestBody EnviarJustificacionRequest request) {
        return Map.of("mensaje", justificaciones.enviar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public List<JustificacionDto> listar(@RequestParam(required = false) String estado) {
        return justificaciones.listar(estado);
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
    public JustificacionDto aprobar(@PathVariable Long id) {
        return justificaciones.revisar(id, true);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
    public JustificacionDto rechazar(@PathVariable Long id) {
        return justificaciones.revisar(id, false);
    }
}
