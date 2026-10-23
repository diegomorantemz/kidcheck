package pe.edu.kidcheck.auditoria;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.kidcheck.auditoria.dto.AuditoriaDto;
import pe.edu.kidcheck.shared.security.CurrentUser;
import pe.edu.kidcheck.shared.time.Tiempo;
import pe.edu.kidcheck.shared.web.PageResponse;
import pe.edu.kidcheck.usuario.UsuarioRepository;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditorias;
    private final UsuarioRepository usuarios;
    private final CurrentUser actual;
    private final ObjectMapper objectMapper;

    /**
     * Registra una acción sin afectar la transacción de negocio: si la
     * auditoría falla, la operación principal no se revierte, pero queda
     * evidencia en el log del servidor.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String entidad, Long idEntidad, String accion, Map<String, Object> datos) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setUsuario(usuarios.getReferenceById(actual.usuarioId()));
            auditoria.setEntidad(entidad);
            auditoria.setIdEntidad(idEntidad);
            auditoria.setAccion(accion);
            auditoria.setDatos(datos == null || datos.isEmpty()
                    ? null : objectMapper.writeValueAsString(datos));
            auditoria.setIp(ipActual());
            auditorias.save(auditoria);
        } catch (Exception ex) {
            log.warn("No se pudo registrar la auditoría {}:{}: {}", entidad, accion, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditoriaDto> listar(String entidad, LocalDate desde, LocalDate hasta,
                                             Pageable pageable) {
        String filtro = entidad == null || entidad.isBlank() ? null : entidad.trim().toLowerCase();
        Page<AuditoriaDto> pagina = auditorias.buscar(
                        actual.colegioId(),
                        filtro,
                        desde == null ? null : Tiempo.inicioDiaUtc(desde),
                        hasta == null ? null : Tiempo.finDiaUtc(hasta),
                        pageable)
                .map(this::aDto);
        return PageResponse.de(pagina);
    }

    private AuditoriaDto aDto(Auditoria auditoria) {
        return new AuditoriaDto(
                auditoria.getId(),
                Tiempo.aInstante(auditoria.getCreadoEn()),
                auditoria.getUsuario() == null ? null : auditoria.getUsuario().getId(),
                auditoria.getUsuario() == null ? null : auditoria.getUsuario().getNombreCompleto(),
                auditoria.getEntidad(),
                auditoria.getIdEntidad(),
                auditoria.getAccion(),
                convertir(auditoria.getDatos()),
                auditoria.getIp());
    }

    private Map<String, Object> convertir(String datos) {
        if (datos == null || datos.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(datos, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of("valor", datos);
        }
    }

    private static String ipActual() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        String reenviada = attributes.getRequest().getHeader("X-Forwarded-For");
        String ip = reenviada == null || reenviada.isBlank()
                ? attributes.getRequest().getRemoteAddr()
                : reenviada.split(",")[0].trim();
        return ip == null || ip.isBlank() ? null : ip.substring(0, Math.min(ip.length(), 45));
    }
}
