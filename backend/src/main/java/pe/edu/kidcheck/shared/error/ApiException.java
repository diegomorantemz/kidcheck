package pe.edu.kidcheck.shared.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Error de negocio con código estable para el cliente.
 * El código se publica en el campo {@code code} del ProblemDetail.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final Map<String, Object> propiedades;

    public ApiException(HttpStatus status, String code, String title, String detail) {
        this(status, code, title, detail, Map.of());
    }

    public ApiException(HttpStatus status, String code, String title, String detail,
                        Map<String, Object> propiedades) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
        this.propiedades = propiedades;
    }
}
