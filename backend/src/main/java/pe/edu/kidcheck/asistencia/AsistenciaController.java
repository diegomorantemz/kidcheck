package pe.edu.kidcheck.asistencia;

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
import pe.edu.kidcheck.asistencia.dto.AsistenciaDto;
import pe.edu.kidcheck.asistencia.dto.JustificarRequest;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistencias;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public List<AsistenciaDto> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) String estado) {
        return asistencias.listar(fecha == null ? LocalDate.now(Tiempo.LIMA) : fecha, seccionId, estado);
    }

    @PostMapping("/{id}/justificar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
    public AsistenciaDto justificar(@PathVariable Long id,
                                    @Valid @RequestBody JustificarRequest request) {
        return asistencias.justificar(id, request);
    }
}
