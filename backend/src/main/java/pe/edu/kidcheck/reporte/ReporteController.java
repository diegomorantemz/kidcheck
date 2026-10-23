package pe.edu.kidcheck.reporte;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.kidcheck.reporte.dto.CsvExport;
import pe.edu.kidcheck.reporte.dto.ReporteDiarioResponse;
import pe.edu.kidcheck.reporte.dto.ReporteMensualResponse;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reportes;

    @GetMapping("/diario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
    public ReporteDiarioResponse diario(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long seccionId) {
        return reportes.diario(fecha == null ? LocalDate.now(Tiempo.LIMA) : fecha, seccionId);
    }

    @GetMapping("/mensual")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
    public ReporteMensualResponse mensual(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Long seccionId) {
        return reportes.mensual(periodo, seccionId);
    }

    @GetMapping("/asistencia.csv")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> csv(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Long seccionId) {
        CsvExport exportacion = reportes.csv(periodo, seccionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + exportacion.nombreArchivo() + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(exportacion.contenido());
    }
}
