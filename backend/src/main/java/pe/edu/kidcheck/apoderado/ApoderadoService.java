package pe.edu.kidcheck.apoderado;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.kidcheck.apoderado.dto.ApoderadoDto;
import pe.edu.kidcheck.apoderado.dto.ApoderadoRequest;
import pe.edu.kidcheck.apoderado.dto.VinculoDto;
import pe.edu.kidcheck.apoderado.dto.VinculoRequest;
import pe.edu.kidcheck.colegio.ColegioRepository;
import pe.edu.kidcheck.estudiante.Estudiante;
import pe.edu.kidcheck.shared.error.ApiException;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.web.PageResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApoderadoService {

    private final ApoderadoRepository apoderados;
    private final EstudianteApoderadoRepository vinculos;
    private final ColegioRepository colegios;
    private final CurrentUser actual;

    @Transactional(readOnly = true)
    public PageResponse<ApoderadoDto> listar(String q, Pageable pageable) {
        String filtro = normalizarFiltro(q);
        return PageResponse.de(apoderados.buscar(actual.colegioId(), filtro, pageable)
                .map(ApoderadoDto::de));
    }

    @Transactional
    public ApoderadoDto crear(ApoderadoRequest request) {
        Apoderado apoderado = new Apoderado();
        apoderado.setColegio(colegios.getReferenceById(actual.colegioId()));
        apoderado.setEstado("activo");
        aplicar(apoderado, request);
        return ApoderadoDto.de(apoderados.save(apoderado));
    }

    @Transactional
    public ApoderadoDto actualizar(Long id, ApoderadoRequest request) {
        Apoderado apoderado = obtener(id);
        aplicar(apoderado, request);
        return ApoderadoDto.de(apoderados.save(apoderado));
    }

    @Transactional(readOnly = true)
    public List<VinculoDto> vinculosDe(Long idEstudiante) {
        return vinculos.listarPorEstudiante(idEstudiante).stream()
                .map(VinculoDto::de)
                .toList();
    }

    @Transactional
    public void reemplazarVinculos(Estudiante estudiante, List<VinculoRequest> lista) {
        vinculos.borrarPorEstudiante(estudiante.getId());
        if (lista == null) {
            return;
        }
        for (VinculoRequest request : lista) {
            Apoderado apoderado = obtener(request.idApoderado());

            EstudianteApoderado vinculo = new EstudianteApoderado();
            vinculo.setId(new EstudianteApoderadoId(estudiante.getId(), apoderado.getId()));
            vinculo.setEstudiante(estudiante);
            vinculo.setApoderado(apoderado);
            vinculo.setParentesco(request.parentesco());
            vinculo.setEsPrincipal((byte) (request.esPrincipal() ? 1 : 0));
            vinculo.setAutorizadoRecoger((byte) (request.autorizadoRecoger() ? 1 : 0));
            vinculos.save(vinculo);
        }
    }

    @Transactional(readOnly = true)
    public Apoderado obtener(Long id) {
        return apoderados.findByIdAndColegioId(id, actual.colegioId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APODERADO_NO_ENCONTRADO",
                        "Apoderado no encontrado",
                        "No existe un apoderado con id " + id + " en este colegio."));
    }

    private void aplicar(Apoderado apoderado, ApoderadoRequest request) {
        apoderado.setNombresApellidos(request.nombresApellidos().trim());
        apoderado.setTelefonoWhatsapp(limpiar(request.telefonoWhatsapp()));
        apoderado.setEmail(limpiar(request.email()));
    }

    private static String normalizarFiltro(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase() + "%";
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
