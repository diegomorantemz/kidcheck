package pe.edu.kidcheck.sync;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.shared.time.Tiempo;
import pe.edu.kidcheck.sync.dto.RosterResponse;
import pe.edu.kidcheck.sync.dto.SyncIngresosRequest;
import pe.edu.kidcheck.sync.dto.SyncIngresosResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService sync;

    @GetMapping("/roster")
    @PreAuthorize("hasAnyRole('VIGILANTE','ADMINISTRADOR','AUXILIAR')")
    public RosterResponse roster(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return sync.roster(fecha == null ? LocalDate.now(Tiempo.LIMA) : fecha);
    }

    @PostMapping("/ingresos")
    @PreAuthorize("hasAnyRole('VIGILANTE','ADMINISTRADOR','AUXILIAR')")
    public SyncIngresosResponse sincronizar(@Valid @RequestBody SyncIngresosRequest request) {
        return sync.sincronizar(request);
    }
}
