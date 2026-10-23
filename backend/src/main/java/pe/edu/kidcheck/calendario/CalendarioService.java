package pe.edu.kidcheck.calendario;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.calendario.dto.CalendarioDiaDto;
import pe.edu.kidcheck.calendario.dto.CalendarioDiaRequest;
import pe.edu.kidcheck.auditoria.AuditoriaService;
import pe.edu.kidcheck.colegio.Colegio;
import pe.edu.kidcheck.colegio.ColegioRepository;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalendarioService {

    private final CalendarioEscolarRepository calendario;
    private final ColegioRepository colegios;
    private final AuditoriaService auditoria;
    private final CurrentUser actual;

    public List<CalendarioDiaDto> listar(Integer anio) {
        int valor = anio == null ? LocalDate.now(Tiempo.LIMA).getYear() : anio;
        return calendario.findByColegioIdAndFechaBetweenOrderByFecha(
                        actual.colegioId(), LocalDate.of(valor, 1, 1), LocalDate.of(valor, 12, 31))
                .stream()
                .map(c -> new CalendarioDiaDto(c.getFecha(), c.laborable(), c.getDescripcion()))
                .toList();
    }

    @Transactional
    public CalendarioDiaDto guardar(LocalDate fecha, CalendarioDiaRequest request) {
        Long colegioId = actual.colegioId();
        CalendarioEscolar dia = calendario.findByColegioIdAndFecha(colegioId, fecha)
                .orElseGet(() -> {
                    Colegio colegio = colegios.findById(colegioId)
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                    "COLEGIO_NO_ENCONTRADO", "Colegio no encontrado",
                                    "No existe el colegio de la sesión."));
                    CalendarioEscolar nuevo = new CalendarioEscolar();
                    nuevo.setColegio(colegio);
                    nuevo.setFecha(fecha);
                    return nuevo;
                });

        dia.setEsLaborable((byte) (request.esLaborable() ? 1 : 0));
        dia.setDescripcion(request.descripcion());
        calendario.save(dia);
        auditoria.registrar("calendario", null, "actualizar",
                Map.of("fecha", fecha.toString(),
                        "esLaborable", request.esLaborable()));
        return new CalendarioDiaDto(dia.getFecha(), dia.laborable(), dia.getDescripcion());
    }
}
