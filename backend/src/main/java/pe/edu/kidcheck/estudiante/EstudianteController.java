package pe.edu.kidcheck.estudiante;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.estudiante.dto.EstudianteDto;
import pe.edu.kidcheck.estudiante.dto.EstudianteRequest;
import pe.edu.kidcheck.shared.web.PageResponse;

@RestController
@RequestMapping("/api/v1/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public PageResponse<EstudianteDto> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20, sort = {"apellidos", "nombres"}) Pageable pageable) {
        return estudianteService.listar(q, seccionId, estado, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public EstudianteDto obtener(@PathVariable Long id) {
        return estudianteService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public EstudianteDto crear(@Valid @RequestBody EstudianteRequest request) {
        return estudianteService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EstudianteDto actualizar(@PathVariable Long id, @Valid @RequestBody EstudianteRequest request) {
        return estudianteService.actualizar(id, request);
    }
}
