package pe.edu.kidcheck.apoderado;

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
import pe.edu.kidcheck.apoderado.dto.ApoderadoDto;
import pe.edu.kidcheck.apoderado.dto.ApoderadoRequest;
import pe.edu.kidcheck.shared.web.PageResponse;

@RestController
@RequestMapping("/api/v1/apoderados")
@RequiredArgsConstructor
public class ApoderadoController {

    private final ApoderadoService apoderadoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
    public PageResponse<ApoderadoDto> listar(@RequestParam(required = false) String q,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return apoderadoService.listar(q, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApoderadoDto crear(@Valid @RequestBody ApoderadoRequest request) {
        return apoderadoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApoderadoDto actualizar(@PathVariable Long id, @Valid @RequestBody ApoderadoRequest request) {
        return apoderadoService.actualizar(id, request);
    }
}
