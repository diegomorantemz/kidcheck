package pe.edu.kidcheck.ingreso;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.ingreso.dto.IngresoResponse;
import pe.edu.kidcheck.ingreso.dto.RegistroIngresoRequest;

@RestController
@RequestMapping("/api/v1/ingresos")
@RequiredArgsConstructor
public class IngresoController {

    private final IngresoService ingresos;

    @PostMapping
    @PreAuthorize("hasAnyRole('VIGILANTE','ADMINISTRADOR','AUXILIAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public IngresoResponse registrar(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RegistroIngresoRequest request) {
        return ingresos.registrar(request, idempotencyKey);
    }
}
