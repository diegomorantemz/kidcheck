package pe.edu.kidcheck.ingreso;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.asistencia.AsistenciaDia;
import pe.edu.kidcheck.asistencia.AsistenciaDiaRepository;
import pe.edu.kidcheck.dispositivo.Dispositivo;
import pe.edu.kidcheck.dispositivo.DispositivoRepository;
import pe.edu.kidcheck.estudiante.Estudiante;
import pe.edu.kidcheck.estudiante.EstudianteRepository;
import pe.edu.kidcheck.estudiante.Matricula;
import pe.edu.kidcheck.estudiante.MatriculaRepository;
import pe.edu.kidcheck.ingreso.dto.IngresoResponse;
import pe.edu.kidcheck.ingreso.dto.RegistroIngresoRequest;
import pe.edu.kidcheck.notificacion.NotificacionService;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;
import pe.edu.kidcheck.usuario.Usuario;
import pe.edu.kidcheck.usuario.UsuarioRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IngresoService {

    private final IngresoRepository ingresos;
    private final AsistenciaDiaRepository asistencias;
    private final EstudianteRepository estudiantes;
    private final MatriculaRepository matriculas;
    private final DispositivoRepository dispositivos;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final CurrentUser actual;

    /**
     * Registra un ingreso de forma idempotente.
     *
     * - La misma Idempotency-Key devuelve el resultado original (reintento de red).
     * - Otro intento para el mismo alumno y día devuelve 409 con la hora original.
     * - La notificación se crea en esta misma transacción (outbox).
     */
    @Transactional
    public IngresoResponse registrar(RegistroIngresoRequest request, String idempotencyKey) {
        String clave = normalizarClave(idempotencyKey);

        Optional<Ingreso> replay = ingresos.findByIdempotencyKey(clave);
        if (replay.isPresent()) {
            return respuestaExistente(replay.get());
        }

        Long colegioId = actual.colegioId();
        Estudiante estudiante = estudiantes.findByIdAndColegioIdForUpdate(request.idEstudiante(), colegioId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CREDENCIAL_INVALIDA",
                        "Credencial inválida",
                        "El estudiante no existe en este colegio."));

        if (!"activo".equals(estudiante.getEstado())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ALUMNO_INACTIVO",
                    "Alumno inactivo", "El estudiante no está habilitado para registrar ingresos.");
        }

        Matricula matricula = matriculas
                .findFirstByEstudianteIdAndEstadoOrderByFechaMatriculaDesc(estudiante.getId(), "activa")
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SIN_MATRICULA_ACTIVA",
                        "Sin matrícula activa", "El estudiante no tiene una matrícula activa."));

        LocalDateTime utc = Tiempo.aUtc(request.fechaHoraUtc());
        LocalDate fechaLocal = Tiempo.fechaLocal(utc);
        LocalTime horaLocal = Tiempo.horaLocal(utc);

        Optional<Ingreso> duplicado = ingresos
                .buscarDelDiaForUpdate(estudiante.getId(), fechaLocal).stream().findFirst();
        if (duplicado.isPresent()) {
            Ingreso original = duplicado.get();
            throw new ApiException(HttpStatus.CONFLICT, "INGRESO_YA_REGISTRADO",
                    "Ingreso ya registrado",
                    estudiante.getNombres() + " ya registró su ingreso a las "
                            + Tiempo.hhmm(original.getHoraLocal()) + ".",
                    Map.of("idIngreso", original.getId(),
                            "horaLocal", original.getHoraLocal().toString(),
                            "fecha", fechaLocal.toString()));
        }

        LocalTime limite = matricula.getSeccion().getHoraLimiteIngreso();
        boolean tarde = horaLocal.isAfter(limite);
        short minutosTarde = 0;
        if (tarde) {
            minutosTarde = (short) Math.max(0, Duration.between(limite, horaLocal).toMinutes());
        }

        Ingreso ingreso = new Ingreso();
        ingreso.setEstudiante(estudiante);
        ingreso.setMatricula(matricula);
        ingreso.setUsuario(usuarios.findById(actual.usuarioId()).orElse(null));
        ingreso.setDispositivo(dispositivo(request.deviceUuid(), colegioId));
        ingreso.setFechaHoraUtc(utc);
        ingreso.setFechaLocal(fechaLocal);
        ingreso.setHoraLocal(horaLocal);
        ingreso.setCanal(request.canal() == null ? "qr" : request.canal());
        ingreso.setObservacion(request.observacion() == null || request.observacion().isBlank()
                ? null : request.observacion().trim());
        ingreso.setIdempotencyKey(clave);
        ingresos.save(ingreso);

        AsistenciaDia asistencia = new AsistenciaDia();
        asistencia.setEstudiante(estudiante);
        asistencia.setSeccion(matricula.getSeccion());
        asistencia.setFecha(fechaLocal);
        asistencia.setEstado(tarde ? AsistenciaDia.TARDE : AsistenciaDia.PRESENTE);
        asistencia.setMinutosTarde(minutosTarde);
        asistencia.setIngreso(ingreso);
        asistencia.setOrigen("qr".equals(ingreso.getCanal()) ? "escaner" : "manual");
        asistencias.save(asistencia);

        boolean encolada = notificaciones.encolarIngreso(ingreso);

        return new IngresoResponse(
                ingreso.getId(),
                asistencia.getEstado().toUpperCase(),
                horaLocal.toString(),
                "Ingreso registrado",
                encolada);
    }

    private IngresoResponse respuestaExistente(Ingreso ingreso) {
        String estado = asistencias
                .findByEstudianteIdAndFecha(ingreso.getEstudiante().getId(), ingreso.getFechaLocal())
                .map(AsistenciaDia::getEstado)
                .orElse(AsistenciaDia.PRESENTE)
                .toUpperCase();
        return new IngresoResponse(
                ingreso.getId(),
                estado,
                ingreso.getHoraLocal().toString(),
                "Ingreso ya registrado",
                false);
    }

    private Dispositivo dispositivo(String deviceUuid, Long colegioId) {
        if (deviceUuid == null || deviceUuid.isBlank()) {
            return null;
        }
        return dispositivos.findByDeviceUuid(deviceUuid)
                .filter(d -> d.getColegio().getId().equals(colegioId))
                .orElse(null);
    }

    private static String normalizarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FALTA_IDEMPOTENCY_KEY",
                    "Falta Idempotency-Key",
                    "Envía el encabezado Idempotency-Key para evitar ingresos duplicados.");
        }
        String limpia = clave.trim();
        if (limpia.length() > 36) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_INVALIDA",
                    "Idempotency-Key inválida",
                    "La clave debe tener como máximo 36 caracteres.");
        }
        return limpia;
    }
}
