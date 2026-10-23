package pe.edu.kidcheck.seccion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.auditoria.AuditoriaService;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/secciones")
@RequiredArgsConstructor
public class SeccionController {

    private final SeccionRepository secciones;
    private final AuditoriaService auditoria;
    private final CurrentUser actual;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    @Transactional(readOnly = true)
    public List<SeccionDto> listar() {
        return secciones.findByPeriodoEstadoOrderByGradoAscLetraAsc("activo")
                .stream()
                .filter(seccion -> seccion.getPeriodo().getColegio().getId()
                        .equals(actual.colegioId()))
                .map(SeccionDto::de)
                .toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public SeccionDto actualizar(@PathVariable Long id,
                                 @Valid @RequestBody SeccionRequest request) {
        Seccion seccion = secciones.findById(id)
                .filter(s -> s.getPeriodo().getColegio().getId().equals(actual.colegioId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SECCION_NO_ENCONTRADA",
                        "Sección no encontrada",
                        "No existe la sección con id " + id + " en este colegio."));

        seccion.setHoraLimiteIngreso(request.horaLimiteIngreso());
        seccion.setToleranciaMinutos((short) request.toleranciaMinutos());
        secciones.save(seccion);

        auditoria.registrar("seccion", seccion.getId(), "actualizar",
                Map.of("horaLimiteIngreso", seccion.getHoraLimiteIngreso().toString(),
                        "toleranciaMinutos", seccion.getToleranciaMinutos()));

        return SeccionDto.de(seccion);
    }

    public record SeccionRequest(
            @NotNull(message = "es obligatorio")
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime horaLimiteIngreso,

            @Min(value = 0, message = "debe ser mayor o igual a 0")
            @Max(value = 120, message = "máximo 120 minutos")
            int toleranciaMinutos
    ) {
    }
}
