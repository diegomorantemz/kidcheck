package pe.edu.kidcheck.panel;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.panel.dto.PanelDiaResponse;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/panel")
@RequiredArgsConstructor
public class PanelController {

    private final PanelService panel;

    @GetMapping("/dia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','PSICOLOGO')")
    public PanelDiaResponse dia(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long seccionId) {
        return panel.panel(fecha == null ? LocalDate.now(Tiempo.LIMA) : fecha, seccionId);
    }
}
