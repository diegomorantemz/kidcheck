package pe.edu.kidcheck.shared.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error de negocio con código estable para el cliente.
 * El código se publica en el campo {@code code} del ProblemDetail.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;

    public ApiException(HttpStatus status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }
}
