package pe.edu.kidcheck.seccion;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/secciones")
@RequiredArgsConstructor
public class SeccionController {

    private final SeccionRepository secciones;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public List<SeccionDto> listar() {
        return secciones.findByPeriodoEstadoOrderByGradoAscLetraAsc("activo")
                .stream()
                .map(SeccionDto::de)
                .toList();
    }
}
