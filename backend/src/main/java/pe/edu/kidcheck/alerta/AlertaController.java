package pe.edu.kidcheck.alerta;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.alerta.dto.AlertaDto;
import pe.edu.kidcheck.alerta.dto.DerivarRequest;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertas;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public List<AlertaDto> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return alertas.listar(estado, fecha);
    }

    @PostMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public AlertaDto atender(@PathVariable Long id) {
        return alertas.atender(id);
    }

    @PostMapping("/{id}/derivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public AlertaDto derivar(@PathVariable Long id,
                             @Valid @RequestBody(required = false) DerivarRequest request) {
        return alertas.derivar(id, request);
    }
}
