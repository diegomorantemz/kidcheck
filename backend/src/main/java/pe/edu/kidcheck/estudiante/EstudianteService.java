package pe.edu.kidcheck.estudiante;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.apoderado.ApoderadoService;
import pe.edu.kidcheck.apoderado.dto.VinculoDto;
import pe.edu.kidcheck.auditoria.AuditoriaService;
import pe.edu.kidcheck.colegio.ColegioRepository;
import pe.edu.kidcheck.credencial.CredencialQrRepository;
import pe.edu.kidcheck.estudiante.dto.EstudianteDto;
import pe.edu.kidcheck.estudiante.dto.EstudianteRequest;
import pe.edu.kidcheck.seccion.Seccion;
import pe.edu.kidcheck.seccion.SeccionRepository;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.web.PageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudiantes;
    private final MatriculaRepository matriculas;
    private final SeccionRepository secciones;
    private final ColegioRepository colegios;
    private final ApoderadoService apoderados;
    private final CredencialQrRepository credenciales;
    private final AuditoriaService auditoria;
    private final CurrentUser actual;

    @Transactional(readOnly = true)
    public PageResponse<EstudianteDto> listar(String q, Long seccionId, String estado, Pageable pageable) {
        String filtro = normalizarFiltro(q);
        String estadoFiltro = (estado == null || estado.isBlank()) ? null : estado.trim();

        Page<Estudiante> pagina = estudiantes.buscar(
                actual.colegioId(), filtro, seccionId, estadoFiltro, pageable);

        return PageResponse.de(pagina.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public EstudianteDto obtener(Long id) {
        return toDto(obtenerEntidad(id));
    }

    @Transactional
    public EstudianteDto crear(EstudianteRequest request) {
        Long colegioId = actual.colegioId();
        String codigo = request.codigoMatricula().trim();

        if (estudiantes.existsByColegioIdAndCodigoMatricula(colegioId, codigo)) {
            throw codigoDuplicado(codigo);
        }
        if (request.seccionId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SECCION_REQUERIDA", "Sección requerida",
                    "Debes indicar la sección para matricular al estudiante.");
        }

        Estudiante estudiante = new Estudiante();
        estudiante.setColegio(colegios.getReferenceById(colegioId));
        estudiante.setEstado(request.estado() == null ? "activo" : request.estado());
        aplicar(estudiante, request);
        estudiantes.save(estudiante);

        matricular(estudiante, request.seccionId());
        apoderados.reemplazarVinculos(estudiante, request.apoderados());

        auditoria.registrar("estudiante", estudiante.getId(), "crear",
                Map.of("codigoMatricula", estudiante.getCodigoMatricula(),
                        "seccionId", request.seccionId()));

        return toDto(estudiante);
    }

    @Transactional
    public EstudianteDto actualizar(Long id, EstudianteRequest request) {
        Estudiante estudiante = obtenerEntidad(id);
        Long colegioId = actual.colegioId();
        String codigo = request.codigoMatricula().trim();

        if (!codigo.equals(estudiante.getCodigoMatricula())
                && estudiantes.existsByColegioIdAndCodigoMatriculaAndIdNot(colegioId, codigo, id)) {
            throw codigoDuplicado(codigo);
        }

        aplicar(estudiante, request);
        estudiantes.save(estudiante);

        if (request.seccionId() != null) {
            matricular(estudiante, request.seccionId());
        }
        if (request.apoderados() != null) {
            apoderados.reemplazarVinculos(estudiante, request.apoderados());
        }

        auditoria.registrar("estudiante", estudiante.getId(), "actualizar",
                Map.of("codigoMatricula", estudiante.getCodigoMatricula(),
                        "estado", estudiante.getEstado()));

        return toDto(estudiante);
    }

    private void matricular(Estudiante estudiante, Long idSeccion) {
        Seccion seccion = secciones.findById(idSeccion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SECCION_NO_ENCONTRADA",
                        "Sección no encontrada",
                        "No existe una sección con id " + idSeccion + "."));

        Matricula matricula = matriculas
                .findFirstByEstudianteIdAndEstadoOrderByFechaMatriculaDesc(estudiante.getId(), "activa")
                .orElseGet(() -> {
                    Matricula nueva = new Matricula();
                    nueva.setEstudiante(estudiante);
                    nueva.setEstado("activa");
                    nueva.setFechaMatricula(LocalDate.now());
                    return nueva;
                });

        matricula.setSeccion(seccion);
        matriculas.save(matricula);
    }

    private EstudianteDto toDto(Estudiante estudiante) {
        Matricula matricula = matriculas
                .findFirstByEstudianteIdAndEstadoOrderByFechaMatriculaDesc(estudiante.getId(), "activa")
                .orElse(null);

        List<VinculoDto> vinculos = apoderados.vinculosDe(estudiante.getId());
        var credencial = credenciales
                .findFirstByEstudianteIdAndEstadoOrderByEmitidaEnDesc(estudiante.getId(), "activa")
                .orElse(null);

        return new EstudianteDto(
                estudiante.getId(),
                estudiante.getCodigoMatricula(),
                estudiante.getNombres(),
                estudiante.getApellidos(),
                estudiante.nombreCompleto(),
                estudiante.getFechaNacimiento(),
                estudiante.getEstado(),
                matricula == null ? null : matricula.getSeccion().getId(),
                matricula == null ? null : matricula.getSeccion().nombre(),
                credencial == null ? null : credencial.getId(),
                credencial != null,
                vinculos);
    }

    private Estudiante obtenerEntidad(Long id) {
        return estudiantes.findByIdAndColegioId(id, actual.colegioId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO",
                        "Estudiante no encontrado",
                        "No existe un estudiante con id " + id + " en este colegio."));
    }

    private void aplicar(Estudiante estudiante, EstudianteRequest request) {
        estudiante.setCodigoMatricula(request.codigoMatricula().trim());
        estudiante.setNombres(request.nombres().trim());
        estudiante.setApellidos(request.apellidos().trim());
        estudiante.setFechaNacimiento(request.fechaNacimiento());
        if (request.estado() != null) {
            estudiante.setEstado(request.estado());
        }
    }

    private static ApiException codigoDuplicado(String codigo) {
        return new ApiException(HttpStatus.CONFLICT, "CODIGO_MATRICULA_DUPLICADO",
                "Código de matrícula duplicado",
                "Ya existe un estudiante con el código " + codigo + " en este colegio.");
    }

    private static String normalizarFiltro(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase() + "%";
    }
}
