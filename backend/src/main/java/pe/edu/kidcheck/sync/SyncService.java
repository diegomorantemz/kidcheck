package pe.edu.kidcheck.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pe.edu.kidcheck.colegio.Colegio;
import pe.edu.kidcheck.colegio.ColegioRepository;
import pe.edu.kidcheck.dispositivo.DispositivoRepository;
import pe.edu.kidcheck.estudiante.EstudianteRepository;
import pe.edu.kidcheck.ingreso.IngresoRepository;
import pe.edu.kidcheck.ingreso.IngresoService;
import pe.edu.kidcheck.ingreso.dto.RegistroIngresoRequest;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;
import pe.edu.kidcheck.sync.dto.RosterResponse;
import pe.edu.kidcheck.sync.dto.SyncIngresosRequest;
import pe.edu.kidcheck.sync.dto.SyncIngresosResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SyncService {

    private final EstudianteRepository estudiantes;
    private final ColegioRepository colegios;
    private final DispositivoRepository dispositivos;
    private final IngresoRepository ingresos;
    private final IngresoService ingresoService;
    private final CurrentUser actual;

    public RosterResponse roster(LocalDate fecha) {
        Long colegioId = actual.colegioId();
        Colegio colegio = colegios.findById(colegioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COLEGIO_NO_ENCONTRADO",
                        "Colegio no encontrado", "No existe el colegio de la sesión."));

        List<RosterResponse.EstudianteRoster> lista = estudiantes.roster(colegioId).stream()
                .map(fila -> {
                    String grado = (String) fila[5];
                    String letra = (String) fila[6];
                    LocalTime horaLimite = (LocalTime) fila[7];
                    return new RosterResponse.EstudianteRoster(
                            ((Number) fila[0]).longValue(),
                            (String) fila[1],
                            (String) fila[2],
                            (String) fila[3],
                            (String) fila[4],
                            grado + "°" + letra,
                            Tiempo.hhmm(horaLimite),
                            null);
                })
                .toList();

        return new RosterResponse(Instant.now().toString(),
                new RosterResponse.ColegioInfo(colegio.getId(), Tiempo.LIMA.getId()),
                lista);
    }

    public SyncIngresosResponse sincronizar(SyncIngresosRequest request) {
        dispositivos.findByDeviceUuid(request.deviceUuid()).ifPresent(dispositivo -> {
            dispositivo.setUltimaSincronizacion(Tiempo.ahoraUtc());
            dispositivos.save(dispositivo);
        });

        List<SyncIngresosResponse.Resultado> resultados = new ArrayList<>();

        for (SyncIngresosRequest.Item item : request.ingresos()) {
            try {
                boolean repetido = ingresos.findByIdempotencyKey(item.clientId()).isPresent();
                var respuesta = ingresoService.registrar(
                        new RegistroIngresoRequest(item.idEstudiante(), request.deviceUuid(),
                                item.fechaHoraUtc(), item.canal()),
                        item.clientId());

                resultados.add(repetido
                        ? SyncIngresosResponse.Resultado.duplicado(item.clientId(),
                        respuesta.idIngreso(), respuesta.horaLocal())
                        : SyncIngresosResponse.Resultado.creado(item.clientId(),
                        respuesta.idIngreso(), respuesta.horaLocal()));
            } catch (ApiException ex) {
                if ("INGRESO_YA_REGISTRADO".equals(ex.getCode())) {
                    resultados.add(SyncIngresosResponse.Resultado.duplicado(item.clientId(),
                            numero(ex.getPropiedades().get("idIngreso")),
                            texto(ex.getPropiedades().get("horaLocal"))));
                } else {
                    resultados.add(SyncIngresosResponse.Resultado.error(item.clientId(),
                            ex.getCode(), ex.getMessage()));
                }
            } catch (Exception ex) {
                resultados.add(SyncIngresosResponse.Resultado.error(item.clientId(),
                        "ERROR_INTERNO", "No se pudo registrar el ingreso."));
            }
        }

        return new SyncIngresosResponse(resultados);
    }

    private static Long numero(Object valor) {
        return valor instanceof Number n ? n.longValue() : null;
    }

    private static String texto(Object valor) {
        return valor instanceof String s ? s : null;
    }
}
