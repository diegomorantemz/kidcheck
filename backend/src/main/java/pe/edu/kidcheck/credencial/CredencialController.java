package pe.edu.kidcheck.credencial;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.credencial.dto.CredencialEmitidaResponse;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CredencialController {

    private final CredencialService credencialService;

    @PostMapping("/estudiantes/{id}/credencial")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CredencialEmitidaResponse emitir(@PathVariable Long id) {
        return credencialService.emitir(id);
    }

    @DeleteMapping("/credenciales/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revocar(@PathVariable Long id) {
        credencialService.revocar(id);
    }
}
