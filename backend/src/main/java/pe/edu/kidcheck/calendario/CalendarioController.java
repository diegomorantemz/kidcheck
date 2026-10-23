package pe.edu.kidcheck.calendario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.calendario.dto.CalendarioDiaDto;
import pe.edu.kidcheck.calendario.dto.CalendarioDiaRequest;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendario")
@RequiredArgsConstructor
public class CalendarioController {

    private final CalendarioService calendario;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public List<CalendarioDiaDto> listar(@RequestParam(required = false) Integer anio) {
        return calendario.listar(anio);
    }

    @PutMapping("/{fecha}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CalendarioDiaDto guardar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Valid @RequestBody CalendarioDiaRequest request) {
        return calendario.guardar(fecha, request);
    }
}
