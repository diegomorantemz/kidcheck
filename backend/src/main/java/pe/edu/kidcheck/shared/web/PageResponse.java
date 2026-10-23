package pe.edu.kidcheck.shared.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltura estable para listados paginados. Evita exponer el formato
 * interno de Spring Data al frontend y a la app Android.
 */
public record PageResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long total,
        int totalPaginas
) {
    public static <T> PageResponse<T> de(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
