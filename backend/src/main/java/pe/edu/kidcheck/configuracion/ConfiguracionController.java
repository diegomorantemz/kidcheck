package pe.edu.kidcheck.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.configuracion.dto.ConfiguracionDto;
import pe.edu.kidcheck.colegio.Colegio;
import pe.edu.kidcheck.colegio.ColegioRepository;
import pe.edu.kidcheck.auditoria.AuditoriaService;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionRepository configuraciones;
    private final ColegioRepository colegios;
    private final AuditoriaService auditoria;
    private final CurrentUser actual;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
    public List<ConfiguracionDto> listar() {
        return configuraciones.findByColegioIdOrderByClave(actual.colegioId()).stream()
                .map(c -> new ConfiguracionDto(c.getClave(), c.getValor()))
                .toList();
    }

    @PutMapping("/{clave}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConfiguracionDto guardar(@PathVariable String clave,
                                    @Valid @RequestBody ValorRequest request) {
        Configuracion configuracion = configuraciones
                .findByColegioIdAndClave(actual.colegioId(), clave)
                .orElseGet(() -> {
                    Colegio colegio = colegios.findById(actual.colegioId())
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                    "COLEGIO_NO_ENCONTRADO", "Colegio no encontrado",
                                    "No existe el colegio de la sesión."));
                    Configuracion nueva = new Configuracion();
                    nueva.setColegio(colegio);
                    nueva.setClave(clave);
                    return nueva;
                });
        configuracion.setValor(request.valor());
        configuraciones.save(configuracion);
        auditoria.registrar("configuracion", configuracion.getId(), "actualizar",
                Map.of("clave", configuracion.getClave(), "valor", configuracion.getValor()));
        return new ConfiguracionDto(configuracion.getClave(), configuracion.getValor());
    }

    public record ValorRequest(
            @NotBlank(message = "es obligatorio")
            @Size(max = 255, message = "máximo 255 caracteres")
            String valor) {
    }
}
